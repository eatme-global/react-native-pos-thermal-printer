package com.posthermalprinter

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.posthermalprinter.helper.PrintJobHandler
import com.posthermalprinter.helper.PrinterServiceInitializer
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import net.posprinter.posprinterface.IMyBinder


class PosThermalPrinterModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {


  private var printerManager: PrinterManager? = null

  override fun getName(): String {
    return NAME
  }

  //region Printer Connection & Pool Management

  /**
   * Initializes the printer pool.
   *
   * @param promise A promise to resolve with the initialization result.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun initializePrinterPool(promise: Promise) {
    PrinterServiceInitializer.initializeServiceAsync(reactContext)
      .thenApply { isInitialized ->
        isInitialized ?: false // Handle potential null value
      }
      .thenAccept { isInitialized ->
        if (isInitialized) {
          printerManager = PrinterServiceInitializer.getPrinterManager()
          binder = PrinterServiceInitializer.getBinder();
          promise.resolve(true)
        } else {
          promise.resolve(false)
        }
      }
      .exceptionally { throwable ->
        Log.e("initializePrinterPool", "Error initializing service", throwable)
        promise.resolve(false)
        null
      }
  }

  /**
   * Retrieves the status of all printers in the pool.
   *
   * @param promise A promise to resolve with the printer pool status.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun getPrinterPoolStatus(promise: Promise) {
    val resultArray = Arguments.createArray();

    try {
      val statusList = printerManager?.getPrinterPoolStatus()

      statusList?.forEach { status ->
        val statusMap = Arguments.createMap()
        statusMap.putString("printerIp", status.printerIp)
        statusMap.putBoolean("isReachable", status.isReachable)
        statusMap.putString("printerName", status.printerName)
        resultArray.pushMap(statusMap)
      }
      promise.resolve(resultArray)
    } catch (e: Exception) {
      Log.e("STATUS_ERROR", "Failed to retrieve printer pool status: ${e.message}")
      promise.resolve(resultArray);
    }
  }

  /**
   * Adds a printer to the printer pool.
   *
   * @param printerConfig Configuration map for the printer to add.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun addPrinterToPool(
    printerConfig: ReadableMap,
    promise: Promise
  ) {
    val ip = printerConfig.getString("ip") ?: ""
    try {
      val result = printerManager?.addPrinterAsync(ip)?.get();
      if (result == true) {
        promise.resolve(true)
      } else {
        promise.resolve(false)
      }
    } catch (e: Exception) {
      promise.reject("REMOVE_PRINTER_ERROR", "Failed to remove printer from pool: ${e.message}")
    }
  }

  /**
   * Removes a printer from the printer pool.
   *
   * @param ip The IP address of the printer to remove.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun removePrinterFromPool(ip: String, promise: Promise) {
    try {
      val result = printerManager?.removePrinterAsync(ip)?.get();
      if (result == true) {
        promise.resolve(true)
      } else {
        promise.resolve(false)
      }
    } catch (e: Exception) {
      promise.reject("REMOVE_PRINTER_ERROR", "Failed to remove printer from pool: ${e.message}")
    }
  }

  /**
   * Retries the connection to a printer.
   *
   * @param ip The IP address of the printer to retry.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun retryPrinterConnection(ip: String, promise: Promise) {
    if (printerManager != null) {
      val result = printerManager?.addPrinterAsync(ip)?.get();
      if(result == true){
        printerManager?.changePendingPrintJobsPrinter(ip, ip);
      }
      promise.resolve(result)
    } else {
      promise.resolve(false)
    }
  }

  /**
   * Retrieves details of pending print jobs.
   *
   * @param promise A promise to resolve with the pending job details.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun getPendingJobDetails(promise: Promise) {
    if (printerManager != null) {
      val result = printerManager?.getPendingJobs();
      promise.resolve(result)
    } else {
      promise.resolve(false)
    }
  }


  /**
   * Deletes pending print jobs.
   *
   * @param jobId The ID of the job to delete.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun deletePendingJobs(jobId: String, promise: Promise) {
    if (printerManager != null) {
      val result = printerManager?.deletePendingJobs(jobId);
      promise.resolve(result)
    } else {
      promise.resolve(false)
    }
  }

  /**
   * Retries a pending print job using a new printer.
   *
   * This React Native method attempts to retry a pending print job identified by [jobId]
   * using a printer with the specified [ip] address. It requires Android N (API 24) or higher.
   *
   * @param jobId The unique identifier of the pending print job to retry.
   * @param ip The IP address of the new printer to use for the retry attempt.
   * @param promise A React Native Promise to resolve with the result of the operation.
   *                Resolves to true if the retry was successful, false otherwise.
   *                Also resolves to false if the printerManager is not initialized.
   *
   * @throws Exception if there's an error during the retry process.
   *
   * @RequiresApi(Build.VERSION_CODES.N) This method requires Android N (API 24) or higher.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun retryPendingJobFromNewPrinter(jobId: String, ip: String, promise: Promise) {
    if (printerManager != null) {
      printerManager?.retryPendingJobFromNewPrinter(jobId, ip);
      promise.resolve(true)
    } else {
      promise.resolve(false)
    }
  }

  // endregion

  //region Print Job Handling Methods

  /**
   * Sets print jobs for a specific printer.
   *
   * @param ip The IP address of the target printer.
   * @param content The content to print.
   * @param metadata Additional metadata for the print job.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.O)
  @ReactMethod
  fun setPrintJobs(ip: String, content: ReadableArray, metadata:String, promise: Promise) {
    try {
      val job = PrintJobHandler.createPrintJob(ip, content, metadata)
      val result = printerManager?.addPrintJob(job)
      promise.resolve(result)
    } catch (e: Exception) {
      promise.reject("PRINT_JOB_ERROR", "Failed to set print jobs: ${e.message}")
    }
  }

  /**
   * Transfers pending print jobs from one printer to another.
   *
   * @param oldPrinterIp The IP address of the original printer.
   * @param newPrinterIp The IP address of the new printer.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun printFromNewPrinter(oldPrinterIp: String, newPrinterIp: String, promise: Promise) {
    printerManager?.changePendingPrintJobsPrinter(oldPrinterIp, newPrinterIp);
    promise.resolve(true);
  }


  /**
   * Transfers pending print jobs from one printer to another.
   *
   * @param printerIp The IP address of the printer.
   * @param promise A promise to resolve with the result of the operation.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  @ReactMethod
  fun checkPrinterStatus(printerIp: String, promise: Promise) {
//    val status = printerManager?.checkPrinterStatus(printerIp);
    promise.resolve(false);
  }


  @ReactMethod
  fun retryPendingJobs(printerIp: String, promise: Promise) {
    promise.resolve(true);
  }

  @ReactMethod
  fun retryPendingJobsFromPrinter(printerIp: String, promise: Promise) {
    promise.resolve(true);
  }

  @ReactMethod
  fun getPrinterPendingJobDetails(printerIp: String, promise: Promise) {
    promise.resolve(intArrayOf());
  }

  @ReactMethod
  fun dismissPendingJobs(printerIp: String, promise: Promise) {
    promise.resolve(intArrayOf());
  }




  //endregion

  /**
   * Cleans up resources when the module is being destroyed.
   */
  @RequiresApi(Build.VERSION_CODES.N)
  override fun invalidate() {
    // This method is called when the module is being destroyed
    printerManager?.shutdown()
    super.invalidate()
  }


  companion object {
    const val NAME = "PosThermalPrinter"
    var binder: IMyBinder? = null
  }


}
