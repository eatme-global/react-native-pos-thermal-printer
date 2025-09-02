package com.posthermalprinter;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReadableMap;
import com.posthermalprinter.helper.*;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.posthermalprinter.imin.IminPrinterModule;
import com.posthermalprinter.util.PrintItem;
import com.posthermalprinter.util.PrinterJob;
import com.posthermalprinter.util.PrinterStatus;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.TaskCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
   * @param printerPool  List of printer IPs to manage
   * @param reactContext The React Native application context
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public PrinterManager(List<String> printerPool, ReactApplicationContext reactContext) {
    this.printerPool = (printerPool != null) ? printerPool : new ArrayList<>();
    this.printQueue = new LinkedBlockingQueue<>();
    this.printExecutor = Executors.newCachedThreadPool();
    this.eventManager = new PrinterEventManager(reactContext);
    this.queueProcessor = new PrintQueueProcessor(printQueue, printerPool, this, eventManager);
    this.statusManager = new PrinterStatusManager();

//    this.printerConnectionUtils = new PrinterConnectionUtils(reactContext);
  }


  /**
   * Asynchronously adds a printer to the printer pool.
   *
   * @param printerIp The IP address of the printer to add
   * @return A CompletableFuture that resolves to true if the printer was added successfully, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> addPrinterAsync(String printerIp, String type) throws ExecutionException, InterruptedException {
    CompletableFuture<Boolean> result = new CompletableFuture<>();


    if (!Objects.equals(type, "INTERNAL")) {
      addNewPrinter(printerIp)
        .thenAccept(added -> {
          if (added) {
            if (!printerPool.contains(printerIp)) {
              printerPool.add(printerIp);
            }
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
    } else {
      IminPrinterModule iMinPrinterModule = PosThermalPrinterModule.Companion.getIMinPrinterModule();
      if (iMinPrinterModule != null) {
        Boolean iMinResult = iMinPrinterModule.initPrinter();
        if (!printerPool.contains("INTERNAL")) {
          printerPool.add("INTERNAL");
        }
        result.complete(iMinResult);
      } else {
        result.complete(false);
      }
    }


    return result;
  }


  /**
   * Asynchronously removes a printer from the printer pool.
   *
   * @param printerIp The IP address of the printer to remove
   * @return A CompletableFuture that resolves to true if the printer was removed successfully, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> removePrinterAsync(String printerIp, String type) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();

    if (printerPool.contains(printerIp)) {
      result.complete(true);
      if (type.equals("INTERNAL")) {
        printerPool.remove("INTERNAL");
      } else {
        printerPool.remove(printerIp);
      }
    } else {
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

    // add print job
    eventManager.sendPrePrintEvent();

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
  public void changePendingPrintJobsPrinter(ReadableMap oldPrinterIp, ReadableMap newPrinterIp) {
//    Don't have this method at the moment ***
//    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
//    queueProcessor.changePendingPrintJobsPrinter(oldPrinterIp, newPrinterIp, binder);
  }


  /**
   * Retries a pending print job using a new printer.
   * <p>
   * This method attempts to change the target printer for a specific pending print job
   * identified by its job ID. It uses the queue processor to modify the job and assign
   * it to a new printer specified by the IP address.
   *
   * @param jobId     The unique identifier of the pending print job to retry.
   * @param printerIp The IP address of the new printer to use for the retry attempt.
   * @throws NullPointerException if the binder is null.
   * @implNote This method retrieves the binder from the EscPosPrinterModule and uses
   * the queue processor to change the pending print job's target printer.
   * It does not provide direct feedback on the success of the operation.
   * @RequiresApi(api = Build.VERSION_CODES.N) This method requires Android N (API 24) or higher.
   * @see PrintQueueProcessor#changePendingPrintJobToPrinter(String, String, IMyBinder)
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public void retryPendingJobFromNewPrinter(String jobId, String printerIp, String type) {
//    Don't have this method at the moment ***
//    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
//    queueProcessor.changePendingPrintJobToPrinter(jobId, printerIp, binder);
  }


  /**
   * Sends a print job directly to a printer.
   *
   * @param job The PrinterJob to print
   * @return A CompletableFuture that resolves to true if printing was successful, false otherwise
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> printToPrinter(PrinterJob job) {
    CompletableFuture<Boolean> printResult = new CompletableFuture<>();

    if (job.getTargetPrinterIp().contentEquals("INTERNAL")) {
      IminPrinterModule iminPrinterModule = PosThermalPrinterModule.Companion.getIMinPrinterModule();
      if (iminPrinterModule != null) {
        try {
          List<List<PrintItem>> jobContent = Collections.singletonList(job.getJobContent());
          for (List<PrintItem> printItems : jobContent) {
            List<List<PrintItem>> separatedItems = separatePrintItemsAroundImages(printItems);

            for (List<PrintItem> items : separatedItems) {
              List<byte[]> commands = PrintJobHandler.processDataBeforeSend(items, job.getTargetPrinterIp());
              for (byte[] item : commands) {
                iminPrinterModule.sendRawData(item);

                if (items.get(0).getType() == PrintItem.Type.IMAGE) {
                  Thread.sleep(1000);
                } else {
                  Thread.sleep(5);
                }
              }
            }
          }
          printResult.complete(true);
        } catch (IOException | InterruptedException e) {
          printResult.completeExceptionally(e);
        }
      }
      return printResult;
    } else {

      try {
        final POSPrinter printer = new POSPrinter(job.getTargetPrinterIp());
        var processedJob = PrintJobHandler.processDataBeforeSend(job.getJobContent(), job.getTargetPrinterIp());

        Log.i("printToPrinter", "Executing printToPrinter");
        printer.printData(processedJob, success -> {
          if (success) {
            Log.d("printToPrinter", "print successful");
            printer.disconnect();
            printResult.complete(true);
          } else {
            Log.d("printToPrinter", "print un-successful");
            printer.disconnect();
            printResult.complete(false);
          }
        });
      } catch (Exception e) {
        Log.e("printToPrinter", "Error setting up print job", e);
        printResult.complete(false);
      }

      return printResult;
    }
  }

  /**
   * Separates print items into groups: before image, image, and after image
   *
   * @param printItems List of print items to process
   * @return List of separated print item groups
   */
  private List<List<PrintItem>> separatePrintItemsAroundImages(List<PrintItem> printItems) {
    List<List<PrintItem>> result = new ArrayList<>();
    List<PrintItem> currentGroup = new ArrayList<>();

    for (int i = 0; i < printItems.size(); i++) {
      PrintItem item = printItems.get(i);

      if (item.getType() == PrintItem.Type.IMAGE) {
        // If we have items before the image, add them as a group
        if (!currentGroup.isEmpty()) {
          result.add(new ArrayList<>(currentGroup));
          currentGroup = new ArrayList<>(); // Create new list instead of clear()
        }
        // Add the image as its own group
        result.add(Collections.singletonList(item));
      } else {
        currentGroup.add(item);

        // Only add the current group if this is the last item
        if (i == printItems.size() - 1) {
          result.add(currentGroup);
        }
      }
    }

    return result;
  }

  private void safeDisconnect(IMyBinder binder) {
    try {
      binder.DisconnectCurrentPort(new TaskCallback() {
        @Override
        public void OnSucceed() {
          Log.d("safeDisconnect", "Disconnected successfully");
        }

        @Override
        public void OnFailed() {
          Log.e("safeDisconnect", "Disconnect failed");
        }
      });
    } catch (Exception e) {
      Log.e("safeDisconnect", "Error during disconnect", e);
    }
  }


  /**
   * Retrieves all pending print jobs.
   *
   * @return A WritableArray containing information about pending print jobs
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public WritableArray getPendingJobs() {
//    Don't have this method at the moment ***, Will return an empty array
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
  public Boolean deletePendingJobs(String jobId) {
//    Don't have this method at the moment ***
//    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
//    return queueProcessor.deleteJobById(jobId,binder);
    return true;
  }


  /**
   * Retrieves the status of all printers in the printer pool.
   *
   * @return A list of PrinterStatus objects representing the status of each printer
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public List<PrinterStatus> getPrinterPoolStatus() throws ExecutionException, InterruptedException {
    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    return statusManager.getPrinterPoolStatus(binder, printerPool).get();
  }


  @RequiresApi(api = Build.VERSION_CODES.N)
  private CompletableFuture<Boolean> addNewPrinter(String printerIp) {
    CompletableFuture<Boolean> additionResult = new CompletableFuture<>();

    Log.i("addNewPrinter", "Attempting to add printer: " + printerIp);
    
    // First check if printer is reachable
    if (!PrinterUtils.isPrinterReachable(printerIp)) {
      Log.w("addNewPrinter", "Printer not reachable: " + printerIp);
      additionResult.complete(false);
      return additionResult;
    }

    IMyBinder binder = PosThermalPrinterModule.Companion.getBinder();
    if (binder != null) {
      Log.i("addNewPrinter", "Binder available, connecting to: " + printerIp);
      PrinterUtils.addPrinter(binder, printerIp, new TaskCallback() {
        @Override
        public void OnSucceed() {
          Log.i("addNewPrinter", "Successfully connected to printer: " + printerIp);
          safeDisconnect(binder);
          additionResult.complete(true);
        }

        @Override
        public void OnFailed() {
          Log.e("addNewPrinter", "Failed to connect to printer: " + printerIp);
          additionResult.complete(false);
          safeDisconnect(binder);
        }
      });
    } else {
      Log.e("addNewPrinter", "Binder is null, cannot connect to printer: " + printerIp);
      additionResult.complete(false);
    }

    return additionResult;
  }

  @RequiresApi(api = Build.VERSION_CODES.N)
  public CompletableFuture<Boolean> checkPrinterStatus(String printerIp) {
    //    Don't have this method at the moment ***
    CompletableFuture<Boolean> additionResult = new CompletableFuture<>();
    additionResult.complete(true);
    return additionResult;
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
