package com.posthermalprinter.helper;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.utils.PosPrinterDev;
import com.posthermalprinter.util.PrinterStatus;

/**
 * Manages the status of printers in the printer pool.
 * This class provides methods to retrieve and check the status of printers.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class PrinterStatusManager {

  /**
   * Retrieves the status of all printers in the printer pool.
   *
   * @param binder The IMyBinder instance used to interact with the printer service.
   * @return A List of PrinterStatus objects representing the status of each printer in the pool.
   *         Returns an empty list if the binder is null or if no printers are found.
   */
  public List<PrinterStatus> getPrinterPoolStatus(IMyBinder binder) {
    if (binder == null) {
      return new ArrayList<>();
    }

    ArrayList<PosPrinterDev.PrinterInfo> printerList = binder.GetPrinterInfoList();
    List<PrinterStatus> statusList = new ArrayList<>();

    if (printerList != null) {
      for (PosPrinterDev.PrinterInfo printerInfo : printerList) {
        statusList.add(createPrinterStatus(printerInfo));
      }
    }

    return statusList;
  }

  /**
   * Creates a PrinterStatus object from a PrinterInfo object.
   *
   * @param printerInfo The PrinterInfo object containing printer details.
   * @return A PrinterStatus object representing the current status of the printer.
   */
  private PrinterStatus createPrinterStatus(PosPrinterDev.PrinterInfo printerInfo) {
    boolean isReachable = PrinterUtils.isPrinterReachable(printerInfo.portInfo);
    return new PrinterStatus(printerInfo.portInfo, isReachable, "PrinterName_" + printerInfo.portInfo);
  }

  /**
   * Checks the real-time reachability of a specific printer.
   *
   * @param printerIp The IP address of the printer to check.
   * @return true if the printer is reachable, false otherwise.
   */
  public boolean checkPrinterReachability(String printerIp) {
    return PrinterUtils.isPrinterReachable(printerIp);
  }
}
