package com.posthermalprinter.helper;

import android.util.Log;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.TaskCallback;
import net.posprinter.utils.PosPrinterDev;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Utility class for printer-related operations.
 * Provides methods for checking printer reachability and managing printers.
 */
public class PrinterUtils {
  static final String TAG = "PrinterUtils";

  /**
   * Checks if a printer is reachable at the given IP address.
   *
   * @param printerIp The IP address of the printer to check.
   * @return true if the printer is reachable, false otherwise.
   */
  public static boolean isPrinterReachable(String printerIp) {
    try {
      InetAddress inet = InetAddress.getByName(printerIp);
      return inet.isReachable(100);
    } catch (IOException e) {
      Log.e(TAG, printerIp + ": Unknown host", e);
    }
    return false;
  }

  /**
   * Adds a printer to the printer management system.
   *
   * @param binder    The IMyBinder instance used to interact with the printer service.
   * @param printerIp The IP address of the printer to add.
   * @param callback  A TaskCallback to handle the result of the add operation.
   */
  public static void addPrinter(IMyBinder binder, String printerIp, TaskCallback callback) {
    PosPrinterDev.PrinterInfo printer = new PosPrinterDev.PrinterInfo("PrinterName_" + printerIp, PosPrinterDev.PortType.Ethernet, printerIp);
    binder.AddPrinter(printer, callback);
  }

  /**
   * Removes a printer from the printer management system.
   *
   * @param binder      The IMyBinder instance used to interact with the printer service.
   * @param printerName The name of the printer to remove.
   * @param callback    A TaskCallback to handle the result of the remove operation.
   */
  public static void removePrinter(IMyBinder binder, String printerName, TaskCallback callback) {
    binder.RemovePrinter(printerName, callback);
  }

}
