package com.posthermalprinter.helper;

import android.annotation.SuppressLint;

import com.posthermalprinter.PosThermalPrinterModule;
import com.posthermalprinter.imin.IminPrinterModule;

// Placeholder comment to give a releases

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.TaskCallback;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PrinterConnectionChecker {
  private final List<String> printerIps;
  private final IMyBinder binder;
  private final List<PrinterConnectionResult> results;

  public static class PrinterConnectionResult {
    public final String printerIp;
    public final boolean isConnectable;

    public PrinterConnectionResult(String printerIp, boolean isConnectable) {
      this.printerIp = printerIp;
      this.isConnectable = isConnectable;
    }
  }

  public PrinterConnectionChecker(IMyBinder binder, List<String> printerIps) {
    this.binder = binder;
    this.printerIps = printerIps;
    this.results = new ArrayList<>();
  }

  @SuppressLint("NewApi")
  public CompletableFuture<List<PrinterConnectionResult>> checkConnections() throws ExecutionException, InterruptedException {
    if (binder == null) {
      return CompletableFuture.completedFuture(new ArrayList<>());
    }

    return checkNextPrinter(0);
  }

  @SuppressLint("NewApi")
  private CompletableFuture<List<PrinterConnectionResult>> checkNextPrinter(int index) throws ExecutionException, InterruptedException {
    if (index >= printerIps.size()) {
      return CompletableFuture.completedFuture(results);
    }

    IminPrinterModule iMinPrinterModule = PosThermalPrinterModule.Companion.getIMinPrinterModule();

    String currentPrinterIp = printerIps.get(index);
    CompletableFuture<Boolean> connectionTest = new CompletableFuture<>();

    if (!Objects.equals(currentPrinterIp, "INTERNAL")) {
      PrinterUtils.addPrinter(binder, currentPrinterIp, new TaskCallback() {
        @Override
        public void OnSucceed() {
          // Connection successful, now disconnect
          binder.DisconnectCurrentPort(new TaskCallback() {
            @Override
            public void OnSucceed() {
              results.add(new PrinterConnectionResult(currentPrinterIp, true));
              connectionTest.complete(true);
            }

            @Override
            public void OnFailed() {
              // Even if disconnect fails, we know connection was possible
              results.add(new PrinterConnectionResult(currentPrinterIp, true));
              connectionTest.complete(true);
            }
          });
        }

        @Override
        public void OnFailed() {
          results.add(new PrinterConnectionResult(currentPrinterIp, false));
          connectionTest.complete(false);
        }
      });
    } else {
      if (iMinPrinterModule != null) {
        Boolean result = iMinPrinterModule.initPrinter();
        results.add(new PrinterConnectionResult(currentPrinterIp, result));
        connectionTest.complete(result);
      } else {
        connectionTest.complete(false);
      }
    }

    return connectionTest
      .thenCompose(result -> {
        try {
          return checkNextPrinter(index + 1);
        } catch (ExecutionException | InterruptedException e) {
          throw new RuntimeException(e);
        }
      });
  }
}
