import { NativeModules } from 'react-native';
import { LINKING_ERROR } from './constants';
import {
  PrintJobRowType,
  type IPPrinter,
  type ParsedPendingJob,
  type PrinterStatus,
  type PrintJobMetadata,
  type PrintJobRow,
  type RawPendingJob,
} from './types';

const EscPosPrinter = NativeModules.PosThermalPrinter
  ? NativeModules.PosThermalPrinter
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**
 * Attempts to reconnect to a printer by IP address.
 *
 * @param {string} ip - The IP address of the printer to reconnect.
 * @returns {Promise<boolean>} - A promise that resolves to a boolean indicating whether the reconnection was successful.
 */
export async function reconnectPrinter(ip: string): Promise<boolean> {
  try {
    const result = await EscPosPrinter.retryPrinterConnection(ip);
    return result;
  } catch (error) {
    console.error('Error retrying printer connection: ', error);
    return false;
  }
}

/**
 * Sends a print job containing text, columns, and images to a printer by IP address.
 *
 * @param {string} ip - The IP address of the printer.
 * @param {PrintJobRow[]} payload - The PrintJob payload (text, columns, images, etc.) to be printed.
 * @param {object} metadata - User-defined metadata for the print job.
 * @returns {Promise<void>} - A promise that resolves when the print job is successfully sent.
 */
export async function printText(
  ip: string,
  payload: PrintJobRow[],
  metadata: PrintJobMetadata
): Promise<void> {
  try {
    await EscPosPrinter.setPrintJobs(ip, payload, JSON.stringify(metadata));
  } catch (error) {
    console.error('Error printing text:', error);
  }
}

/**
 * Sends a cash drawer open print job to a printer by IP address.
 *
 * @param {string} ip - The IP address of the printer.
 * @returns {Promise<void>} - A promise that resolves when the print job is successfully sent.
 */
export async function openCashBox(ip: string): Promise<void> {
  try {
    const metadata: PrintJobMetadata = {
      type: 'Open Cashbox',
    };
    await EscPosPrinter.setPrintJobs(
      ip,
      [{ type: PrintJobRowType.CASHBOX }],
      JSON.stringify(metadata)
    );
  } catch (error) {
    console.error('Error opening cashbox:', error);
  }
}

/**
 * Prints pending jobs from an old printer to a new printer.
 *
 * @param {string} oldPrinterIp - The IP address of the old printer.
 * @param {string} newPrinterIp - The IP address of the new printer.
 * @returns {Promise<any>} - A promise that resolves when the jobs are successfully printed on the new printer.
 */
export async function printPendingJobsWithNewPrinter(
  oldPrinterIp: string,
  newPrinterIp: string
): Promise<any> {
  try {
    return await EscPosPrinter.printFromNewPrinter(oldPrinterIp, newPrinterIp);
  } catch (error) {
    console.error('Error printing pending jobs with new printer:', error);
    return false;
  }
}

/**
 * Adds a printer to the printer pool.
 *
 * @param {IPPrinter} printer - The printer object containing its IP and other details.
 * @returns {Promise<any>} - A promise that resolves when the printer is added to the pool.
 */
export async function addPrinterToPool(printer: IPPrinter): Promise<any> {
  try {
    const result = await EscPosPrinter.addPrinterToPool(printer);
    return result;
  } catch (error) {
    console.error('Error adding printer to pool:', error);
    return false;
  }
}

/**
 * Removes a printer from the printer pool by IP address.
 *
 * @param {string} ip - The IP address of the printer to remove from the pool.
 * @returns {Promise<any>} - A promise that resolves when the printer is removed.
 */
export async function removePrinterFromPool(ip: string): Promise<any> {
  return await EscPosPrinter.removePrinterFromPool(ip);
}

/**
 * Prints an image using a base64 encoded string.
 *
 * @param {string} base64Image - The base64 encoded image string.
 * @returns {Promise<void>} - A promise that resolves when the image is printed.
 */
export async function printImage(base64Image: string): Promise<void> {
  try {
    await EscPosPrinter.printImage(base64Image);
  } catch (error) {
    console.error('Error printing image:', error);
  }
}

/**
 * Retrieves the current status of the printer pool.
 *
 * @returns {Promise<PrinterStatus[]>} - A promise that resolves to an array of printer status objects.
 */
export async function getPrinterPoolStatus(): Promise<PrinterStatus[]> {
  try {
    return await EscPosPrinter.getPrinterPoolStatus();
  } catch (error) {
    console.error('Error fetching printer pool status:', error);
    return [];
  }
}

/**
 * Retrieves and parses pending print jobs.
 *
 * This function fetches pending jobs from the native module,
 * then parses the metadata of each job from a JSON string to an object.
 *
 * @returns {Promise<ParsedPendingJob[]>} A promise that resolves to an array of parsed pending jobs.
 * If no pending jobs are found, it returns an empty array.
 *
 * @throws Will throw an error if the native module call fails or if JSON parsing fails.
 */
export async function getPendingJobs(): Promise<ParsedPendingJob[]> {
  try {
    const rawPendingJobs: RawPendingJob[] =
      await EscPosPrinter.getPendingJobDetails();

    if (rawPendingJobs && rawPendingJobs.length > 0) {
      return rawPendingJobs.map((job) => ({
        ...job,
        metadata: JSON.parse(job.metadata),
      }));
    }
  } catch (error) {
    console.error('Error initializing printer pool:', error);
    return [];
  }

  return [];
}

/**
 * Deletes a pending print job by its ID.
 *
 * This function calls the native module to delete a specific pending job.
 *
 * @param {string} jobId - The unique identifier of the job to be deleted.
 * @returns {Promise<boolean>} A promise that resolves to:
 *   - true if the job was successfully deleted
 *   - false if the job wasn't found or couldn't be deleted
 *
 * @throws Will throw an error if the native module call fails.
 */
export async function deletePendingJob(jobId: string): Promise<boolean> {
  try {
    return await EscPosPrinter.deletePendingJobs(jobId);
  } catch (error) {
    console.error('Error deleting pending job:', error);
    return false;
  }
}

/**
 * Retries a pending print job using a new printer.
 *
 * This function attempts to reassign a pending print job to a new printer.
 * It communicates with the native module to change the target printer for the specified job.
 *
 * @async
 * @function retryPendingJobFromNewPrinter
 * @param {string} jobId - The unique identifier of the pending print job to retry.
 * @param {string} printerIp - The IP address of the new printer to use for the retry attempt.
 * @returns {Promise<boolean>} A promise that resolves to true if the retry was successful, false otherwise.
 *
 * @throws {Error} If there's an error during the retry process, it will be caught and logged, and the function will return false.
 */
export async function retryPendingJobFromNewPrinter(
  jobId: string,
  printerIp: string
): Promise<boolean> {
  try {
    return await EscPosPrinter.retryPendingJobFromNewPrinter(jobId, printerIp);
  } catch (error) {
    console.error('Error retrying pending job from new printer:', error);
    return false;
  }
}

/**
 * Initializes the printer pool.
 *
 * @returns {Promise<boolean>} - A promise that resolves to a boolean indicating whether the initialization was successful.
 */
export async function initializePrinterPool(): Promise<boolean> {
  try {
    return await EscPosPrinter.initializePrinterPool();
  } catch (error) {
    console.error('Error initializing printer pool:', error);
    return false;
  }
}

// New Implementation

export async function getPendingPrinterJobs(
  printerIp: string
): Promise<ParsedPendingJob[]> {
  try {
    const rawPendingJobs: RawPendingJob[] =
      await EscPosPrinter.getPrinterPendingJobDetails(printerIp);

    if (rawPendingJobs && rawPendingJobs.length > 0) {
      return rawPendingJobs.map((job) => ({
        ...job,
        metadata: JSON.parse(job.metadata),
      }));
    }
  } catch (error) {
    console.error('Error initializing printer pool:', error);
    return [];
  }

  return [];
}

export async function deletePrinterPendingJobs(
  printerIp: string
): Promise<boolean> {
  try {
    return await EscPosPrinter.dismissPendingJobs(printerIp);
  } catch (error) {
    console.error('Error deleting pending job:', error);
    return false;
  }
}

export async function retryPendingJobsFromPrinter(
  printerIp: string
): Promise<boolean> {
  try {
    return await EscPosPrinter.retryPendingJobsFromPrinter(printerIp);
  } catch (error) {
    console.error('Error deleting pending job:', error);
    return false;
  }
}
