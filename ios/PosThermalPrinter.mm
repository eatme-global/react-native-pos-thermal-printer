#import "PosThermalPrinter.h"
#import "PrinterJobManager.h"
#import "PrinterConnectionManager.h"
#import "PrinterUtils.h"

#import "PrinterStatus.h"

@interface PosThermalPrinter()

@property (nonatomic, strong) PrinterJobManager *printerJobManager;
@property (nonatomic,
           strong) PrinterConnectionManager *printerConnectionManager;

@end

@implementation PosThermalPrinter
RCT_EXPORT_MODULE()

@synthesize bridge = _bridge;

/**
 * @brief Returns an array of supported event names.
 * @return An array containing the names of supported events.
 */
- (NSArray<NSString *> *)supportedEvents {
  return @[@"PrinterUnreachable", @"PrePrintCheck"];
}


- (void)sendPrinterUnreachableEvent:(NSString *)printerIp {
  if (self.bridge) {
    [self sendEventWithName:@"PrinterUnreachable" body:@{@"printerIp": printerIp}];
  }
}

/**
 * @brief Initializes the SecondLibrary instance.
 * @return An initialized SecondLibrary instance.
 */
- (instancetype)init {
  self = [super init];
  if (self) {
    _printerConnectionManager = [[PrinterConnectionManager alloc] initWithThermalPrinterLibrary:self];
    _printerJobManager = [[PrinterJobManager alloc] initWithThermalPrinterLibrary:self];
  }
  return self;
}

/**
 * @brief Sets the React Native bridge for this module.
 * @param bridge The RCTBridge instance to set.
 */
- (void)setBridge:(RCTBridge *)bridge {
  _bridge = bridge;
}

/**
 * @brief Retries a pending print job using a new printer.
 * @param jobId The ID of the job to retry.
 * @param newPrinterIP The IP address of the new printer.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(retryPendingJobFromNewPrinter:(NSString *)jobId
                  config:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *printerIP = config[@"ip"];
  [self.printerJobManager retryPendingJobFromNewPrinter:jobId newPrinterIP:printerIP completion:^(BOOL success) {
    resolve(@(success));
  }];
}

/**
 * @brief Deletes pending print jobs.
 * @param jobId The ID of the job(s) to delete.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(deletePendingJobs:(NSString *)jobId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerJobManager deletePendingJobs:jobId completion:^(BOOL success) {
    resolve(@(success));
  }];
}

/**
 * @brief Retrieves details of pending print jobs.
 * @param resolve A block to call with the pending job details.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(getPendingJobDetails:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerJobManager getPendingJobDetails:^(NSArray *pendingJobs) {
    resolve(pendingJobs);
  }];
}

/**
 * @brief Updates print jobs from an old printer to a new printer.
 * @param oldPrinterIp The IP address of the old printer.
 * @param newPrinterIp The IP address of the new printer.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(printFromNewPrinter:(NSDictionary *)oldPrinter
                  newPrinterIp:(NSDictionary *)newPrinter
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  
  NSString *oldPrinterIp = oldPrinter[@"ip"];
  NSString *newPrinterIp = newPrinter[@"ip"];

  [self.printerJobManager updateJobsForPrinter:oldPrinterIp toNewIP:newPrinterIp completion:^(BOOL jobsUpdated) {
    resolve(@YES);
  }];
}


/**
 * @brief Initializes the printer pool.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(initializePrinterPool:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  // This method doesn't do anything in the original code, so we'll just resolve with YES
  resolve(@YES);
}

/**
 * @brief Checks if a printer is reachable.
 * @param printerIp The IP address of the printer to check.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(checkIsReachable:(NSString *)printerIp
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerConnectionManager checkIsReachable:printerIp completion:^(BOOL isReachable) {
    resolve(@(isReachable));
  }];
}

/**
 * @brief Retries connecting to a printer.
 * @param printerIp The IP address of the printer to reconnect.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(retryPrinterConnection:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  
  NSString *printerIp = config[@"ip"];
  [self.printerConnectionManager retryPrinterConnection:printerIp completion:^(BOOL success) {
    if (success) {
      NSLog(@"Printer Connection is successful");
      resolve(@YES);
    } else {
      NSLog(@"Printer Connection failed");
      resolve(@NO);
    }
  }];
}

RCT_EXPORT_METHOD(retryPendingJobs:(NSString *)printerIp
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerConnectionManager retryPrinterConnection:printerIp completion:^(BOOL success) {
    if (success) {
      NSLog(@"Printer Connection is successful");
      [self.printerJobManager updateJobsForPrinter:printerIp toNewIP:printerIp completion:^(BOOL jobsUpdated) {
        resolve(@(jobsUpdated));
      }];
      resolve(@YES);
    } else {
      NSLog(@"Printer Connection failed");
      resolve(@NO);
    }
  }];
}



/**
 * @brief Sets print jobs for a specific printer.
 * @param ip The IP address of the printer.
 * @param content The content of the print jobs.
 * @param metadata Additional metadata for the print jobs.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(setPrintJobs:(NSDictionary *)config
                  content:(NSArray *)content
                  metadata:(NSString *)metadata
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  // Send pre-print event
  if (self.bridge) {
    [self sendEventWithName:@"PrePrintCheck" body:@""];
  }
      
  NSString *printerIp = config[@"ip"];
  
  
  // Add a small delay to allow the JS side to process
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)),
  dispatch_get_main_queue(),
                 ^{
    [self.printerJobManager setPrintJobs:printerIp content:content metadata:metadata completion:^(BOOL success) {
      resolve(@(success));
    }];
  });
}

/**
 * @brief Adds a printer to the printer pool.
 * @param config A dictionary containing the printer configuration.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(addPrinterToPool:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerConnectionManager addPrinterToPool:config completion:^(BOOL success) {
    resolve(@(success));
  }];
}

/**
 * @brief Adds a printer to the printer pool.
 * @param config A dictionary containing the printer configuration.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(checkPrinterStatus:(NSString *)printerIp
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
   
  // Initialize the printer controller
  PrinterController *printer = [[PrinterController alloc] initWithIP:printerIp port:9100];

  // Asynchronous status check
  [printer checkPrinterStatusWithCompletion:^(BOOL isOffline,
                                              PrinterStatus *status,
                                              NSError *error) {
    if (error) {
      NSLog(@"Error: %@", error.localizedDescription);
      return;
    }
        
    if (isOffline) {
      NSLog(@"Printer is offline");
    } else {
      NSLog(@"Printer is online");
    }
  }];

  // Clean up when done
  [printer disconnectPrinter];
}

/**
 * @brief Removes a printer from the printer pool.
 * @param printerIp The IP address of the printer to remove.
 * @param resolve A block to call with the result of the operation.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(removePrinterFromPool:(NSString *)printerIp
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerConnectionManager removePrinterFromPool:printerIp completion:^(BOOL success) {
    resolve(@(success));
  }];
}

/**
 * @brief Retrieves the status of the printer pool.
 * @param resolve A block to call with the printer pool status.
 * @param reject A block to call if an error occurs.
 */
RCT_EXPORT_METHOD(getPrinterPoolStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [self.printerConnectionManager getPrinterPoolStatus:^(NSArray *printers) {
    resolve(printers);
  }];
}


// New Implementations

RCT_EXPORT_METHOD(getPrinterPendingJobDetails:(NSDictionary *)config
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *printerIp = config[@"ip"];
  [self.printerJobManager getPrinterPendingJobDetails:printerIp completion:^(NSArray *pendingJobs) {
    resolve(pendingJobs);
  }];
}


RCT_EXPORT_METHOD(dismissPendingJobs:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *printerIp = config[@"ip"];
  [self.printerConnectionManager retryPrinterConnection:printerIp completion:^(BOOL success) {
    if (success) {
      NSLog(@"Printer Connection is successful");
                
      [self.printerJobManager dismissPrinterJobs:printerIp completion:^(BOOL removed) {
        resolve(@(removed));
      }];
    } else {
      NSLog(@"Printer Connection failed");
      resolve(@NO);
    }
  }];
}


RCT_EXPORT_METHOD(retryPendingJobsFromPrinter:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  NSString *printerIp = config[@"ip"];
  [self.printerConnectionManager retryPrinterConnection:printerIp completion:^(BOOL success) {
    if (success) {
      NSLog(@"Printer Connection is successful");
                
      [self.printerJobManager updateJobsForPrinter:printerIp toNewIP:printerIp completion:^(BOOL jobsUpdated) {
        resolve(@(jobsUpdated));
      }];
                
    } else {
      NSLog(@"Printer Connection failed");
      resolve(@NO);
    }
  }];
}




@end
