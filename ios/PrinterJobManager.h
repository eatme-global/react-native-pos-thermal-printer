#import <Foundation/Foundation.h>
#import "PrinterJob.h"
#import "PrinterConnectionManager.h"

@class PosThermalPrinter;

@interface PrinterJobManager : NSObject


/**
 * @brief Initializes a new PrinterJobManager instance.
 * @param secondLibrary The SecondLibrary instance to use for communication.
 * @return An initialized PrinterJobManager instance.
 */
- (instancetype)initWithThermalPrinterLibrary:(PosThermalPrinter *)thermalPrinterLibrary;

/**
 * @brief Retries a pending print job using a new printer.
 * @param jobId The ID of the job to retry.
 * @param newPrinterIP The IP address of the new printer.
 * @param completion A block to be called when the operation completes. The block takes a BOOL parameter indicating success.
 */
- (void)retryPendingJobFromNewPrinter:(NSString *)jobId newPrinterIP:(NSString *)newPrinterIP completion:(void (^)(BOOL success))completion;

/**
 * @brief Deletes pending print jobs with the specified job ID.
 * @param jobId The ID of the job(s) to delete.
 * @param completion A block to be called when the operation completes. The block takes a BOOL parameter indicating success.
 */
- (void)deletePendingJobs:(NSString *)jobId completion:(void (^)(BOOL success))completion;

/**
 * @brief Retrieves details of all pending print jobs.
 * @param completion A block to be called with an array of pending job details.
 */
- (void)getPendingJobDetails:(void (^)(NSArray *pendingJobs))completion;

/**
 * @brief Sets up print jobs for a specific printer.
 * @param ip The IP address of the printer.
 * @param content An array of print job content.
 * @param metadata Additional metadata for the print jobs.
 * @param completion A block to be called when the operation completes. The block takes a BOOL parameter indicating success.
 */
- (void)setPrintJobs:(NSString *)ip content:(NSArray *)content metadata:(NSString *)metadata completion:(void (^)(BOOL success))completion;

/**
 * @brief Updates all jobs for a printer with a new IP address.
 * @param oldIP The old IP address of the printer.
 * @param newIP The new IP address of the printer.
 * @param completion A block to be called when the operation completes. The block takes a BOOL parameter indicating whether any jobs were updated.
 */
- (void)updateJobsForPrinter:(NSString *)oldIP toNewIP:(NSString *)newIP completion:(void (^)(BOOL jobsUpdated))completion;

@end
