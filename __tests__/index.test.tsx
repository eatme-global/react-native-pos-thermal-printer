import { NativeEventEmitter, NativeModules } from "react-native";
import {
  addPrinterToPool,
  getPendingJobs,
  getPrinterPoolStatus,
  initializePrinterPool,
  openCashBox,
  printText,
  reconnectPrinter,
  removePrinterFromPool,
  printPendingJobsWithNewPrinter,
  deletePendingJob,
  retryPendingJobFromNewPrinter,
  getPendingPrinterJobs,
  deletePrinterPendingJobs,
  retryPendingJobsFromPrinter,
  getPrinterStatus,
} from "../src/printerModule";
import {
  PrintAlignment,
  PrintFontSize,
  PrintJobRowType,
  type IPPrinter,
  type PrintJobMetadata,
  type PrintJobRow,
} from "../src/types";
import { LINKING_ERROR } from "../src/constants";

const testIp = "192.168.1.100";

// Single combined mock for react-native
jest.mock("react-native", () => {
  const mockSelect = jest.fn((obj) => obj.ios || obj.default || "");

  return {
    NativeModules: {
      PosThermalPrinter: {
        setPrintJobs: jest.fn(),
        retryPrinterConnection: jest.fn(),
        getPrinterPoolStatus: jest.fn(),
        getPendingJobDetails: jest.fn(),
        initializePrinterPool: jest.fn(),
        addPrinterToPool: jest.fn(),
        removePrinterFromPool: jest.fn(),
        printImage: jest.fn(),
        printFromNewPrinter: jest.fn(),
        deletePendingJobs: jest.fn(),
        retryPendingJobFromNewPrinter: jest.fn(),
        getPrinterPendingJobDetails: jest.fn(),
        dismissPendingJobs: jest.fn(),
        retryPendingJobsFromPrinter: jest.fn(),
        checkPrinterStatus: jest.fn(),
      },
      PrinterReachability: {},
    },
    Platform: {
      OS: "ios",
      select: mockSelect,
    },
    NativeEventEmitter: jest.fn(() => ({
      addListener: jest.fn(() => ({
        remove: jest.fn(),
      })),
      emit: jest.fn(),
    })),
  };
});

const mockEventEmitter = {
  addListener: jest.fn(),
  remove: jest.fn(),
};

(NativeEventEmitter as jest.Mock).mockImplementation(() => mockEventEmitter);

describe("printText", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should call native module with correct parameters", async () => {
    const testPayload: PrintJobRow[] = [
      {
        type: PrintJobRowType.TEXT,
        text: "Test Receipt",
        bold: true,
        fontSize: PrintFontSize.NORMAL,
        alignment: PrintAlignment.CENTER,
        wrapWords: true,
      },
    ];
    const testMetadata: PrintJobMetadata = {
      type: "Receipt",
      orderId: "12345",
    };

    await printText(testIp, testPayload, testMetadata);

    expect(NativeModules.PosThermalPrinter.setPrintJobs).toHaveBeenCalledWith(
      testIp,
      testPayload,
      JSON.stringify(testMetadata),
    );
  });

  it("should handle errors when adding print jobs", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();

    NativeModules.PosThermalPrinter.setPrintJobs.mockRejectedValue(
      new Error("Remove error"),
    );

    const testPayload: PrintJobRow[] = [
      {
        type: PrintJobRowType.TEXT,
        text: "Test Receipt",
        bold: true,
        fontSize: PrintFontSize.NORMAL,
        alignment: PrintAlignment.CENTER,
        wrapWords: true,
      },
    ];
    const testMetadata: PrintJobMetadata = {
      type: "Receipt",
      orderId: "12345",
    };

    await printText(testIp, testPayload, testMetadata);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("openCashBox", () => {
  it("should send correct cash drawer open command", async () => {
    await openCashBox(testIp);

    expect(NativeModules.PosThermalPrinter.setPrintJobs).toHaveBeenCalledWith(
      testIp,
      [{ type: PrintJobRowType.CASHBOX }],
      JSON.stringify({ type: "Open Cashbox" }),
    );
  });

  it("should handle errors when opening cash drawer", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.setPrintJobs.mockRejectedValue(
      new Error("Cash drawer error"),
    );

    await openCashBox(testIp);

    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("reconnectPrinter", () => {
  it("should attempt to reconnect and return success status", async () => {
    NativeModules.PosThermalPrinter.retryPrinterConnection.mockResolvedValue(
      true,
    );
    const result = await reconnectPrinter(testIp);
    expect(result).toBe(true);
  });

  it("should handle errors when reconnecting printers", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();

    NativeModules.PosThermalPrinter.retryPrinterConnection.mockRejectedValue(
      false,
    );
    const result = await reconnectPrinter(testIp);
    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("getPrinterPoolStatus", () => {
  it("should return printer status array", async () => {
    const mockStatus = [
      {
        printerIp: testIp,
        isReachable: true,
        printerName: "Test Printer",
      },
    ];
    NativeModules.PosThermalPrinter.getPrinterPoolStatus.mockResolvedValue(
      mockStatus,
    );

    const status = await getPrinterPoolStatus();
    expect(status).toEqual(mockStatus);
  });

  it("should handle errors and return empty array", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.getPrinterPoolStatus.mockRejectedValue(
      new Error("Status error"),
    );

    const status = await getPrinterPoolStatus();

    expect(status).toEqual([]);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("getPendingJobs", () => {
  it("should parse and return pending jobs", async () => {
    const mockRawJobs = [
      {
        jobId: "1",
        printerIp: testIp,
        printerName: "Test Printer",
        metadata: JSON.stringify({ type: "Receipt", orderId: "12345" }),
      },
    ];
    NativeModules.PosThermalPrinter.getPendingJobDetails.mockResolvedValue(
      mockRawJobs,
    );

    const jobs = await getPendingJobs();
    expect(jobs[0]?.metadata).toEqual({ type: "Receipt", orderId: "12345" });
  });

  it("should handle parsing errors and return empty array", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    const mockRawJobs = [
      {
        jobId: "1",
        printerIp: testIp,
        printerName: "Test Printer",
        metadata: "invalid-json", // Invalid JSON to trigger parse error
      },
    ];
    NativeModules.PosThermalPrinter.getPendingJobDetails.mockResolvedValue(
      mockRawJobs,
    );

    const jobs = await getPendingJobs();

    expect(jobs).toEqual([]);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("should return empty array when no jobs exist", async () => {
    NativeModules.PosThermalPrinter.getPendingJobDetails.mockResolvedValue([]);

    const jobs = await getPendingJobs();

    expect(jobs).toEqual([]);
  });

  it("should handle null response", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.getPendingJobDetails.mockResolvedValue(
      null,
    );

    const jobs = await getPendingJobs();

    expect(jobs).toEqual([]);
    consoleSpy.mockRestore();
  });
});

describe("addPrinterToPool", () => {
  it("should add printer to pool successfully", async () => {
    const printer: IPPrinter = { ip: testIp };
    NativeModules.PosThermalPrinter.addPrinterToPool.mockResolvedValue(true);

    const result = await addPrinterToPool(printer);
    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.addPrinterToPool,
    ).toHaveBeenCalledWith(printer);
  });

  it("should handle errors when adding printer", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    const printer: IPPrinter = { ip: testIp };
    NativeModules.PosThermalPrinter.addPrinterToPool.mockRejectedValue(
      new Error("Add printer error"),
    );

    const result = await addPrinterToPool(printer);

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("initializePrinterPool", () => {
  it("should initialize printer pool successfully", async () => {
    NativeModules.PosThermalPrinter.initializePrinterPool.mockResolvedValue(
      true,
    );

    const result = await initializePrinterPool();
    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.initializePrinterPool,
    ).toHaveBeenCalled();
  });

  it("should handle initialization errors", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.initializePrinterPool.mockRejectedValue(
      new Error("Init error"),
    );

    const result = await initializePrinterPool();

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("removePrinterFromPool", () => {
  it("should remove printer from pool successfully", async () => {
    NativeModules.PosThermalPrinter.removePrinterFromPool.mockResolvedValue(
      true,
    );

    const result = await removePrinterFromPool(testIp);

    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.removePrinterFromPool,
    ).toHaveBeenCalledWith(testIp);
  });

  it("should handle errors when removing printer", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();

    NativeModules.PosThermalPrinter.removePrinterFromPool.mockRejectedValue(
      new Error("Remove error"),
    );

    const result = await removePrinterFromPool(testIp);

    expect(consoleSpy).toHaveBeenCalled();
    expect(result).toBeFalsy();
    consoleSpy.mockRestore();
  });
});

describe("printPendingJobsWithNewPrinter", () => {
  it("should print pending jobs with new printer successfully", async () => {
    NativeModules.PosThermalPrinter.printFromNewPrinter.mockResolvedValue(true);

    const result = await printPendingJobsWithNewPrinter(testIp, testIp);

    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.printFromNewPrinter,
    ).toHaveBeenCalledWith(testIp, testIp);
  });

  it("should handle errors when printing with new printer", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.printFromNewPrinter.mockRejectedValue(
      new Error("Print error"),
    );

    const result = await printPendingJobsWithNewPrinter(testIp, testIp);

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("should handle parsing errors in jobs metadata", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    const mockRawJobs = [
      {
        jobId: "1",
        printerIp: testIp,
        printerName: "Test Printer",
        metadata: "invalid-json",
      },
    ];
    NativeModules.PosThermalPrinter.getPrinterPendingJobDetails.mockResolvedValue(
      mockRawJobs,
    );

    const jobs = await getPendingPrinterJobs(testIp);

    expect(jobs).toEqual([]);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it("should return empty array for null response", async () => {
    NativeModules.PosThermalPrinter.getPrinterPendingJobDetails.mockResolvedValue(
      null,
    );

    const jobs = await getPendingPrinterJobs(testIp);

    expect(jobs).toEqual([]);
  });
});

describe("deletePendingJob", () => {
  const testJobId = "job123";

  it("should delete pending job successfully", async () => {
    NativeModules.PosThermalPrinter.deletePendingJobs.mockResolvedValue(true);

    const result = await deletePendingJob(testJobId);

    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.deletePendingJobs,
    ).toHaveBeenCalledWith(testJobId);
  });

  it("should handle errors when deleting pending job", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.deletePendingJobs.mockRejectedValue(
      new Error("Delete error"),
    );

    const result = await deletePendingJob(testJobId);

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("retryPendingJobFromNewPrinter", () => {
  const testJobId = "job123";

  it("should retry pending job from new printer successfully", async () => {
    NativeModules.PosThermalPrinter.retryPendingJobFromNewPrinter.mockResolvedValue(
      true,
    );

    const result = await retryPendingJobFromNewPrinter(testJobId, testIp);

    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.retryPendingJobFromNewPrinter,
    ).toHaveBeenCalledWith(testJobId, testIp);
  });

  it("should handle errors when retrying from new printer", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.retryPendingJobFromNewPrinter.mockRejectedValue(
      new Error("Retry error"),
    );

    const result = await retryPendingJobFromNewPrinter(testJobId, testIp);

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("getPendingPrinterJobs", () => {
  it("should get and parse pending printer jobs successfully", async () => {
    const mockRawJobs = [
      {
        jobId: "1",
        printerIp: testIp,
        printerName: "Test Printer",
        metadata: JSON.stringify({ type: "Receipt", orderId: "12345" }),
      },
    ];
    NativeModules.PosThermalPrinter.getPrinterPendingJobDetails.mockResolvedValue(
      mockRawJobs,
    );

    const jobs = await getPendingPrinterJobs(testIp);

    expect(jobs).toHaveLength(1);
    expect(jobs[0]?.metadata).toEqual({ type: "Receipt", orderId: "12345" });
  });

  it("should handle errors and return empty array", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.getPrinterPendingJobDetails.mockRejectedValue(
      new Error("Get jobs error"),
    );

    const jobs = await getPendingPrinterJobs(testIp);

    expect(jobs).toEqual([]);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("deletePrinterPendingJobs", () => {
  it("should delete printer pending jobs successfully", async () => {
    NativeModules.PosThermalPrinter.dismissPendingJobs.mockResolvedValue(true);

    const result = await deletePrinterPendingJobs(testIp);

    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.dismissPendingJobs,
    ).toHaveBeenCalledWith(testIp);
  });

  it("should handle errors when deleting printer pending jobs", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.dismissPendingJobs.mockRejectedValue(
      new Error("Delete error"),
    );

    const result = await deletePrinterPendingJobs(testIp);

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("retryPendingJobsFromPrinter", () => {
  it("should retry pending jobs from printer successfully", async () => {
    NativeModules.PosThermalPrinter.retryPendingJobsFromPrinter.mockResolvedValue(
      true,
    );

    const result = await retryPendingJobsFromPrinter(testIp);

    expect(result).toBe(true);
    expect(
      NativeModules.PosThermalPrinter.retryPendingJobsFromPrinter,
    ).toHaveBeenCalledWith(testIp);
  });

  it("should handle errors when retrying pending jobs", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.retryPendingJobsFromPrinter.mockRejectedValue(
      new Error("Retry error"),
    );

    const result = await retryPendingJobsFromPrinter(testIp);

    expect(result).toBe(false);
    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("getPrinterStatus", () => {
  it("should get printer status successfully", async () => {
    const mockStatus = { status: "ready" };
    NativeModules.PosThermalPrinter.checkPrinterStatus.mockResolvedValue(
      mockStatus,
    );

    const result = await getPrinterStatus(testIp);

    expect(result).toEqual(mockStatus);
    expect(
      NativeModules.PosThermalPrinter.checkPrinterStatus,
    ).toHaveBeenCalledWith(testIp);
  });

  it("should handle errors when getting printer status", async () => {
    const consoleSpy = jest.spyOn(console, "error").mockImplementation();
    NativeModules.PosThermalPrinter.checkPrinterStatus.mockRejectedValue(
      new Error("Status error"),
    );

    await getPrinterStatus(testIp);

    expect(consoleSpy).toHaveBeenCalled();
    consoleSpy.mockRestore();
  });
});

describe("EscPosPrinter Proxy", () => {
  it("should use PosThermalPrinter when available", () => {
    const mockPrinter = {
      setPrintJobs: jest.fn(),
    };
    NativeModules.PosThermalPrinter = mockPrinter;

    jest.isolateModules(() => {
      const { EscPosPrinter } = require("../src/printerModule");
      expect(EscPosPrinter).toBe(mockPrinter);
    });
  });

  it("should throw LINKING_ERROR when native module is not available", () => {
    // Remove PosThermalPrinter
    delete NativeModules.PosThermalPrinter;

    let proxyEscPosPrinter: any;
    jest.isolateModules(() => {
      const { EscPosPrinter } = require("../src/printerModule");
      proxyEscPosPrinter = EscPosPrinter;
    });

    // Test should throw when accessing any method
    expect(() => {
      proxyEscPosPrinter.setPrintJobs();
    }).toThrow(LINKING_ERROR);
  });

  it("should throw LINKING_ERROR when native module is null", () => {
    // Remove PosThermalPrinter
    NativeModules.PosThermalPrinter = null;

    let proxyEscPosPrinter: any;
    jest.isolateModules(() => {
      const { EscPosPrinter } = require("../src/printerModule");
      proxyEscPosPrinter = EscPosPrinter;
    });

    // Test should throw when accessing any method
    expect(() => {
      proxyEscPosPrinter.setPrintJobs();
    }).toThrow(LINKING_ERROR);
  });
});
