package com.posthermalprinter.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.posthermalprinter.PrinterManager;
import com.facebook.react.bridge.ReactApplicationContext;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.service.PosprinterService;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Initializes and manages the printer service connection.
 * This class provides methods to initialize the printer service and retrieve
 * the binder and printer manager instances.
 */
public class PrinterServiceInitializer {

  private static IMyBinder binder;
  private static PrinterManager printerManager;

  public static boolean initialized;

  /**
   * Asynchronously initializes the printer service.
   *
   * @param reactContext The ReactApplicationContext used to bind the service.
   * @return A CompletableFuture that completes with true if the service is successfully initialized,
   * false if binding fails, or exceptionally if an error occurs.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public static CompletableFuture<Boolean> initializeServiceAsync(ReactApplicationContext reactContext) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();

    if (initialized) {
      future.complete(true);
      Log.i("initializeServiceAsync", "already initialized");
    } else {
      try {
        Log.i("initializeService", "Initializing service");

        ServiceConnection serviceConnection = new ServiceConnection() {
          @Override
          public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (IMyBinder) service;
            initialized = true;
            Log.i("onServiceConnected", "Service Connected Successfully");
            future.complete(true);
          }

          @Override
          public void onServiceDisconnected(ComponentName name) {
            binder = null;
            Log.i("onServiceDisconnected", "Service Disconnected Successfully");
            future.completeExceptionally(new Exception("Service Disconnected"));
          }
        };

        printerManager = new PrinterManager(new ArrayList<String>(), reactContext);

        Intent intent = new Intent(reactContext, PosprinterService.class);
        boolean success = reactContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (!success) {
          Log.e("initializeService", "Failed to bind service");
          future.complete(false);
        } else {
          Log.i("initializeService", "Service binding initiated");
        }

        // Handle future cancellation
        future.whenComplete((result, throwable) -> {
          if (future.isCancelled()) {
            reactContext.unbindService(serviceConnection);
            Log.i("initializeService", "Service unbound due to future cancellation");
          }
        });

      } catch (Exception e) {
        Log.e("initializeService", "Exception on catch block", e);
        future.completeExceptionally(e);
      }
    }

    return future;
  }

  /**
   * Retrieves the binder instance for the printer service.
   *
   * @return The IMyBinder instance, or null if the service is not connected.
   */
  public static IMyBinder getBinder() {
    return binder;
  }

  /**
   * Retrieves the printer manager instance.
   *
   * @return The PrinterManager instance.
   */
  public static PrinterManager getPrinterManager() {
    return printerManager;
  }
}

