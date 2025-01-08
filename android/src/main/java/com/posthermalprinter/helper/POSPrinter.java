package com.posthermalprinter.helper;


import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class POSPrinter {
  private static final int DEFAULT_PORT = 9100;
  private static final int CONNECT_TIMEOUT = 5000;

  private String ipAddress;
  private int port;
  private Socket socket;
  private OutputStream outputStream;
  private boolean isConnected = false;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  public POSPrinter(String ipAddress, int port) {
    this.ipAddress = ipAddress;
    this.port = port;
  }

  public POSPrinter(String ipAddress) {
    this(ipAddress, DEFAULT_PORT);
  }

  private boolean connect() {
    try {
      if (isConnected && socket != null && !socket.isClosed()) {
        return true;
      }

      socket = new Socket();
      // Set larger buffer sizes
      socket.setReceiveBufferSize(64 * 1024);
      socket.setSendBufferSize(64 * 1024);
      // Disable Nagle's algorithm for faster transmission
      socket.setTcpNoDelay(true);
      socket.connect(new InetSocketAddress(ipAddress, port), CONNECT_TIMEOUT);
      outputStream = socket.getOutputStream();
      isConnected = true;
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      isConnected = false;
      return false;
    }
  }

  private boolean printMultiple(List<byte[]> dataList) {
    if (!isConnected || socket == null || socket.isClosed()) {
      return false;
    }

    try {
      // Combine all data into a single byte array
      ByteArrayOutputStream combinedData = new ByteArrayOutputStream();
      for (byte[] data : dataList) {
        combinedData.write(data);
      }

      // Send all data at once
      outputStream.write(combinedData.toByteArray());
      outputStream.flush();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      isConnected = false;
      return false;
    }
  }

  public void printData(List<byte[]> dataList, PrinterCallback callback) {
    executor.execute(() -> {
      boolean success = connect();
      if (!success) {
        notifyResult(callback, false);
        return;
      }

      success = printMultiple(dataList);
      notifyResult(callback, success);
    });
  }

  public void disconnect() {
    try {
      if (outputStream != null) {
        outputStream.close();
      }
      if (socket != null) {
        socket.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      outputStream = null;
      socket = null;
      isConnected = false;
    }
  }

  private void notifyResult(PrinterCallback callback, boolean success) {
    if (callback != null) {
      mainHandler.post(() -> callback.onPrintResult(success));
    }
  }

  public void destroy() {
    disconnect();
    executor.shutdown();
  }

  public boolean isConnected() {
    return isConnected && socket != null && !socket.isClosed();
  }

  public interface PrinterCallback {
    void onPrintResult(boolean success);
  }
}



