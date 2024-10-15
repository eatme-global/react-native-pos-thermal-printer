#import <Foundation/Foundation.h>

@class PosThermalPrinter;

@interface PrinterConnectionManager : NSObject

/**
 * @brief Returns the shared instance of the PrinterConnectionManager.
 * @return The singleton instance of PrinterConnectionManager.
 */
+ (instancetype)sharedInstance;

/**
 * @brief Initializes the PrinterConnectionManager with a SecondLibrary instance.
 * @param PosThermalPrinter The SecondLibrary instance to use for communication.
 * @return An initialized PrinterConnectionManager instance.
 */
- (instancetype)initWithThermalPrinterLibrary:(PosThermalPrinter *)thermalPrinterLibrary;

/**
 * @brief Checks if a printer is reachable.
 * @param printerIp The IP address of the printer to check.
 * @param completion A block to be called when the check is complete. The block takes a BOOL parameter indicating reachability.
 */
- (void)checkIsReachable:(NSString *)printerIp completion:(void (^)(BOOL isReachable))completion;

/**
 * @brief Attempts to reconnect to a printer.
 * @param printerIp The IP address of the printer to reconnect.
 * @param completion A block to be called when the retry attempt is complete. The block takes a BOOL parameter indicating success.
 */
- (void)retryPrinterConnection:(NSString *)printerIp completion:(void (^)(BOOL success))completion;

/**
 * @brief Adds a printer to the printer pool.
 * @param config A dictionary containing the printer configuration.
 * @param completion A block to be called when the addition is complete. The block takes a BOOL parameter indicating success.
 */
- (void)addPrinterToPool:(NSDictionary *)config completion:(void (^)(BOOL success))completion;

/**
 * @brief Removes a printer from the printer pool.
 * @param printerIp The IP address of the printer to remove.
 * @param completion A block to be called when the removal is complete. The block takes a BOOL parameter indicating success.
 */
- (void)removePrinterFromPool:(NSString *)printerIp completion:(void (^)(BOOL success))completion;

/**
 * @brief Retrieves the status of all printers in the pool.
 * @param completion A block to be called with an array of printer status dictionaries.
 */
- (void)getPrinterPoolStatus:(void (^)(NSArray *printers))completion;

/**
 * @brief Sends a printer unreachable event once for a given printer IP.
 * @param printerIp The IP address of the unreachable printer.
 */
- (void)sendPrinterUnreachableEventOnce:(NSString *)printerIp;

@end
