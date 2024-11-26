#import "PrinterJobManager.h"
#import "PrintItemProcessor.h"
#import "PrinterUtils.h"
#import "PrintItem.h"
#import "POSWIFIManager.h"
#import "PosCommand.h"
#import "PosThermalPrinter.h"
#import "HelperFunctions.h"
#import "PrinterStatus.h"

@interface PrinterJobManager ()

@property (nonatomic, weak) PosThermalPrinter *thermalPrinterLibrary;
@property (nonatomic, strong) NSMutableArray *printQueue;
@property (nonatomic, strong) NSMutableArray *pendingQueue;
@property (nonatomic, strong) dispatch_queue_t printExecutor;
@property (atomic, assign) BOOL isPaused;
@property (atomic, strong) PrinterJob *currentJob;
@property (nonatomic, strong) NSLock *queueLock;

@end

@implementation PrinterJobManager

- (instancetype)initWithThermalPrinterLibrary:(PosThermalPrinter *)thermalPrinterLibrary {
    self = [super init];
    if (self) {
        _thermalPrinterLibrary = thermalPrinterLibrary;
        _printQueue = [NSMutableArray new];
        _pendingQueue = [NSMutableArray new];
        _printExecutor = dispatch_queue_create("com.printer.printExecutor", DISPATCH_QUEUE_SERIAL);
        _isPaused = NO;
        _queueLock = [[NSLock alloc] init];
    }
    return self;
}

- (NSTimeInterval)timeoutForType:(NSString *)type {
    if ([type isEqualToString:@"KOT"]) {
        return 0.3;
    } else if ([type isEqualToString:@"RECEIPT"]) {
        return 1.0;
    } else if ([type isEqualToString:@"BILL"]) {
        return 1.0;
    } else if ([type isEqualToString:@"TEST_CONNECTION"]) {
        return 0.1;
    } else if ([type isEqualToString:@"CASH_IN_OUT"]) {
        return 0.1;
    } else if ([type isEqualToString:@"OPEN_DRAWER"]) {
        return 0.1;
    } else if ([type isEqualToString:@"SHIFT_OPEN_SUMMARY"]) {
        return 0.4;
    } else if ([type isEqualToString:@"SHIFT_CLOSE_SUMMARY"]) {
        return 0.8;
    } else if ([type isEqualToString:@"ITEM_SALES_REPORT"]) {
        return 0.4;
    }
    return 0.5;
}


- (void)retryPendingJobFromNewPrinter:(NSString *)jobId newPrinterIP:(NSString *)newPrinterIP completion:(void (^)(BOOL success))completion {
    dispatch_async(self.printExecutor, ^{
        BOOL jobFound = NO;
        PrinterJob *jobToPrint = nil;
        
        [self.queueLock lock];
        
        if (self.currentJob && [self.currentJob.jobId isEqualToString:jobId]) {
            jobToPrint = self.currentJob;
            self.currentJob = nil;
            jobFound = YES;
        }
        
        if (!jobFound) {
            for (PrinterJob *job in self.pendingQueue) {
                if ([job.jobId isEqualToString:jobId]) {
                    jobToPrint = job;
                    [self.pendingQueue removeObject:job];
                    jobFound = YES;
                    break;
                }
            }
        }
        
        [self.queueLock unlock];
        
      
        
        if (jobFound) {
            jobToPrint.targetPrinterIp = newPrinterIP;
            [self printJob:jobToPrint completion:^(BOOL success) {
                if (success) {
                    // Do Nothing
                } else {
                    [self.queueLock lock];
                    [self.pendingQueue addObject:jobToPrint];
                    [self.queueLock unlock];
                }
                completion(success);
                [self processPrintQueue];
            }];
        } else {
            completion(NO);
        }
    });
}

- (void)deletePendingJobs:(NSString *)jobId completion:(void (^)(BOOL success))completion {
    dispatch_async(self.printExecutor, ^{
        BOOL jobFound = NO;
        
        [self.queueLock lock];
        
        if (self.currentJob && [self.currentJob.jobId isEqualToString:jobId]) {
            self.currentJob = nil;
            jobFound = YES;
        }
        
        NSMutableArray *jobsToKeep = [NSMutableArray array];
        for (PrinterJob *job in self.pendingQueue) {
            if (![job.jobId isEqualToString:jobId]) {
                [jobsToKeep addObject:job];
            } else {
                jobFound = YES;
            }
        }
        
        self.pendingQueue = jobsToKeep;
        
        [self.queueLock unlock];
        
        completion(jobFound);
    });
}

- (void)getPendingJobDetails:(void (^)(NSArray *pendingJobs))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSMutableArray *pendingJobsArray = [NSMutableArray array];
        
        self.isPaused = YES;
        
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [self.queueLock lock];
            
            if (self.currentJob && self.currentJob.pending) {
                [pendingJobsArray addObject:[self jobToDictionary:self.currentJob]];
            }
            
            for (PrinterJob *job in self.pendingQueue) {
                if (job.pending) {
                    [pendingJobsArray addObject:[self jobToDictionary:job]];
                }
            }
            
            
            [self.queueLock unlock];
            
            self.isPaused = NO;
            
            [self processPrintQueue];
            
            completion(pendingJobsArray);
        });
    });
}

- (void) setPrintJobs:(NSString *)ip content:(NSArray *)content metadata:(NSString *)metadata completion:(void (^)(BOOL success))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSMutableArray *printItems = [NSMutableArray array];
        
        for (NSDictionary *item in content) {
            PrintItem *printItem = [PrintItemProcessor createPrintItemFromDictionary:item];
            if (printItem) {
                [printItems addObject:printItem];
            }
        }
        
        NSString *printerName = [NSString stringWithFormat:@"PrinterName_%@", ip];
        NSString *jobId = [PrinterUtils generateUniqueJobId];
        PrinterJob *job = [[PrinterJob alloc] initWithPrinterIp:ip jobContent:printItems printerName:printerName metadata:metadata jobId:jobId];
        
        [self addPrintJob:job];
        completion(YES);
    });
}

- (void)addPrintJob:(PrinterJob *)job {
    if (!job) {
        NSLog(@"Warning: Attempting to add nil job");
        return;
    }

    [self.queueLock lock];

    @try {
            [self.printQueue addObject:job];
            NSLog(@"Job added to print queue for printer: %@", job.targetPrinterIp);
    }
    @finally {
        [self.queueLock unlock];  // Always unlock in finally block
    }
  
    [self processPrintQueue];
}

- (void)processPrintQueue {
    dispatch_async(self.printExecutor, ^{
        [self processNextJob];
    });
}

- (void)processNextJob {
    
    if (self.isPaused) {
        return;
    }
    
    [self.queueLock lock];
    PrinterJob *job = nil;
    if (self.currentJob == nil && self.printQueue.count > 0) {
        self.currentJob = self.printQueue.firstObject;
        [self.printQueue removeObjectAtIndex:0];
    }
    job = self.currentJob;
    [self.queueLock unlock];
    
    if (job == nil) {
        return;
    }
    
    [self printJob:job completion:^(BOOL success) {
        if (!success) {
            [self.thermalPrinterLibrary sendPrinterUnreachableEvent:job.targetPrinterIp];
        }
        
     
      
        [self.queueLock lock];
        self.currentJob = nil;
        [self.queueLock unlock];
        
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1 * NSEC_PER_SEC)), self.printExecutor, ^{
            [self processNextJob];
        });
    }];
}

- (void)printJob:(PrinterJob *)job completion:(void (^)(BOOL success))completion {
    POSWIFIManager *wifiManager = [[POSWIFIManager alloc] init];
    
    // Check printer status
    PrinterController *printer = [[PrinterController alloc] initWithIP:job.targetPrinterIp port:9100];
    BOOL isOffline = [printer isPrinterOffline];
    [printer disconnectPrinter];
    
    NSLog(@"Printer is %@", isOffline ? @"offline" : @"online");
    
    // Create connection block that handles the printing process
    void (^connectAndPrint)(void) = ^{
        [wifiManager POSConnectWithHost:job.targetPrinterIp port:9100 completion:^(BOOL isConnect) {
            if (!isConnect) {
                [[PrinterConnectionManager sharedInstance] sendPrinterUnreachableEventOnce:job.targetPrinterIp];
                completion(NO);
                return;
            }
            
            // Initialize printer and prepare data
            NSMutableData *dataM = [NSMutableData dataWithData:[PosCommand initializePrinter]];
            
            // Process print items
            for (PrintItem *item in job.jobContent) {
                // Handle font size
                [self configureFontSize:item.fontSize forData:dataM];
                
                // Handle item type
                [self processItemType:item withData:dataM wifiManager:wifiManager];
            }
            
            // Send print data
            [wifiManager POSWriteCommandWithData:dataM];
            
            // Handle completion and cleanup
            [self handlePrintCompletion:job wifiManager:wifiManager completion:completion];
        }];
    };
    
    // If printer is offline, delay before trying to print
    if (isOffline) {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)),
                      dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), connectAndPrint);
    } else {
        connectAndPrint();
    }
}

- (void)configureFontSize:(FontSize)fontSize forData:(NSMutableData *)dataM {
    switch (fontSize) {
        case FontSizeNormal:
            [dataM appendData:[HelperFunctions selectFont:1 width:1]];
            break;
        case FontSizeWide:
            [dataM appendData:[HelperFunctions selectFont:1 width:2]];
            break;
        case FontSizeTall:
            [dataM appendData:[HelperFunctions selectFont:2 width:1]];
            break;
        case FontSizeBig:
            [dataM appendData:[HelperFunctions selectFont:2 width:2]];
            break;
    }
}

- (void)processItemType:(PrintItem *)item withData:(NSMutableData *)dataM wifiManager:(POSWIFIManager *)wifiManager {
    switch (item.type) {
        case PrintItemTypeText:
            [self addTextToPrintData:dataM item:item];
            break;
            
        case PrintItemTypeFeed:
            [dataM appendData:[PosCommand printAndFeedForwardWhitN:item.lines]];
            break;
            
        case PrintItemTypeImage:
            [dataM appendData:[PosCommand printAndFeedLine]];
            [dataM appendData:[PosCommand printRasteBmpWithM:RasterNolmorWH
                                                  andImage:item.bitmapImage
                                                  andType:Dithering]];
            break;
            
        case PrintItemTypeColumn:
            [self addColumnToPrintData:dataM item:item];
            break;
            
        case PrintItemTypeQRCode:
            [self addQRCodeToPrintData:dataM item:item];
            break;
            
        case PrintItemTypeCashBox:
            [wifiManager POSWriteCommandWithData:[PosCommand openCashBoxRealTimeWithM:0 andT:2]];
            break;
            
        case PrintItemTypeCut:
            [dataM appendData:[PosCommand selectCutPageModelAndCutpage:0]];
            break;
            
        default:
            break;
    }
}

- (void)handlePrintCompletion:(PrinterJob *)job
                 wifiManager:(POSWIFIManager *)wifiManager
                 completion:(void (^)(BOOL))completion {
    // Parse metadata
    NSError *jsonError;
    NSDictionary *metadata = [NSJSONSerialization JSONObjectWithData:[job.metadata dataUsingEncoding:NSUTF8StringEncoding]
                                                           options:NSJSONReadingAllowFragments
                                                             error:&jsonError];
    NSString *printType = metadata[@"type"];
    NSTimeInterval timeout = [self timeoutForType:printType];
    
    // Handle completion with semaphore
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(timeout * NSEC_PER_SEC)),
                  dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        [wifiManager POSDisConnect];
        dispatch_semaphore_signal(semaphore);
    });
    
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    completion(YES);
}


- (void)addTextToPrintData:(NSMutableData *)dataM item:(PrintItem *)item {
    NSStringEncoding encodeCharset;
    
    if ([PrinterUtils containsChineseCharacter:item.text]) {
        encodeCharset = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000);
        [dataM appendData:[PosCommand selectChineseCharacterModel]];
        [dataM appendData:[PosCommand setChineseCharLeftAndRightSpaceWithN1:0 andN2:0]];
    } else {
        [dataM appendData:[PosCommand CancelChineseCharModel]];
        encodeCharset = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingDOSLatinUS);
    }

    [dataM appendData:[PosCommand selectAlignment:item.getAlignmentAsInt]];

    if (item.bold) {
        [dataM appendData:[PosCommand selectOrCancleBoldModel:1]];
    }

    NSInteger width = 48;
    
    if(item.fontSize == FontSizeBig || item.fontSize == FontSizeWide){
        width = 24;
    }

    NSString *itemString = [PrinterUtils sanitizeStringForPrinter:item.text];
    
    NSArray<NSString *> *lines = [PrinterUtils splitTextIntoLines:itemString width:width wrapWords:item.wrapWords];
    

    for (NSString *line in lines) {
       NSData *textData = [line dataUsingEncoding:encodeCharset];
       [dataM appendData:textData];
   
       [dataM appendData:[PosCommand printAndFeedLine]];
    }

    
    if (item.bold) {
        [dataM appendData:[PosCommand selectOrCancleBoldModel:0]];
    }
    
}

- (void)addColumnToPrintData:(NSMutableData *)dataM item:(PrintItem *)item {
    if (item.columns) {
        NSStringEncoding encodeCharset;
        
        if ([PrinterUtils columnsContainChineseCharacters:item.columns]) {
            encodeCharset = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000);
            [dataM appendData:[PosCommand selectChineseCharacterModel]];
            [dataM appendData:[PosCommand setChineseCharLeftAndRightSpaceWithN1:0 andN2:0]];
        } else {
            [dataM appendData:[PosCommand CancelChineseCharModel]];
            encodeCharset = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingDOSLatinUS);
        }
        
        NSInteger maxLines = 0;
        for (ColumnItem *col in item.columns) {
            if (col.lines) {
                maxLines = MAX(maxLines, col.lines.count);
            }
        }
        
        if (item.bold) {
            [dataM appendData:[PosCommand selectOrCancleBoldModel:1]];
        }
        
        for (NSInteger lineIndex = 0; lineIndex < maxLines; lineIndex++) {
            NSMutableString *lineBuilder = [NSMutableString string];
            
            for (ColumnItem *column in item.columns) {
                NSString *line = @"";
                if (column.lines && lineIndex < column.lines.count) {
                    line = column.lines[lineIndex];
                }
                
                if (!line) {
                    line = @"";
                }
                
                TextAlignment alignment = column.alignment ? column.alignment : TextAlignmentLeft;
                
                NSString *formattedText = [PrinterUtils padText:line width:column.width alignment:alignment];
                
                [lineBuilder appendString:formattedText];
            }
        
            NSData *data = [lineBuilder dataUsingEncoding:encodeCharset];
            [dataM appendData:data];
        }
    
        if (item.bold) {
            [dataM appendData:[PosCommand selectOrCancleBoldModel:0]];
        }
    }
}

- (void)addQRCodeToPrintData:(NSMutableData *)dataM item:(PrintItem *)item {
    [dataM appendData:[PosCommand selectAlignment:item.getAlignmentAsInt]];
    [dataM appendData:[PosCommand setQRcodeUnitsize:item.getUnits]];
    [dataM appendData:[PosCommand setErrorCorrectionLevelForQrcode:48]];
    [dataM appendData:[PosCommand sendDataToStoreAreaWitQrcodeConent:item.text usEnCoding:CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000)]];
    [dataM appendData:[PosCommand printTheQRcodeInStore]];
}

- (void)updateJobsForPrinter:(NSString *)oldIP toNewIP:(NSString *)newIP completion:(void (^)(BOOL jobsUpdated))completion {
    dispatch_async(self.printExecutor, ^{
        self.isPaused = YES;
        
        [self.queueLock lock];
        
        BOOL jobsUpdated = NO;
        
        // Filter jobs that need to be updated
        NSPredicate *predicate = [NSPredicate predicateWithFormat:@"targetPrinterIp == %@", oldIP];
        NSArray *jobsToMove = [self.pendingQueue filteredArrayUsingPredicate:predicate];
        
        // Update and move jobs
        for (PrinterJob *job in jobsToMove) {
            job.targetPrinterIp = newIP;
            [self.printQueue addObject:job];
            jobsUpdated = YES;
        }
        
        // Remove moved jobs from pending queue
        [self.pendingQueue removeObjectsInArray:jobsToMove];
            
        
        
        // Update current job if it exists
        if (self.currentJob && [self.currentJob.targetPrinterIp isEqualToString:oldIP]) {
            self.currentJob.targetPrinterIp = newIP;
            jobsUpdated = YES;
        }
        
        [self.queueLock unlock];
        
        self.isPaused = NO;
        
        // Resume processing if there are jobs in the queue
        if (self.printQueue.count > 0 || self.currentJob) {
            [self processPrintQueue];
        }
        
        completion(jobsUpdated);
    });
}


- (NSDictionary *)jobToDictionary:(PrinterJob *)job {
    return @{
        @"printerIp": job.targetPrinterIp ?: [NSNull null],
        @"printerName": job.printerName ?: [NSNull null],
        @"metadata": job.metadata ?: [NSNull null],
        @"jobId": job.jobId ?: [NSNull null]
    };
}


// New Printer Implementation

- (void)dismissPrinterJobs:(NSString *)printerIp completion:(void (^)(BOOL removed))completion {
    dispatch_async(self.printExecutor, ^{
        self.isPaused = YES;
        
        [self.queueLock lock];
        
        BOOL jobsUpdated = NO;
        
        // Check and update current job
        if (self.currentJob && [self.currentJob.targetPrinterIp isEqualToString:printerIp]) {
            self.currentJob = nil;
            jobsUpdated = YES;
        }
        
        // Remove jobs from pending queue for the specified printer IP
        NSMutableArray *jobsToKeep = [NSMutableArray array];
        for (PrinterJob *job in self.pendingQueue) {
            if (![job.targetPrinterIp isEqualToString:printerIp]) {
                [jobsToKeep addObject:job];
            } else {
                jobsUpdated = YES;
            }
        }
        
        // Update pending queue with filtered jobs
        if (self.pendingQueue.count != jobsToKeep.count) {
            self.pendingQueue = jobsToKeep;
            jobsUpdated = YES;
        }
        
        [self.queueLock unlock];
        
        self.isPaused = NO;
        
        // Resume queue processing if there are jobs
        if (self.printQueue.count > 0 || self.currentJob) {
            [self processPrintQueue];
        }
        
        completion(jobsUpdated);
    });
}


- (void)getPrinterPendingJobDetails:(NSString *)printerIp completion:(void (^)(NSArray *pendingJobs))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSMutableArray *pendingJobsArray = [NSMutableArray array];
        
        self.isPaused = YES;
        
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [self.queueLock lock];
            
            // Check current job
            if (self.currentJob &&
                self.currentJob.pending &&
                [self.currentJob.targetPrinterIp isEqualToString:printerIp]) {
                [pendingJobsArray addObject:[self jobToDictionary:self.currentJob]];
            }
            

            NSLog(@"pendingQueue count: %lu", (unsigned long)self.pendingQueue.count);
            // Check pending queue
            for (PrinterJob *job in self.pendingQueue) {
                
                if (job.pending && [job.targetPrinterIp isEqualToString:printerIp]) {
                    NSLog(@"printer IP: %@", job.targetPrinterIp);
                    [pendingJobsArray addObject:[self jobToDictionary:job]];
                }
            }
            
            [self.queueLock unlock];
            
            self.isPaused = NO;
            
            [self processPrintQueue];
            
            NSLog(@"pending count: %lu", (unsigned long)pendingJobsArray.count);

            
            completion(pendingJobsArray);
        });
    });
}


@end
