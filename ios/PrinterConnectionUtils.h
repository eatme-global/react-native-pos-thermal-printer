#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>


@class PosThermalPrinter;  // Forward declaration

@interface PrinterConnectionUtils :  RCTEventEmitter <RCTBridgeModule>

@property (nonatomic, weak) PosThermalPrinter *thermalPrinterLibrary;

/**
 * @property isMonitoringRunning
 * @brief Indicates whether the printer monitoring process is currently active.
 */
@property (nonatomic, assign) BOOL isMonitoringRunning;


/**
 * @brief Returns the number of printers currently being monitored.
 * @return The count of monitored printers.
 */
- (NSInteger)printerLength;

/**
 * @brief Retrieves the current reachability status of all monitored printers.
 * @return A dictionary mapping printer IPs to their reachability status.
 */
- (NSDictionary<NSString *, NSNumber *> *)getReachabilityMap;

/**
 * @brief Checks if a specific printer is reachable.
 * @param printerIp The IP address of the printer to check.
 * @return An NSNumber representing the reachability status (YES if reachable, NO otherwise).
 */
- (NSNumber *)isReachable:(NSString *)printerIp;

/**
 * @brief Initializes the PrinterConnectionUtils instance.
 * @param PosThermalPrinter The SecondLibrary instance to use.
 * @param showLogs Whether to show debug logs.
 * @return An initialized PrinterConnectionUtils instance.
 */
- (instancetype)initWithThermalPrinterLibrary:(PosThermalPrinter *)thermalPrinterLibrary showLogs:(BOOL)showLogs;

/**
 * @brief Adds a new printer to the monitoring list and restarts the monitoring process.
 * @param newPrinterIp The IP address of the printer to add.
 */
- (void)addPrinterAndRestart:(NSString *)newPrinterIp;

/**
 * @brief Removes a printer from the monitoring list and restarts the monitoring process.
 * @param printerIp The IP address of the printer to remove.
 */
- (void)removePrinterAndRestart:(NSString *)printerIp;

/**
 * @brief Stops the periodic reachability check for all printers.
 */
- (void)stopPeriodicReachabilityCheck;

/**
 * @brief Checks if a specific printer is reachable.
 * @param printerIp The IP address of the printer to check.
 * @param completion A block to be called with the reachability result.
 */
- (void)isPrinterReachable:(NSString *)printerIp completion:(void (^)(BOOL isReachable))completion;

/**
 * @brief Starts the periodic reachability check for all monitored printers.
 */
- (void)startPeriodicReachabilityCheck;

/**
 * @brief Indicates whether the monitoring process is currently running.
 * @return YES if monitoring is active, NO otherwise.
 */
- (BOOL)isMonitoringRunning;

/**
 * @brief Returns the shared instance of PrinterConnectionUtils.
 * @return The singleton instance of PrinterConnectionUtils.
 */
+ (instancetype)sharedInstance;

@end
