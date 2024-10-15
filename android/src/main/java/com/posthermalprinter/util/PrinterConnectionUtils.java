package com.posthermalprinter.util;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PrinterConnectionUtils {

  private static final String TAG = "PrinterUtils";
  private final ReactApplicationContext reactContext;
  private final Map<String, Boolean> reachabilityMap = new ConcurrentHashMap<>();
  private boolean showLogs = false;
  private Process p;
  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private List<String> currentPrinterIps = new ArrayList<>();

  public PrinterConnectionUtils(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  // Blocking version of pingHost
  public boolean pingHostBlocking(String host) {
    boolean result = false;
    BufferedReader bufferedReader = null;

    try {
      p = Runtime.getRuntime().exec("ping -c 1 -w 5 " + host);
      InputStream ins = p.getInputStream();
      InputStreamReader reader = new InputStreamReader(ins);
      bufferedReader = new BufferedReader(reader);

      while (bufferedReader.readLine() != null) {
        // Reading output
      }

      int status = p.waitFor();
      result = (status == 0);

      if (showLogs) {
        Log.i(TAG, result ? "Ping successful: " + host : "Ping failed: " + host);
      }

    } catch (IOException | InterruptedException e) {
      Log.e(TAG, "Error during ping: " + host, e);
      result = false;
    } finally {
      if (p != null) {
        p.destroy();
      }
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return result;
  }

  // Method to start periodic reachability checks every 15 seconds
  public void startPeriodicReachabilityCheck(List<String> printerIps) {
    if (showLogs) {
      Log.i(TAG, "Starting periodic reachability checks.");
    }
    this.currentPrinterIps = new ArrayList<>(printerIps); // Ensure the list is properly updated
    scheduler.scheduleAtFixedRate(() -> {
      for (String printerIp : currentPrinterIps) {
        boolean reachable = pingHostBlocking(printerIp);
        reachabilityMap.put(printerIp, reachable); // Update the reachability status
        if (showLogs) {
          Log.i(TAG, "Printer " + printerIp + " reachability: " + reachable);
        }
        if (!reachable) {
          sendPrinterUnreachableEvent(printerIp);
        }
      }

      if (showLogs) {
        Log.i(TAG, "Updated reachability status for printers. Current map: " + reachabilityMap);
      }
    }, 0, 5, TimeUnit.SECONDS); // Run every 15 seconds
  }

  // Restart method to reinitialize and start reachability checks
  public void restartPeriodicCheck(List<String> printerIps) {
    stopPeriodicCheck(); // Stop the current scheduler
    this.currentPrinterIps = new ArrayList<>(printerIps); // Update the list with the new printer IPs
    scheduler = Executors.newScheduledThreadPool(1); // Reinitialize the scheduler
    startPeriodicReachabilityCheck(printerIps); // Start the check again with updated IPs
  }

  // Method to add a new printer and restart the checks
  public void addPrinterAndRestart(String newPrinterIp) {
    if (!currentPrinterIps.contains(newPrinterIp)) {

      Log.i(TAG, "Adding new printer IP: " + newPrinterIp);
      currentPrinterIps.add(newPrinterIp);
      restartPeriodicCheck(currentPrinterIps); // Restart with updated list
    } else {
      Log.i(TAG, "Printer IP already exists in the list: " + newPrinterIp);
    }
  }

  public int printerLength() {
    return currentPrinterIps.size();
  }

  public Map<String, Boolean> getReachabilityMap() {
    return reachabilityMap;
  }

  // Method to get the reachability status of a specific IP
  public Boolean isReachable(String printerIp) {
    return reachabilityMap.get(printerIp);
  }

  // Method to remove a printer and restart the checks
  public void removePrinterAndRestart(String printerIp) {
    if (currentPrinterIps.contains(printerIp)) {
      Log.i(TAG, "Removing printer IP: " + printerIp);
      currentPrinterIps.remove(printerIp); // Remove the printer from the list
      reachabilityMap.remove(printerIp); // Remove from reachability map
      restartPeriodicCheck(currentPrinterIps); // Restart with updated list
    } else {
      Log.i(TAG, "Printer IP not found in the list: " + printerIp);
    }
  }

  private void sendPrinterUnreachableEvent(String printerIp) {
    WritableMap params = Arguments.createMap();
    params.putString("printerIp", printerIp);
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit("PrinterUnreachable", params);
  }

  // Stop the scheduled checks and shutdown the executor
  public void stopPeriodicCheck() {
    Log.i(TAG, "Stopping periodic reachability checks.");
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      Log.e(TAG, "Error stopping scheduler: ", e);
      scheduler.shutdownNow();
    }
  }
}
