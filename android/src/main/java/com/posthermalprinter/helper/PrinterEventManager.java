package com.posthermalprinter.helper;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages printer-related events and notifications.
 * This class handles sending printer unreachable events to the React Native layer
 * and keeps track of which printers have already been reported as unreachable.
 */
public class PrinterEventManager {
  private final ReactApplicationContext reactContext;
  private final Set<String> reportedUnreachablePrinters;

  /**
   * Constructs a new PrinterEventManager.
   *
   * @param reactContext The ReactApplicationContext used for sending events to React Native.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public PrinterEventManager(ReactApplicationContext reactContext){
    this.reactContext = reactContext;
    this.reportedUnreachablePrinters = ConcurrentHashMap.newKeySet();
  }

  /**
   * Sends a printer unreachable event once for a given printer IP.
   * This method ensures that the unreachable event is only sent once per printer
   * until the printer's status is reset.
   *
   * @param printerIp The IP address of the unreachable printer.
   */
  public void sendPrinterUnreachableEventOnce(String printerIp) {
    if (reportedUnreachablePrinters.add(printerIp)) {
      sendPrinterUnreachableEvent(printerIp);
    }
  }

  /**
   * Resets the unreachable status for a given printer IP.
   * After calling this method, sendPrinterUnreachableEventOnce will send
   * an event for this printer IP again if called.
   *
   * @param printerIp The IP address of the printer to reset.
   */
  public void resetPrinterUnreachableStatus(String printerIp) {
    reportedUnreachablePrinters.remove(printerIp);
  }

  /**
   * Sends a printer unreachable event to the React Native layer.
   *
   * @param printerIp The IP address of the unreachable printer.
   */
  private void sendPrinterUnreachableEvent(String printerIp) {
    WritableMap params = Arguments.createMap();
    params.putString("printerIp", printerIp);
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit("PrinterUnreachable", params);
  }
}
