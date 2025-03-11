// PrinterConnectionUtils.m

#import "PrinterConnectionUtils.h"
#import <React/RCTLog.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import "POSWIFIManager.h"
#import "PosThermalPrinter.h"

@interface PrinterConnectionUtils ()

@property (nonatomic, strong) NSMutableArray<NSString *> *currentPrinterIps;
@property (nonatomic, strong) NSMutableDictionary<NSString *, NSNumber *> *reachabilityMap;
@property (nonatomic, assign) BOOL showLogs;
@property (nonatomic, strong) dispatch_queue_t monitorQueue;
@property (nonatomic, strong) dispatch_queue_t pingQueue;
@property (nonatomic, strong) dispatch_source_t cycleTimer;
@property (nonatomic, strong) NSDate *lastCycleStartTime;
//@property (atomic, assign) BOOL isMonitoringRunning;

@end

@implementation PrinterConnectionUtils

@synthesize thermalPrinterLibrary;

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents {
    return @[@"PrinterUnreachable"];
}

- (void)sendPrinterUnreachableEvent:(NSString *)printerIp {
    dispatch_async(dispatch_get_main_queue(), ^{
        [self sendEventWithName:@"PrinterUnreachable" body:@{@"printerIp": printerIp}];
    });
}

+ (instancetype)sharedInstance {
    static PrinterConnectionUtils *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[self alloc] init];
    });
    return sharedInstance;
}

- (instancetype)initWithThermalPrinterLibrary:(PosThermalPrinter *)thermalPrinterLibrary showLogs:(BOOL)showLogs {
    self = [super init];
    if (self) {
        _currentPrinterIps = [NSMutableArray array];
        _reachabilityMap = [NSMutableDictionary dictionary];
        _showLogs = showLogs;
        _monitorQueue = dispatch_queue_create("com.printerutils.monitorQueue", dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_SERIAL, QOS_CLASS_USER_INITIATED, 0));
        _pingQueue = dispatch_queue_create("com.printerutils.pingQueue", dispatch_queue_attr_make_with_qos_class(DISPATCH_QUEUE_CONCURRENT, QOS_CLASS_USER_INITIATED, 0));
        self.thermalPrinterLibrary = thermalPrinterLibrary;
    }
    return self;
}

- (void)startPeriodicReachabilityCheck {
    dispatch_async(self.monitorQueue, ^{
        if (self.cycleTimer) {
            dispatch_source_cancel(self.cycleTimer);
            self.cycleTimer = nil;
        }
        
        self.cycleTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, self.monitorQueue);
        dispatch_source_set_timer(self.cycleTimer, dispatch_time(DISPATCH_TIME_NOW, 0), 15 * NSEC_PER_SEC, 1 * NSEC_PER_SEC);
        
        __weak typeof(self) weakSelf = self;
        dispatch_source_set_event_handler(self.cycleTimer, ^{
            [weakSelf startNewCycle];
        });
        
        dispatch_resume(self.cycleTimer);
        
        self.isMonitoringRunning = YES;
        self.lastCycleStartTime = [NSDate date];
        
        if (self.showLogs) {
            dispatch_async(dispatch_get_main_queue(), ^{
                RCTLogInfo(@"Starting continuous printer reachability checks.");
            });
        }

        [self startNewCycle];
    });
}

- (void)stopPeriodicReachabilityCheck {
    dispatch_async(self.monitorQueue, ^{
        if (self.cycleTimer) {
            dispatch_source_cancel(self.cycleTimer);
            self.cycleTimer = nil;
        }
        
        self.isMonitoringRunning = NO;
        
        if (self.showLogs) {
            dispatch_async(dispatch_get_main_queue(), ^{
                RCTLogInfo(@"Stopping the printer reachability check.");
            });
        }
    });
}

- (BOOL)isMonitoringRunning {
    return _isMonitoringRunning;
}

- (void)startNewCycle {
    if (!self.isMonitoringRunning) return;
    
    NSDate *now = [NSDate date];
    if (self.lastCycleStartTime) {
        NSTimeInterval timeSinceLastCycle = [now timeIntervalSinceDate:self.lastCycleStartTime];
        NSLog(@"Time since last cycle start: %.2f seconds", timeSinceLastCycle);
    }
    self.lastCycleStartTime = now;
    
    NSLog(@"Starting new printer check cycle at: %@", now);
    
    [self checkPrinters];
}

- (void)checkPrinters {
    NSArray *printerIpsCopy;
    @synchronized(self) {
        printerIpsCopy = [self.currentPrinterIps copy];
    }
    
    for (NSString *printerIp in printerIpsCopy) {
       
                   [self checkPrinter:printerIp];
      
    }
}

- (void)checkPrinter:(NSString *)printerIp {
    if (!self.isMonitoringRunning) return;
    
    if (self.showLogs) {
        dispatch_async(dispatch_get_main_queue(), ^{
            RCTLogInfo(@"Checking printer reachability for IP: %@", printerIp);
        });
    }
    [NSThread sleepForTimeInterval:0.1];


    dispatch_async(self.pingQueue, ^{
        [self isPrinterReachable:printerIp completion:^(BOOL isReachable) {
            if (!self.isMonitoringRunning) return;
            
            @synchronized (self) {
                self.reachabilityMap[printerIp] = @(isReachable);
            }

            if (!isReachable) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self.thermalPrinterLibrary sendPrinterUnreachableEvent:printerIp];
                });
            }
        }];
    });
}

- (void)restartPeriodicCheck {
    dispatch_async(self.monitorQueue, ^{
        NSLog(@"Restarting periodic check");
        [self stopPeriodicReachabilityCheck];
        
        if (self.currentPrinterIps.count > 0) {
            [self startPeriodicReachabilityCheck];
        } else {
            NSLog(@"No printers to monitor, not restarting checks.");
        }
    });
}

- (void)addPrinterAndRestart:(NSString *)newPrinterIp {
    dispatch_async(self.monitorQueue, ^{
        if (![self.currentPrinterIps containsObject:newPrinterIp]) {
            RCTLogInfo(@"Adding new printer IP: %@", newPrinterIp);
            
            @synchronized(self) {
                [self.currentPrinterIps addObject:newPrinterIp];
                self.reachabilityMap[newPrinterIp] = @(YES);
            }
            
            [self restartPeriodicCheck];
        } else {
            RCTLogInfo(@"Printer IP already exists in the list: %@", newPrinterIp);
        }
    });
}

- (NSInteger)printerLength {
    @synchronized(self) {
        return self.currentPrinterIps.count;
    }
}

- (NSDictionary<NSString *, NSNumber *> *)getReachabilityMap {
    @synchronized(self) {
        return [self.reachabilityMap copy];
    }
}

- (NSNumber *)isReachable:(NSString *)printerIp {
    @synchronized(self) {
        return self.reachabilityMap[printerIp];
    }
}

- (void)removePrinterAndRestart:(NSString *)printerIp {
    dispatch_async(self.monitorQueue, ^{
        BOOL shouldRestart = NO;
        @synchronized(self) {
            if ([self.currentPrinterIps containsObject:printerIp]) {
                [self.currentPrinterIps removeObject:printerIp];
                [self.reachabilityMap removeObjectForKey:printerIp];
                shouldRestart = YES;
            }
        }
        
        if (shouldRestart) {
            [self restartPeriodicCheck];
        } else {
            RCTLogInfo(@"Printer IP not found in the list: %@", printerIp);
        }
    });
}

- (void)isPrinterReachable:(NSString *)printerIp completion:(void (^)(BOOL isReachable))completion {
    POSWIFIManager *wifiManager = [[POSWIFIManager alloc] init];

    [wifiManager POSConnectWithHost:printerIp port:9100 completion:^(BOOL isConnect) {
        if (isConnect) {
            [wifiManager POSDisConnect];
        }
        completion(isConnect);
    }];
}

@end
