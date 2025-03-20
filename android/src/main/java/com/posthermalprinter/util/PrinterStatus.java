package com.posthermalprinter.util;

/**
 * Represents the status of a printer in the printer pool.
 * This class encapsulates information about a printer's IP address,
 * reachability, and name.
 */
public class PrinterStatus {

  private final String printerIp;
  private final boolean isReachable;
  private final String printerName;

  /**
   * Constructs a new PrinterStatus object.
   *
   * @param printerIp   The IP address of the printer.
   * @param isReachable Whether the printer is currently reachable.
   * @param printerName The name of the printer.
   */
  public PrinterStatus(
    String printerIp,
    boolean isReachable,
    String printerName
  ) {
    this.printerIp = printerIp;
    this.isReachable = isReachable;
    this.printerName = printerName;
  }

  /**
   * Gets the IP address of the printer.
   *
   * @return The IP address of the printer.
   */
  public String getPrinterIp() {
    return printerIp;
  }

  /**
   * Checks if the printer is currently reachable.
   *
   * @return true if the printer is reachable, false otherwise.
   */
  public boolean isReachable() {
    return isReachable;
  }

  /**
   * Gets the name of the printer.
   *
   * @return The name of the printer.
   */
  public String getPrinterName() {
    return printerName;
  }
}
