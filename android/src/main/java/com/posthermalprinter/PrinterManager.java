
package com.posthermalprinter;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.posthermalprinter.helper.*;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.posthermalprinter.util.PrinterJob;
import com.posthermalprinter.util.PrinterStatus;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.TaskCallback;
import net.posprinter.utils.PosPrinterDev;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages printer operations including adding, removing printers, and handling print jobs.
 */
  public class PrinterManager {

  private static final String TAG = "PrinterManager";
  private final List<String> printerPool;
  private final BlockingQueue<PrinterJob> printQueue; // A queue for print jobs
  private final ExecutorService printExecutor;
  private final PrintQueueProcessor queueProcessor;
  private final PrinterEventManager eventManager;
  private final PrinterStatusManager statusManager;


//  If you need a live monitoring
//  private final PrinterConnectionUtils printerConnectionUtils;

  /**
   * Constructs a new PrinterManager.
   *
   * @param printerPool List of printer IPs to manage
   * @param reactContext The React Native application context
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public PrinterManager(List<String> printerPool, ReactApplicationContext reactContext) {
    this.printerPool = (printerPool != null) ? printerPool : new ArrayList<>();
    this.printQueue = new LinkedBlockingQueue<>();
    this.printExecutor = Executors.newCachedThreadPool();
    this.eventManager = new PrinterEventManager(reactContext);
    this.queueProcessor = new PrintQueueProcessor(printQueue, printerPool,this, eventManager);
    this.statusManager = new PrinterStatusManager();

//    this.printerConnectionUtils = new PrinterConnectionUtils(reactContext);
  }


  @RequiresApi(api = Build.VERSION_CODES.N)
  private CompletableFuture<Boolean> removePrinterIfExists(String printerIp) {
    CompletableFuture<Boolean> removalResult = new CompletableFuture<>();
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();

    ArrayList<PosPrinterDev.PrinterInfo> list = binder.GetPrinterInfoList();
    if (list == null || list.isEmpty()) {
      return CompletableFuture.completedFuture(true);
    }

    PosPrinterDev.PrinterInfo existingPrinter = list.stream()
      .filter(p -> p.portInfo != null && p.portInfo.equals(printerIp))
      .findFirst()
      .orElse(null);

    if (existingPrinter == null) {
      return CompletableFuture.completedFuture(true);
    }

    PrinterUtils.removePrinter(binder, existingPrinter.printerName, new TaskCallback() {
      @Override
      public void OnSucceed() {
        eventManager.resetPrinterUnreachableStatus(printerIp);
        Log.i(TAG, "Removed Existing Printer Successfully: " + printerIp);
        printerPool.remove(printerIp);
        removalResult.complete(true);
      }

      @Override
      public void OnFailed() {
        Log.e(TAG, "Failed to Remove Existing Printer: " + printerIp);
        removalResult.complete(false);
      }
    });

    return removalResult;
  }

  private void removePrinter(PosPrinterDev.PrinterInfo printer, int listSize, CompletableFuture<Boolean> result) {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();

    PrinterUtils.removePrinter(binder, printer.printerName, new TaskCallback() {
      @RequiresApi(api = Build.VERSION_CODES.N)
      @Override
      public void OnSucceed() {
        Log.i(TAG, "Removed Existing Printer Successfully: " + printer.portInfo);
        eventManager.resetPrinterUnreachableStatus(printer.portInfo);
        printerPool.remove(printer.portInfo);
        result.complete(true);

        if (listSize == 1) {
          Log.i(TAG, "Shutdown Printer (No Printers Available)");
          shutdown();
        }
      }

      @RequiresApi(api = Build.VERSION_CODES.N)
      @Override
      public void OnFailed() {
        Log.e(TAG, "Failed to Remove Existing Printer: " + printer.portInfo);
        result.complete(false);
      }
    });
  }


  @RequiresApi(api = Build.VERSION_CODES.N)
  private CompletableFuture<Boolean> addNewPrinter(String printerIp) {
    CompletableFuture<Boolean> additionResult = new CompletableFuture<>();

    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();

    PrinterUtils.addPrinter(binder, printerIp, new TaskCallback() {
      @Override
      public void OnSucceed() {
        additionResult.complete(true);
      }

      @Override
      public void OnFailed() {
        additionResult.complete(false);
      }
    });

    return additionResult;
  }

  /**
   * Asynchronously adds a printer to the printer pool.
   *
   * @param printerIp The IP address of the printer to add
   * @return A CompletableFuture that resolves to true if the printer was added successfully, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> addPrinterAsync(String printerIp) {
    if (!PrinterUtils.isPrinterReachable(printerIp)) {
      Log.i(TAG, "Printer IP is not reachable: " + printerIp);
      return CompletableFuture.completedFuture(false);
    }

    CompletableFuture<Boolean> result = new CompletableFuture<>();

    removePrinterIfExists(printerIp)
      .thenCompose(removed -> addNewPrinter(printerIp))
      .thenAccept(added -> {
        if (added) {
          printerPool.add(printerIp);
          eventManager.resetPrinterUnreachableStatus(printerIp);
          Log.i(TAG, "Added Printer Successfully: " + printerIp);
          result.complete(true);
        } else {
          Log.e(TAG, "Failed to Add Printer: " + printerIp);
          result.complete(false);
        }
      })
      .exceptionally(ex -> {
        Log.e(TAG, "Exception during printer addition: " + ex.getMessage());
        result.complete(false);
        return null;
      });

    return result;
  }

  /**
   * Asynchronously removes a printer from the printer pool.
   *
   * @param printerIp The IP address of the printer to remove
   * @return A CompletableFuture that resolves to true if the printer was removed successfully, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> removePrinterAsync(String printerIp) {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();

    if (binder == null) {
      Log.e(TAG, "Binder is null");
      return CompletableFuture.completedFuture(false);
    }

    ArrayList<PosPrinterDev.PrinterInfo> printerList = binder.GetPrinterInfoList();
    if (printerList == null || printerList.isEmpty()) {
      Log.i(TAG, "No printers in the list");
      return CompletableFuture.completedFuture(false);
    }

    CompletableFuture<Boolean> result = new CompletableFuture<>();

    Optional<PosPrinterDev.PrinterInfo> printerToRemove = printerList.stream()
      .filter(printer -> printer.portInfo != null && printer.portInfo.equals(printerIp))
      .findFirst();

    if (printerToRemove.isPresent()) {
      removePrinter(printerToRemove.get(), printerList.size(), result);
    } else {
      Log.e(TAG, "Printer not found: " + printerIp);
      result.complete(false);
    }

    return result;
  }


  /**
   * Adds a print job to the print queue.
   *
   * @param job The PrinterJob to add to the queue
   * @return true if the job was added successfully, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public Boolean addPrintJob(PrinterJob job) {
    try {
      printQueue.add(job);
      Log.i("addPrintJob", "Job added ");
      IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
      queueProcessor.processPrintQueue(binder);
      return true;
    } catch (Exception e) {
      Log.e("addPrintJob", "Exception: " + e.toString());
      return false;
    }
  }

  /**
   * Changes the target printer for pending print jobs.
   *
   * @param oldPrinterIp The IP of the old printer
   * @param newPrinterIp The IP of the new printer
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public void changePendingPrintJobsPrinter(String oldPrinterIp, String newPrinterIp){
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    queueProcessor.changePendingPrintJobsPrinter(oldPrinterIp, newPrinterIp, binder);
  }

  /**
   * Retries a pending print job using a new printer.
   *
   * This method attempts to change the target printer for a specific pending print job
   * identified by its job ID. It uses the queue processor to modify the job and assign
   * it to a new printer specified by the IP address.
   *
   * @param jobId The unique identifier of the pending print job to retry.
   * @param printerIp The IP address of the new printer to use for the retry attempt.
   *
   * @throws NullPointerException if the binder is null.
   *
   * @implNote This method retrieves the binder from the EscPosPrinterModule and uses
   *           the queue processor to change the pending print job's target printer.
   *           It does not provide direct feedback on the success of the operation.
   *
   * @see PrintQueueProcessor#changePendingPrintJobToPrinter(String, String, IMyBinder)
   *
   * @RequiresApi(api = Build.VERSION_CODES.N) This method requires Android N (API 24) or higher.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public void retryPendingJobFromNewPrinter(String jobId, String printerIp){
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    queueProcessor.changePendingPrintJobToPrinter(jobId, printerIp, binder);
  }



  /**
   * Retrieves the status of all printers in the printer pool.
   *
   * @return A list of PrinterStatus objects representing the status of each printer
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public List<PrinterStatus> getPrinterPoolStatus() {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    return statusManager.getPrinterPoolStatus(binder);
  }

  /**
   * Sends a print job directly to a printer.
   *
   * @param job The PrinterJob to print
   * @return A CompletableFuture that resolves to true if printing was successful, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> printToPrinter(PrinterJob job) {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();

    CompletableFuture<Boolean> printResult = new CompletableFuture<>();

    if (PrinterUtils.isPrinterReachable(job.getTargetPrinterIp())) {
      if (binder != null) {
        binder.SendDataToPrinter(job.getPrinterName(), new TaskCallback() {
          @Override
          public void OnSucceed() {
            Log.i("Print Job", "Print Job Success");
            printResult.complete(true);
          }

          @Override
          public void OnFailed() {
            Log.e("Print Job", "Print Job Failed");
            printResult.complete(false);
          }
        }, new ProcessData() {
          @Override
          public List<byte[]> processDataBeforeSend() {
            return PrintJobHandler.processDataBeforeSend(job);
          }
        });
      }
    } else {
      Log.i("printToPrinter", "Printer IP is not reachable");
      printResult.complete(false);
    }

    return printResult;
  }

  /**
   * Retrieves all pending print jobs.
   *
   * @return A WritableArray containing information about pending print jobs
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public WritableArray getPendingJobs() {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    return queueProcessor.getPendingJobsForJS(binder);
  }

  /**
   * Deletes a pending print job by its ID.
   *
   * @param jobId The ID of the job to delete
   * @return true if the job was deleted successfully, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public Boolean deletePendingJobs(String jobId)  {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    return queueProcessor.deleteJobById(jobId,binder);
  }

  /**
   * Shuts down the print executor service.
   * This method should be called when the PrinterManager is no longer needed.
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public void shutdown() {
    printExecutor.shutdown();
    try {
      if (!printExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
        printExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      printExecutor.shutdownNow();
    }
  }

}
