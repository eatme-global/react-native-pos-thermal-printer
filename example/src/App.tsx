/* eslint-disable react-native/no-inline-styles */
import {
  Alert,
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

import {
  addPrinterToPool,
  deletePendingJob,
  deletePrinterPendingJobs,
  EventServiceProvider,
  getPendingPrinterJobs,
  getPrinterPoolStatus,
  getPrinterStatus,
  initializePrinterPool,
  PrintAlignment,
  PrintFontSize,
  PrintFontWeight,
  PrintJobRowType,
  printPendingJobsWithNewPrinter,
  printText,
  reconnectPrinter,
  PosPrinterType,
  removePrinterFromPool,
  retryPendingJobFromNewPrinter,
  retryPendingJobsFromPrinter,
} from "react-native-pos-thermal-printer";

import {
  type IPosPrinter,
  type ParsedPendingJob,
  type PrinterStatus,
  type PrintJobRow,
} from "react-native-pos-thermal-printer";

import { useCallback, useState } from "react";

export default function App() {
  const customRetryFunction = useCallback(
    async (printerIp: string | { ip: string }) => {
      // Implement a reconnect mechanism [reconnect printer]
      Alert.alert(
        "Printer Unreachable",
        `Unable to connect to printer at IP ${printerIp}`,
      );
    },
    [],
  );
  return (
    <EventServiceProvider onReconnect={customRetryFunction}>
      <PrinterContent />
    </EventServiceProvider>
  );
}

const PrinterContent: React.FC = () => {
  const [printerPoolStatus, setPrinterPoolStatus] = useState<
    PrinterStatus[] | []
  >([]);

  interface PrinterIPs {
    [jobId: string]: string;
  }

  const [printerIPs, setPrinterIPs] = useState<{ [jobId: string]: string }>({});

  const handleIPChange = (jobId: string, text: string): void => {
    setPrinterIPs((prevState: PrinterIPs) => ({
      ...prevState,
      [jobId]: text,
    }));
  };

  const [pendingJobs, setPendingJobs] = useState<ParsedPendingJob[]>([]);

  const handlePrintTest = (ip: string, printerType: PosPrinterType) => {
    const result: PrintJobRow[] = [
      {
        type: PrintJobRowType.IMAGE,
        url: "https://i.pinimg.com/736x/fa/66/73/fa66736df84509ac13e05c9372131550.jpg",
        width: 100,
        printerWidth: 190.0,
        alignment: PrintAlignment.CENTER,
      },
      {
        type: PrintJobRowType.TEXT,
        text: "Table 4",
        bold: PrintFontWeight.NORMAL,
        alignment: PrintAlignment.CENTER,
        fontSize: PrintFontSize.BIG,
        wrapWords: false,
      },
      {
        type: PrintJobRowType.TEXT,
        text: "Dine-in",
        bold: PrintFontWeight.NORMAL,
        alignment: PrintAlignment.CENTER,
        fontSize: PrintFontSize.BIG,
        wrapWords: false,
      },
      {
        type: PrintJobRowType.TEXT,
        text: "------------------------",
        bold: PrintFontWeight.BOLD,
        alignment: PrintAlignment.CENTER,
        fontSize: PrintFontSize.WIDE,
        wrapWords: false,
      },
      {
        type: PrintJobRowType.TEXT,
        text: "String with ƒäñçÿ ",
        bold: PrintFontWeight.NORMAL,
        alignment: PrintAlignment.CENTER,
        fontSize: PrintFontSize.WIDE,
        wrapWords: false,
      },
      {
        type: PrintJobRowType.TEXT,
        text: "String with ƒäñçÿ ",
        bold: PrintFontWeight.NORMAL,
        alignment: PrintAlignment.CENTER,
        fontSize: PrintFontSize.WIDE,
        wrapWords: false,
      },
      {
        type: PrintJobRowType.TEXT,
        text: "String with ƒäñçÿ ",
        bold: PrintFontWeight.NORMAL,
        alignment: PrintAlignment.CENTER,
        fontSize: PrintFontSize.WIDE,
        wrapWords: false,
      },

      { type: PrintJobRowType.FEED, lines: 2 },
      {
        type: PrintJobRowType.COLUMN,
        bold: true,
        fontSize: PrintFontSize.NORMAL,
        columns: [
          { text: "TRY", width: 20, alignment: PrintAlignment.LEFT },
          { text: "", width: 2, alignment: PrintAlignment.LEFT },
          { text: "Qty", width: 5, alignment: PrintAlignment.CENTER },
          { text: "", width: 2, alignment: PrintAlignment.LEFT },
          { text: "Price", width: 19, alignment: PrintAlignment.RIGHT },
        ],
      },
      // {
      //   type: PrintJobRowType.COLUMN,
      //   bold: true,
      //   fontSize: PrintFontSize.NORMAL,
      //   columns: [
      //     { text: "TRY", width: 20, alignment: PrintAlignment.LEFT },
      //     { text: "", width: 2, alignment: PrintAlignment.LEFT },
      //     { text: "Qty", width: 5, alignment: PrintAlignment.CENTER },
      //     { text: "", width: 2, alignment: PrintAlignment.LEFT },
      //     { text: "Price", width: 19, alignment: PrintAlignment.RIGHT },
      //   ],
      // },
      // { type: PrintJobRowType.FEED, lines: 1 },
      // {
      //   type: PrintJobRowType.TEXT,
      //   text: "String with ƒäñçÿ ",
      //   bold: PrintFontWeight.NORMAL,
      //   alignment: PrintAlignment.CENTER,
      //   fontSize: PrintFontSize.WIDE,
      //   wrapWords: false,
      // },
      // {
      //   type: PrintJobRowType.TEXT,
      //   text: "------------------------",
      //   bold: PrintFontWeight.NORMAL,
      //   alignment: PrintAlignment.CENTER,
      //   fontSize: PrintFontSize.WIDE,
      //   wrapWords: false,
      // },

      {
        type: PrintJobRowType.CASHBOX,
      },
      {
        type: PrintJobRowType.IMAGE,
        width: 100,
        url: "https://png.pngtree.com/png-clipart/20190921/original/pngtree-beautiful-black-and-white-butterfly-png-image_4699516.jpg",
        alignment: PrintAlignment.CENTER,
        printerWidth: 190.0,
      },
      {
        type: PrintJobRowType.IMAGE,
        width: 100,
        url: "https://logos-world.net/wp-content/uploads/2020/04/Adidas-Logo-1950-1971.png",
        alignment: PrintAlignment.CENTER,
        printerWidth: 190.0,
      },
      // {
      //   type: PrintJobRowType.QRCODE,
      //   text: "https://upload.wikimedia.org/wikipedia/commons/2/24/Adidas_logo.png",
      //   alignment: PrintAlignment.RIGHT,
      // },
      // {
      //   type: PrintJobRowType.FEED,
      //   lines: Platform.OS === "ios" ? 5 : 0,
      // },
      { type: PrintJobRowType.CUT },
    ];

    // for (let i = 0; i < 6; i++) {
    const printer: IPosPrinter = {
      ip: ip,
      type: printerType,
    };
    printText(printer, result, { type: "Receipt", category: "Drinks" });
    // }
  };

  const addNewPrinterToPool = async (
    ip: string,
    printerType: PosPrinterType,
  ) => {
    const printer: IPosPrinter = {
      ip: ip,
      type: printerType,
    };

    const status = await addPrinterToPool(printer);
    console.log("addPrinterToPoolStatus: ", status);
    if (status) {
      handlePrinterPoolStatus();
    }
  };

  const handleReConnectPrinter = async (
    ip: string,
    printerType: PosPrinterType,
  ) => {
    const printer: IPosPrinter = {
      ip: ip,
      type: printerType,
    };
    const status = await reconnectPrinter(printer);
    if (status) {
      handlePrinterPoolStatus();
    }
  };

  const handlePrinterPoolStatus = async () => {
    const status = await getPrinterPoolStatus();
    setPrinterPoolStatus(status);
  };

  const handleRemovePrinter = async (
    ip: string,
    printerType: PosPrinterType,
  ) => {
    const printer: IPosPrinter = {
      ip: ip,
      type: printerType,
    };
    const status = await removePrinterFromPool(printer);
    if (status) {
      handlePrinterPoolStatus();
    }
  };

  const handleDeleteJob = async (jobId: string) => {
    const status = await deletePendingJob(jobId);
    if (status) {
      // fetchPendingJobs();
    }
  };

  // const fetchPendingJobs = async () => {
  // const result = await getPendingJobs();
  // setPendingJobs(result);
  // };

  const fetchPrinterPendingJobs = async (ip: string, type: PosPrinterType) => {
    const printer: IPosPrinter = {
      ip,
      type,
    };
    const result = await getPendingPrinterJobs(printer);
    setPendingJobs(result);
  };

  const deletePendingJobs = async (
    ip: string,
    type: PosPrinterType.NETWORK,
  ) => {
    const printer: IPosPrinter = {
      ip,
      type,
    };
    const result = await deletePrinterPendingJobs(printer);
    if (result) {
      setPendingJobs([]);
    }
  };

  const retryPendingPrinterJobs = async (
    ip: string,
    printerType: PosPrinterType,
  ) => {
    const printer: IPosPrinter = {
      ip: ip,
      type: printerType,
    };
    const result = await retryPendingJobsFromPrinter(printer);
    if (result) {
      // fetchPendingJobs();
      fetchPrinterPendingJobs(ip, printerType);
    }
  };

  const handlePendingJobPrintFromNewPrinter = async (jobId: string) => {
    const printer: IPosPrinter = {
      ip: printerIPs[jobId] || "",
      type:
        printerIPs[jobId] !== ""
          ? PosPrinterType.NETWORK
          : PosPrinterType.INTERNAL,
    };
    // if (newPrinterIp) {
    const status = await retryPendingJobFromNewPrinter(jobId, printer);
    if (status) {
      // fetchPendingJobs();
    }
    // }
  };

  const getPrinterStatusPrinter = async (
    ip: string,
    printerType: PosPrinterType,
  ) => {
    const printer: IPosPrinter = {
      ip: ip,
      type: printerType,
    };
    await getPrinterStatus(printer);
  };

  const [ipOld, setOldIp] = useState<string>("");
  const [ipNew, setNewIp] = useState<string>("");
  const [ip, setIp] = useState<string>("");

  return (
    <EventServiceProvider
      onReconnect={async (IP: string) => {
        console.info(IP);
      }}
      onBeforePrint={() => console.info("before print")}
    >
      <SafeAreaView>
        <ScrollView style={{}}>
          <Button
            title="Initialize Printer"
            onPress={() => initializePrinterPool()}
          />
          <View style={styles.container}>
            <TextInput
              style={styles.textInput}
              onChange={(e) => setIp(e.nativeEvent.text)}
              placeholder="Enter IP"
            />

            <Button
              title="Add Printer"
              onPress={() => addNewPrinterToPool(ip, PosPrinterType.NETWORK)}
            />

            <View style={{ marginTop: 10 }}>
              <Button
                title="Initialize Internal Printer"
                onPress={() => addNewPrinterToPool("", PosPrinterType.INTERNAL)}
              />
            </View>
            <View style={styles.printerPoolStatusContainer}>
              <Text style={styles.printerPoolHeading}>
                Printer Pool Status: {}
              </Text>

              <Button
                title="Get Printer Pool Status"
                onPress={handlePrinterPoolStatus}
              />
              <View>
                {printerPoolStatus.length > 0 &&
                  printerPoolStatus.map((printerStatus, index) => (
                    <View
                      key={index}
                      style={{
                        flexDirection: "column",
                        borderWidth: 1,
                        borderColor: "gray",
                        backgroundColor: printerStatus.isReachable
                          ? "#c8f7d6"
                          : "#ffd4d9",
                        marginBottom: 10,
                        padding: 10,
                      }}
                    >
                      <Text>Printer Name:{printerStatus.printerName}</Text>
                      <Text>Printer IP:{printerStatus.printerIp}</Text>
                      <Text>
                        IP Status:{" "}
                        {printerStatus.isReachable
                          ? "Reachable"
                          : "Not Reachable"}
                      </Text>
                      <View style={{ marginTop: 10, flexDirection: "column" }}>
                        <TouchableOpacity
                          onPress={() =>
                            handlePrintTest(
                              printerStatus.printerIp,
                              ip === PosPrinterType.INTERNAL
                                ? PosPrinterType.INTERNAL
                                : PosPrinterType.NETWORK,
                            )
                          }
                          style={{
                            backgroundColor: "gray",
                            paddingVertical: 10,
                            paddingHorizontal: 20,
                          }}
                        >
                          <Text style={{ color: "white", textAlign: "center" }}>
                            Printer Test
                          </Text>
                        </TouchableOpacity>

                        <TouchableOpacity
                          onPress={() =>
                            handleRemovePrinter(
                              printerStatus.printerIp,
                              ip === PosPrinterType.INTERNAL
                                ? PosPrinterType.INTERNAL
                                : PosPrinterType.NETWORK,
                            )
                          }
                          style={{
                            backgroundColor: "red",
                            paddingVertical: 10,
                            paddingHorizontal: 20,
                          }}
                        >
                          <Text style={{ color: "white", textAlign: "center" }}>
                            Printer Remove
                          </Text>
                        </TouchableOpacity>

                        {!printerStatus.isReachable && (
                          <TouchableOpacity
                            onPress={() =>
                              handleReConnectPrinter(
                                printerStatus.printerIp,
                                ip === PosPrinterType.INTERNAL
                                  ? PosPrinterType.INTERNAL
                                  : PosPrinterType.NETWORK,
                              )
                            }
                            style={{
                              backgroundColor: "orange",
                              paddingVertical: 10,
                              paddingHorizontal: 20,
                            }}
                          >
                            <Text
                              style={{ color: "white", textAlign: "center" }}
                            >
                              Reconnect Printer
                            </Text>
                          </TouchableOpacity>
                        )}
                        <TouchableOpacity
                          onPress={() =>
                            getPrinterStatusPrinter(
                              printerStatus.printerIp,
                              ip === PosPrinterType.INTERNAL
                                ? PosPrinterType.INTERNAL
                                : PosPrinterType.NETWORK,
                            )
                          }
                          style={{
                            backgroundColor: "orange",
                            paddingVertical: 10,
                            paddingHorizontal: 20,
                          }}
                        >
                          <Text style={{ color: "white", textAlign: "center" }}>
                            Get Printer Status
                          </Text>
                        </TouchableOpacity>
                      </View>
                    </View>
                  ))}
              </View>
              <View
                style={{
                  height: 2,
                  backgroundColor: "black",
                  width: 300,
                  marginVertical: 20,
                }}
              />
              <View style={{}}>
                <Text style={styles.printerPoolHeading}>
                  Reroute pending jobs
                </Text>
                <TextInput
                  style={{ ...styles.textInput, width: "100%" }}
                  onChange={(e) => setOldIp(e.nativeEvent.text)}
                  placeholder="Pending jobs IP"
                />

                <TextInput
                  style={{ ...styles.textInput, width: "100%" }}
                  onChange={(e) => setNewIp(e.nativeEvent.text)}
                  placeholder="New printer IP"
                />
              </View>

              <Button
                title="Print Pending Jobs with New Printer"
                onPress={() =>
                  printPendingJobsWithNewPrinter(
                    { ip: ipOld, type: PosPrinterType.NETWORK },
                    { ip: ipNew, type: PosPrinterType.NETWORK },
                  )
                }
              />
              <View
                style={{
                  height: 2,
                  backgroundColor: "black",
                  width: 300,
                  marginVertical: 20,
                }}
              />

              <Button
                title="delete pending Jobs 127"
                onPress={() =>
                  deletePendingJobs("192.168.8.127", PosPrinterType.NETWORK)
                }
              />
              {pendingJobs.length > 0 ? (
                <Button
                  title="retry Pending Jobs 127"
                  onPress={() =>
                    retryPendingPrinterJobs(
                      "192.168.8.127",
                      PosPrinterType.NETWORK,
                    )
                  }
                />
              ) : (
                <View />
              )}
              <View>
                {pendingJobs.length > 0 &&
                  pendingJobs.map((job, index) => {
                    return (
                      <View
                        key={index}
                        style={{
                          flexDirection: "column",
                          borderWidth: 1,
                          borderColor: "gray",
                          backgroundColor: "#ffd4d9",
                          padding: 10,
                        }}
                      >
                        <Text>Job ID:{job.jobId}</Text>
                        <Text>Printer Name:{job.printerName}</Text>
                        <Text>Printer IP:{job.printerName}</Text>
                        <Text>Print Type:{job.metadata.type}</Text>
                        <Text>
                          Print Metadata:{JSON.stringify(job.metadata)}
                        </Text>
                        <TouchableOpacity
                          onPress={() => handleDeleteJob(job.jobId)}
                          style={{
                            backgroundColor: "red",
                            paddingVertical: 10,
                            paddingHorizontal: 20,
                          }}
                        >
                          <Text style={{ color: "white", textAlign: "center" }}>
                            Remove Job
                          </Text>
                        </TouchableOpacity>

                        <TextInput
                          style={{
                            borderWidth: 1,
                            borderColor: "gray",
                            padding: 10,
                            marginVertical: 5,
                          }}
                          placeholder="Enter new printer IP"
                          value={printerIPs[job.jobId] || ""}
                          onChangeText={(text) =>
                            handleIPChange(job.jobId, text)
                          }
                        />

                        <TouchableOpacity
                          onPress={() =>
                            handlePendingJobPrintFromNewPrinter(job.jobId)
                          }
                          style={{
                            backgroundColor: "blue",
                            paddingVertical: 10,
                            paddingHorizontal: 20,
                            marginVertical: 5,
                          }}
                        >
                          <Text style={{ color: "white", textAlign: "center" }}>
                            Print Job From New Printer
                          </Text>
                        </TouchableOpacity>
                      </View>
                    );
                  })}
              </View>
            </View>
            <Button
              title="Get Pending Jobs 127"
              onPress={() =>
                fetchPrinterPendingJobs("192.168.8.127", PosPrinterType.NETWORK)
              }
            />
          </View>
        </ScrollView>
      </SafeAreaView>
    </EventServiceProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginVertical: 50,
    alignItems: "center",
    justifyContent: "center",
  },
  textInput: {
    width: "80%",
    borderColor: "gray",
    borderWidth: 1,
    marginBottom: 10,
    padding: 10,
  },
  printerPoolStatusContainer: { gap: 5, marginTop: 20, marginBottom: 10 },
  printerPoolHeading: {
    textAlign: "center",
    fontWeight: "bold",
    marginTop: 10,
    marginBottom: 10,
  },
});
