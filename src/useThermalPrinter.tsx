import { useState, useEffect } from 'react';
import {
  reconnectPrinter,
  printText,
  printPendingJobsWithNewPrinter,
  addPrinterToPool,
  removePrinterFromPool,
  printImage,
  getPrinterPoolStatus,
  deletePendingJob,
  getPendingJobs,
  initializePrinterPool,
  retryPendingJobFromNewPrinter,
  openCashBox,
} from './printerModule';
import { PRINTER_WIDTH, type PrinterStatus, type IPPrinter } from './types';

/**
 * Custom hook to interact with ESC/POS printers.
 *
 * @returns {Object} - Returns various functions and state for printer management.
 * @property {number} PRINTER_WIDTH - The width of the printer.
 * @property {boolean} isInitialized - Printer pool initialization status.
 * @property {function} reconnectPrinter - Reconnects a printer.
 * @property {function} printText - Prints text to the printer.
 * @property {function} printPendingJobsWithNewPrinter - Prints pending jobs with a new printer.
 * @property {function} addPrinterToPool - Adds a printer to the pool.
 * @property {function} removePrinterFromPool - Removes a printer from the pool.
 * @property {function} printImage - Prints an image.
 * @property {function} getPrinterPoolStatus - Gets the status of the printer pool.
 * @property {function} getBluetoothDevices - Retrieves available Bluetooth devices.
 * @property {function} initializePrinterPool - Initializes the printer pool.
 * @property {function} getPendingJobs - Gets the pending jobs.
 * @property {function} deletePendingJob - Deletes a pending job.
 * @property {function} retryPendingJobFromNewPrinter - Retries a pending job with a new printer.
 * @property {function} openCashBox - Open cash drawer connected to a specific printer.

 */
export const useThermalPrinter = () => {
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initialize = async () => {
      const result = await initializePrinterPool();
      setIsInitialized(result);
    };
    initialize();
  }, []);

  return {
    PRINTER_WIDTH,
    isInitialized,
    reconnectPrinter,
    printText,
    printPendingJobsWithNewPrinter,
    addPrinterToPool,
    removePrinterFromPool,
    printImage,
    getPrinterPoolStatus,
    deletePendingJob,
    initializePrinterPool,
    getPendingJobs,
    retryPendingJobFromNewPrinter,
    openCashBox,
  };
};

export type { PrinterStatus, IPPrinter };
