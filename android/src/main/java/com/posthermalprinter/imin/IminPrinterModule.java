package com.posthermalprinter.imin;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.imin.library.SystemPropManager;
import com.imin.printerlib.Callback;
import com.imin.printerlib.IminPrintUtils;
import com.imin.printerlib.print.PrintUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class IminPrinterModule extends ReactContextBaseJavaModule {
  public static final String NAME = "IminPrinter";
  private static final String TAG = "IminInnerPrinterModule";
  //  public static ReactApplicationContext reactApplicationContext;
  private IminPrintUtils mIminPrintUtils;


  public IminPrinterModule(ReactApplicationContext reactContext) {

    mIminPrintUtils = IminPrintUtils.getInstance(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @SuppressLint("NewApi")
  @RequiresApi(api = Build.VERSION_CODES.N)
  public Boolean initPrinter() throws ExecutionException, InterruptedException {
    final IminPrintUtils printUtils = mIminPrintUtils;
    String deviceModel = SystemPropManager.getModel();
    List<String> spiDeviceList = new ArrayList<>(Arrays.asList("M2-202", "M2-203", "M2-Pro"));
    List<String> usbDeviceList = new ArrayList<>(Arrays.asList("S1-701", "S1-702", "D1p-601", "D1p-602", "D1p-603", "D1p-604", "D1w-701", "D1w-702", "D1w-703", "D1w-704", "D4-501", "D4-502", "D4-503", "D4-504", "D4-505", "M2-Max", "D1", "D1-Pro", "Swift 1", "I22T01", "I20D01", "D4-504 Pro", "I23D01"));

    CompletableFuture<Boolean> result = new CompletableFuture<>();

    // Add a timeout mechanism
    CompletableFuture.delayedExecutor(800, TimeUnit.MILLISECONDS).execute(() -> {
      if (!result.isDone()) {
        Log.w(TAG, "Printer initialization timed out after 500ms");
        result.complete(false);
      }
    });

    ThreadPoolManager.getInstance().executeTask(new Runnable() {
      @RequiresApi(api = Build.VERSION_CODES.N)
      @Override
      public void run() {
        try {
          if (spiDeviceList.contains(deviceModel)) {
            printUtils.resetDevice();
            printUtils.initPrinter(IminPrintUtils.PrintConnectType.SPI);
            printUtils.getPrinterStatus(IminPrintUtils.PrintConnectType.SPI, new Callback() {
              @Override
              public void callback(int status) {
                if (!result.isDone()) {  // Only complete if not already completed by timeout
                  if (status == -1 && PrintUtils.getPrintStatus() == -1) {
                    result.complete(false);
                  } else {
                    result.complete(true);
                  }
                }
              }
            });
          } else if (usbDeviceList.contains(deviceModel)) {
            printUtils.resetDevice();
            Log.d("initPrinter", "Usb is executing");
            printUtils.initPrinter(IminPrintUtils.PrintConnectType.USB);
            int status = printUtils.getPrinterStatus(IminPrintUtils.PrintConnectType.USB);
            if (!result.isDone()) {  // Only complete if not already completed by timeout
              result.complete(status > -1);
            }
          } else {
            // Handle unknown device model
            if (!result.isDone()) {
              Log.w(TAG, "Unknown device model: " + deviceModel);
              result.complete(false);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          if (!result.isDone()) {  // Only complete if not already completed by timeout
            Log.e(TAG, "Error initializing printer: " + e.getMessage());
            result.complete(false);
          }
        }
      }
    });

    return result.get();
  }


  public void sendRawData(byte[] bytes) {
    final IminPrintUtils printUtils = mIminPrintUtils;
    ThreadPoolManager.getInstance().executeTask(new Runnable() {
      @Override
      public void run() {
        try {
          printUtils.sendRAWData(bytes);
        } catch (Exception e) {
          e.printStackTrace();
          Log.i(TAG, "ERROR: " + e.getMessage());
        }
      }
    });
  }

}
