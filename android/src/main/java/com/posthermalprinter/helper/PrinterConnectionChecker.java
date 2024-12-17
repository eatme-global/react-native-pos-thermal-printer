package com.posthermalprinter.helper;
import android.annotation.SuppressLint;
import android.os.IBinder;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.TaskCallback;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
  public CompletableFuture<List<PrinterConnectionResult>> checkConnections() {
    if (binder == null) {
      return CompletableFuture.completedFuture(new ArrayList<>());
    }

    return checkNextPrinter(0);
  }

  @SuppressLint("NewApi")
  private CompletableFuture<List<PrinterConnectionResult>> checkNextPrinter(int index) {
    if (index >= printerIps.size()) {
      return CompletableFuture.completedFuture(results);
    }

    String currentPrinterIp = printerIps.get(index);
    CompletableFuture<Boolean> connectionTest = new CompletableFuture<>();

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

    return connectionTest
      .thenCompose(result -> checkNextPrinter(index + 1));
  }
}
