package com.posthermalprinter.util;

import java.util.List;

/**
 * Represents a print job to be sent to a printer.
 * This class encapsulates all the information needed for a print job,
 * including the content to be printed, target printer details, and job status.
 */
public class PrinterJob {
  private final List<PrintItem> jobContent;
  private String targetPrinterIp;
  private String printerName;
  private boolean isPending;
  private final String metadata;
  private final String jobId;

  /**
   * Constructs a new PrinterJob.
   *
   * @param jobContent      The list of PrintItems to be printed.
   * @param targetPrinterIp The IP address of the target printer.
   * @param printerName     The name of the target printer.
   * @param metadata        Additional metadata for the print job.
   * @param jobId           A unique identifier for this print job.
   */
  public PrinterJob(List<PrintItem> jobContent, String targetPrinterIp, String printerName, String metadata, String jobId) {
    this.jobContent = jobContent;
    this.targetPrinterIp = targetPrinterIp;
    this.printerName = printerName;
    this.isPending = false;
    this.metadata = metadata;
    this.jobId = jobId;
  }

  /**
   * Gets the unique identifier of this print job.
   *
   * @return The job ID.
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * Gets the content of the print job.
   *
   * @return A list of PrintItems representing the job content.
   */
  public List<PrintItem> getJobContent() {
    return jobContent;
  }

  /**
   * Gets the metadata associated with this print job.
   *
   * @return The metadata string.
   */
  public String getMetadata() {
    return metadata;
  }

  /**
   * Checks if the print job is currently pending.
   *
   * @return true if the job is pending, false otherwise.
   */
  public boolean getIsPending() {
    return isPending;
  }

  /**
   * Gets the name of the target printer.
   *
   * @return The printer name.
   */
  public String getPrinterName() {
    return printerName;
  }

  /**
   * Gets the IP address of the target printer.
   *
   * @return The printer's IP address.
   */
  public String getTargetPrinterIp() {
    return targetPrinterIp;
  }

  /**
   * Sets a new target printer for this job.
   *
   * @param name            The name of the new target printer.
   * @param targetPrinterIp The IP address of the new target printer.
   */
  public void setNewTargetPrinterIp(String name, String targetPrinterIp) {
    this.printerName = name;
    this.targetPrinterIp = targetPrinterIp;
  }

  /**
   * Marks this print job as pending.
   */
  public void setPending() {
    this.isPending = true;
  }

}
