#import "PrinterJobManager.h"
#import "PrintItemProcessor.h"
#import "PrinterUtils.h"
#import "PrintItem.h"
#import "POSWIFIManager.h"
#import "PosCommand.h"
#import "PosThermalPrinter.h"
#import "HelperFunctions.h"

@interface PrinterJobManager ()

@property (nonatomic, weak) PosThermalPrinter *thermalPrinterLibrary;
@property (nonatomic, strong) NSMutableArray *printQueue;
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
        _printExecutor = dispatch_queue_create("com.printer.printExecutor", DISPATCH_QUEUE_SERIAL);
        _isPaused = NO;
        _queueLock = [[NSLock alloc] init];
    }
    return self;
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
            for (PrinterJob *job in self.printQueue) {
                if ([job.jobId isEqualToString:jobId]) {
                    jobToPrint = job;
                    [self.printQueue removeObject:job];
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
//                    [self.secondLibrary sendPrinterUnreachableEvent:newPrinterIP];
                } else {
                    [self.queueLock lock];
                    [self.printQueue addObject:jobToPrint];
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
        for (PrinterJob *job in self.printQueue) {
            if (![job.jobId isEqualToString:jobId]) {
                [jobsToKeep addObject:job];
            } else {
                jobFound = YES;
            }
        }
        
        self.printQueue = jobsToKeep;
        
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
            
            for (PrinterJob *job in self.printQueue) {
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

- (void)setPrintJobs:(NSString *)ip content:(NSArray *)content metadata:(NSString *)metadata completion:(void (^)(BOOL success))completion {
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
    [self.queueLock lock];
    [self.printQueue addObject:job];
    [self.queueLock unlock];
    
    [self processPrintQueue];
}

- (void)processPrintQueue {
    NSLog(@"ProcessPrintQueue executing");
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
            
            job.pending = YES;
            [self.queueLock lock];
            [self.printQueue addObject:job];
            [self.queueLock unlock];
            [[PrinterConnectionManager sharedInstance] sendPrinterUnreachableEventOnce:job.targetPrinterIp];
            
        }
        
        [self.queueLock lock];
        self.currentJob = nil;
        [self.queueLock unlock];
        
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)), self.printExecutor, ^{
            [self processNextJob];
        });
    }];
}

- (void)printJob:(PrinterJob *)job completion:(void (^)(BOOL success))completion {
    POSWIFIManager *wifiManager = [[POSWIFIManager alloc] init];

    [wifiManager POSConnectWithHost:job.targetPrinterIp port:9100 completion:^(BOOL isConnect) {
        if (isConnect) {
            NSMutableData *dataM = [NSMutableData dataWithData:[PosCommand initializePrinter]];
            
            for (PrintItem *item in job.jobContent) {
                switch (item.fontSize) {
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
                
                switch (item.type) {
                    case PrintItemTypeText:
                        [self addTextToPrintData:dataM item:item];
                        break;
                    case PrintItemTypeFeed:
                        [dataM appendData:[PosCommand printAndFeedForwardWhitN:item.lines]];
                        break;
                    case PrintItemTypeImage:
                        [dataM appendData:[PosCommand printAndFeedLine]];
                        [dataM appendData:[PosCommand printRasteBmpWithM:RasterNolmorWH andImage:item.bitmapImage andType:Dithering]];
                        break;
                    case PrintItemTypeColumn:
                        [self addColumnToPrintData:dataM item:item];
                        break;
                    case PrintItemTypeQRCode:
                        [self addQRCodeToPrintData:dataM item:item];
                        break;
                    case PrintItemTypeCashBox:
                        [dataM appendData:[PosCommand openCashBoxRealTimeWithM:0 andT:2]];
                        break;
                    case PrintItemTypeCut:
                        [dataM appendData:[PosCommand selectCutPageModelAndCutpage:0]];
                        break;
                    default:
                        break;
                }
            }
            
            [wifiManager POSWriteCommandWithData:dataM];
            [wifiManager POSDisConnect];
            
            completion(YES);
        } else {
            [[PrinterConnectionManager sharedInstance] sendPrinterUnreachableEventOnce:job.targetPrinterIp];
            completion(NO);
        }
    }];
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
    
    NSArray<NSString *> *lines = [PrinterUtils splitTextIntoLines:item.text width:width wrapWords:item.wrapWords];
    

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
        
        // Update jobs in the queue
        for (PrinterJob *job in self.printQueue) {
            if ([job.targetPrinterIp isEqualToString:oldIP]) {
                job.targetPrinterIp = newIP;
                jobsUpdated = YES;
            }
        }
        
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

@end
