package com.posthermalprinter.helper;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import com.posthermalprinter.PrinterManager;
import com.posthermalprinter.util.PrinterJob;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.utils.PosPrinterDev;

import org.json.JSONException;
import org.json.JSONObject;


@RequiresApi(api = Build.VERSION_CODES.N)
public class PrintQueueProcessor {
  private static final String TAG = "PrintQueueProcessor";
  private final BlockingQueue<PrinterJob> printQueue;
  private final List<String> printerPool;
  private final PrinterManager printerManager;
  private final PrinterEventManager eventManager;
  private  ExecutorService printExecutor;
  private volatile boolean isRunning = true;




  public PrintQueueProcessor(BlockingQueue<PrinterJob> printQueue, List<String> printerPool, PrinterManager printerManager, PrinterEventManager eventManager) {
    this.printQueue = printQueue;
    this.printerPool = printerPool;
    this.printerManager = printerManager;
    this.eventManager = eventManager;
    this.printExecutor = Executors.newSingleThreadExecutor();
    startQueueProcessor();
  }


  private void startQueueProcessor() {
    printExecutor.execute(new Runnable() {
      @Override
      public void run() {
        while (isRunning) {
          try {
            // Take will block until a job is available
            PrinterJob job = printQueue.take();
            if (job != null) {
              try {
                processJob(job);
              } catch (JSONException e) {
                Log.e(TAG, "Error processing job: " + e.getMessage());
              }
            }
          } catch (InterruptedException e) {
            Log.e(TAG, "Queue processor interrupted: " + e.getMessage());
            if (!isRunning) {
              break;
            }
          } catch (Exception e) {
            Log.e(TAG, "Unexpected error in queue processor: " + e.getMessage());
            // Add a small delay before continuing
            try {
              Thread.sleep(1000);
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
            }
          }
        }
      }
    });
  }


  /**
   * Processes the print queue by submitting print jobs to the print executor.
   *
   * @param binder An {@link IMyBinder} object used to retrieve printer information.
   *
   * @implNote This method:
   *           1. Checks if the binder is valid.
   *           2. Retrieves the list of available printers.
   *           3. Submits a task to the print executor that:
   *              a. Polls jobs from the print queue.
   *              b. Processes each job using the processJob method.
   *              c. Continues until the queue is empty.
   *
   * @throws NullPointerException if the binder is null.
   *
   * @see PrinterJob
   * @see PosPrinterDev.PrinterInfo
   */
  public void processPrintQueue(IMyBinder binder) {
    if (binder == null) {
      Log.e(TAG, "Binder is null, cannot process print queue");
      return;
    }

    // Instead of submitting new tasks, just ensure the queue processor is running
    if (!isRunning) {
      isRunning = true;
      startQueueProcessor();
    }
  }


  /**
   * Processes a single print job.
   *
   * @param job The {@link PrinterJob} to be processed.
   *
   * @implNote This method:
   *           1. Checks if the target printer is in the printer pool.
   *           2. Verifies if the printer exists in the info list.
   *           3. Attempts to print the job.
   *           4. Handles success and failure scenarios, including retrying failed jobs.
   *           5. Manages printer unreachable status and events.
   *           6. Re-queues unprocessed jobs with a delay to avoid tight loops.
   *
   * @see PrinterJob
   * @see PosPrinterDev.PrinterInfo
   */

  private void processJob(PrinterJob job) throws JSONException {
    for (String manager : printerPool) {
      if (manager.equals(job.getTargetPrinterIp())) {
        // Use thenAccept instead of blocking get()
        printerManager.printToPrinter(job)
          .thenAccept(printSuccess -> {
            if (printSuccess) {
              Log.i(TAG, "Printed job '" + job.getJobContent() + "' on printer " + manager);
            } else {
              eventManager.sendPrinterUnreachableEvent(job.getTargetPrinterIp());
              Log.e(TAG, "Print failed for job '" + job.getJobContent() + "'.");
            }

            // Handle the delay after print completion
            try {
              JSONObject jsonObject = new JSONObject(job.getMetadata());
              long timeout = timeoutForType(jsonObject.getString("type"));
              Thread.sleep(timeout);
            } catch (InterruptedException e) {
              Log.e(TAG, "Sleep interrupted", e);
            } catch (JSONException e) {
              Log.e(TAG, "JSON parsing error", e);
            }
          })
          .exceptionally(throwable -> {
            Log.e(TAG, "Error processing print job: " + throwable.getMessage());
            return null;
          });
        return; // Exit the loop after starting the print job
      }
    }



    // Add a small delay according to the print type
    try {
      JSONObject jsonObject = new JSONObject(job.getMetadata());
      Log.i("printToPrinter" , String.valueOf(jsonObject.getString("type").equals("Receipt")));

      long timeout = timeoutForType(jsonObject.getString("type"));

      Thread.sleep(timeout);
    } catch (InterruptedException e) {
      Log.e(TAG, "Sleep interrupted", e);
    }

  }

  private long timeoutForType(String type) {
    if (type == null) {
      return 500;  // default case for null
    }

    return switch (type) {
      case "KOT" -> 300;
      case "RECEIPT", "BILL" -> 1000;
      case "TEST_CONNECTION", "CASH_IN_OUT", "OPEN_DRAWER" -> 100;
      case "SHIFT_OPEN_SUMMARY", "ITEM_SALES_REPORT" -> 400;
      case "SHIFT_CLOSE_SUMMARY" -> 800;
      default -> 500;
    };
  }

  /**
   * Checks if a printer with the given IP address exists in the provided printer info list.
   *
   * @param infoList A list of {@link PosPrinterDev.PrinterInfo} objects to search through.
   * @param targetPrinterIp The IP address of the printer to find.
   * @return true if a printer with the given IP is found in the list, false otherwise.
   *
   * @see PosPrinterDev.PrinterInfo
   */
  private boolean isPrinterInInfoList(ArrayList<PosPrinterDev.PrinterInfo> infoList, String targetPrinterIp) {
    for (PosPrinterDev.PrinterInfo info : infoList) {
      if (info.portInfo.equals(targetPrinterIp)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Changes the target printer for pending print jobs from an old printer IP to a new printer IP.
   * This method modifies the print queue by updating the printer information for jobs
   * that were originally targeted to the old printer IP.
   *
   * @param oldPrinterIp The IP address of the old printer to be replaced.
   * @param newPrinterIp The IP address of the new printer to be used.
   * @param binder An {@link IMyBinder} object used to retrieve printer information.
   *               If null, the method will log an error and exit.
   *
   *
   * @apiNote This method requires API level 24 (Android 7.0) or higher.
   *
   * @implNote The method performs the following steps:
   *           1. Retrieves the list of available printers.
   *           2. Finds the new printer information based on the provided IP.
   *           3. Drains the original print queue to a temporary queue.
   *           4. Updates the printer information for relevant jobs.
   *           5. Adds all jobs back to the original queue.
   *           6. Triggers processing of the updated queue.
   *
   * @see PrinterJob
   * @see PosPrinterDev.PrinterInfo
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public void changePendingPrintJobsPrinter(String oldPrinterIp, String newPrinterIp, IMyBinder binder) {
    ArrayList<PosPrinterDev.PrinterInfo> printerList = binder != null ? binder.GetPrinterInfoList() : null;

    // Create a temporary queue to hold modified jobs
    BlockingQueue<PrinterJob> tempQueue = new LinkedBlockingQueue<>();

    PosPrinterDev.PrinterInfo printerInfo = null;

    // Check if the list is not null and contains printers
    if (printerList != null && !printerList.isEmpty()) {
      // Find the new printer info
      for (PosPrinterDev.PrinterInfo info : printerList) {
        if (info.portInfo.equals(newPrinterIp)) {
          printerInfo = info;
          break;
        }
      }

      // Drain the original queue
      printQueue.drainTo(tempQueue);

      // Iterate through the temporary queue and modify IP if necessary
      for (PrinterJob job : tempQueue) {
        if (job.getTargetPrinterIp().equals(oldPrinterIp) && printerInfo != null) {
          job.setNewTargetPrinterIp(printerInfo.printerName, printerInfo.portInfo);
          job.removePending();
        }
        // Add the job back to the original queue
        printQueue.add(job);
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Log.e(TAG, "Sleep interrupted", e);
      }
      // Trigger processing of the updated queue
      processPrintQueue(binder);
    } else {
      Log.e(TAG, "Printer list is null or empty");
    }
  }

  /**
   * Changes the target printer for a specific pending print job.
   *
   * This method modifies the print queue by updating the printer information for a job
   * that matches the provided job ID. It assigns the job to a new printer specified by the IP address.
   *
   * @param jobId The unique identifier of the job to be modified.
   * @param printerIp The IP address of the new printer to be used.
   * @param binder An {@link IMyBinder} object used to retrieve printer information.
   *               If null, the method will log an error and exit.
   *
   *
   * @apiNote This method requires API level 24 (Android 7.0) or higher.
   *
   * @implNote The method performs the following steps:
   *           1. Retrieves the list of available printers.
   *           2. Finds the new printer information based on the provided IP.
   *           3. Drains the original print queue to a temporary queue.
   *           4. Updates the printer information for the matching job.
   *           5. Adds all jobs back to the original queue.
   *           6. Triggers processing of the updated queue.
   *
   * @see PrinterJob
   * @see PosPrinterDev.PrinterInfo
   */
  @RequiresApi(api = Build.VERSION_CODES.N)
  public void changePendingPrintJobToPrinter(String jobId, String printerIp, IMyBinder binder) {
    ArrayList<PosPrinterDev.PrinterInfo> printerList = binder != null ? binder.GetPrinterInfoList() : null;

    // Create a temporary queue to hold modified jobs
    BlockingQueue<PrinterJob> tempQueue = new LinkedBlockingQueue<>();

    PosPrinterDev.PrinterInfo printerInfo = null;

    // Check if the list is not null and contains printers
    if (printerList != null && !printerList.isEmpty()) {
      // Find the new printer info
      for (PosPrinterDev.PrinterInfo info : printerList) {
        if (info.portInfo.equals(printerIp)) {
          printerInfo = info;
          break;
        }
      }

      // Drain the original queue
      printQueue.drainTo(tempQueue);

      // Iterate through the temporary queue and modify IP if necessary
      for (PrinterJob job : tempQueue) {
        if (job.getJobId().equals(jobId) && printerInfo != null) {
          job.setNewTargetPrinterIp(printerInfo.printerName, printerInfo.portInfo);
          job.removePending();
        }
        // Add the job back to the original queue
        printQueue.add(job);
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Log.e(TAG, "Sleep interrupted", e);
      }

      // Trigger processing of the updated queue
      processPrintQueue(binder);
    } else {
      Log.e(TAG, "Printer list is null or empty");
    }
  }


  /**
   * Retrieves pending print jobs and formats them for use in JavaScript.
   * This method extracts information from pending print jobs in the queue
   * and creates a WritableArray containing job details.
   *
   * @param binder An {@link IMyBinder} object used for processing the print queue.
   * @return A {@link WritableArray} containing information about pending print jobs,
   *         formatted for use in JavaScript.
   *
   *
   * @implNote The method performs the following steps:
   *           1. Drains the print queue to a temporary list.
   *           2. Iterates through the jobs, creating a WritableMap for each pending job.
   *           3. Adds job information (printer IP, name, metadata, and job ID) to each map.
   *           4. Adds all jobs back to the original queue.
   *           5. Triggers processing of the updated queue.
   *
   * @see PrinterJob
   * @see WritableArray
   * @see WritableMap
   */
  public WritableArray getPendingJobsForJS(IMyBinder binder) {
    WritableArray pendingJobsArray = Arguments.createArray();
    List<PrinterJob> tempList = new ArrayList<>();

    // Drain the queue to a temporary list
    printQueue.drainTo(tempList);

    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Log.e(TAG, "Sleep interrupted", e);
    }


    for (PrinterJob job : tempList) {
      if (job.getIsPending()) {
        WritableMap jobMap = Arguments.createMap();
        jobMap.putString("printerIp", job.getTargetPrinterIp());
        jobMap.putString("printerName", job.getPrinterName());
        jobMap.putString("metadata", job.getMetadata());
        jobMap.putString("jobId", job.getJobId());
        pendingJobsArray.pushMap(jobMap);
      }

      // Add the job back to the queue
      printQueue.offer(job);
    }


    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Log.e(TAG, "Sleep interrupted", e);
    }



    // Trigger processing of the updated queue
    processPrintQueue(binder);

    return pendingJobsArray;
  }


  /**
   * Deletes a print job from the queue based on its job ID.
   *
   * @param jobId The unique identifier of the job to be deleted.
   * @param binder An {@link IMyBinder} object used for processing the print queue.
   * @return true if the job was found and deleted, false otherwise.
   *
   *
   * @implNote The method performs the following steps:
   *           1. Drains the print queue to a temporary list.
   *           2. Iterates through the jobs, searching for the specified job ID.
   *           3. If found, removes the job from the list.
   *           4. Adds all remaining jobs back to the original queue.
   *           5. Triggers processing of the updated queue.
   *
   * @see PrinterJob
   */
  public boolean deleteJobById(String jobId, IMyBinder binder) {
    boolean jobFound = false;
    List<PrinterJob> tempList = new ArrayList<>();

    // Drain the queue to a temporary list
    printQueue.drainTo(tempList);

    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Log.e(TAG, "Sleep interrupted", e);
    }

    // Use an iterator to safely remove the job if found
    Iterator<PrinterJob> iterator = tempList.iterator();
    while (iterator.hasNext()) {
      PrinterJob job = iterator.next();
      if (job.getJobId().equals(jobId)) {
        iterator.remove();
        jobFound = true;
        Log.i(TAG, "Job with ID " + jobId + " has been removed from the queue.");
        break;
      }
    }

    // Add all remaining jobs back to the queue
    printQueue.addAll(tempList);

    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Log.e(TAG, "Sleep interrupted", e);
    }


    // Trigger processing of the updated queue
    processPrintQueue(binder);

    if (!jobFound) {
      Log.w(TAG, "Job with ID " + jobId + " was not found in the queue.");
    }

    return jobFound;
  }

  /**
   * Initiates a shutdown of the print executor service.
   * This method should be called when the print service is no longer needed
   * to ensure proper cleanup of resources.
   *
   * @implNote This method calls shutdown() on the printExecutor,
   *           which will allow previously submitted tasks to execute
   *           before terminating, but will not accept new tasks.
   *
   * @see ExecutorService#shutdown()
   */
  public void shutdown() {
    isRunning = false;
    printExecutor.shutdown();
    try {
      if (!printExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        printExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      printExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
