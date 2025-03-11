#import "PrinterConnectionManager.h"
#import "PosThermalPrinter.h"
#import "PrinterConnectionUtils.h"

@interface PrinterConnectionManager ()

@property (nonatomic, weak) PosThermalPrinter *thermalPrinterLibrary;
@property (nonatomic, strong) NSMutableArray *printerPool;
@property (nonatomic, strong) NSMutableDictionary *printerConfigs;
@property (nonatomic, strong) PrinterConnectionUtils *printerConnectionUtils;
@property (nonatomic, strong) NSMutableSet<NSString *> *reportedUnreachablePrinters;

@end

@implementation PrinterConnectionManager

+ (instancetype)sharedInstance {
  static PrinterConnectionManager *sharedInstance = nil;
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    sharedInstance = [[self alloc] init];
  });
  return sharedInstance;
}



- (instancetype)init {
  self = [super init];
  if (self) {
    _printerPool = [NSMutableArray new];
    _printerConfigs = [NSMutableDictionary new];
    _printerConnectionUtils = [[PrinterConnectionUtils alloc] init];
    _reportedUnreachablePrinters = [NSMutableSet set];
  }
  return self;
}

- (instancetype)initWithThermalPrinterLibrary:(PosThermalPrinter *)thermalPrinterLibrary {
  PrinterConnectionManager *sharedInstance = [PrinterConnectionManager sharedInstance];
  sharedInstance.thermalPrinterLibrary = thermalPrinterLibrary;
  return sharedInstance;
}




- (void)checkIsReachable:(NSString *)printerIp completion:(void (^)(BOOL isReachable))completion {
  [self.printerConnectionUtils isPrinterReachable:printerIp completion:^(BOOL isReachable) {
    if (isReachable) {
      [self resetPrinterUnreachableStatus:printerIp];
    } else {
      [self sendPrinterUnreachableEventOnce:printerIp];
    }
    completion(isReachable);
  }];
}

- (void)retryPrinterConnection:(NSString *)printerIp completion:(void (^)(BOOL success))completion {
  [self.printerConnectionUtils isPrinterReachable:printerIp completion:^(BOOL isReachable) {
    if (isReachable) {
      [self resetPrinterUnreachableStatus:printerIp];
    }
    completion(isReachable);
  }];
}

- (void)addPrinterToPool:(NSDictionary *)config completion:(void (^)(BOOL success))completion {
  NSString *ip = config[@"ip"];
  
  
  [self.printerConnectionUtils isPrinterReachable:ip completion:^(BOOL isReachable) {
    if (isReachable) {
      if ([self.printerPool containsObject:ip]) {
        completion(YES);
        return;
      }
      
      self.printerConfigs[ip] = config;
      [self.printerPool addObject:ip];
      [self resetPrinterUnreachableStatus:ip];
      completion(YES);
    } else {
      completion(NO);
    }
  }];
}

- (void)removePrinterFromPool:(NSDictionary *)config completion:(void (^)(BOOL success))completion {
  NSString *printerIp = config[@"ip"];
  [self.printerConfigs removeObjectForKey:printerIp];
  [self.printerPool removeObject:printerIp];
  [self resetPrinterUnreachableStatus:printerIp];
  completion(YES);
}

- (void)getPrinterPoolStatus:(void (^)(NSArray *printers))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        
        NSMutableArray *connectedPrinters = [NSMutableArray new];
        
        for (NSString *printerIp in self.printerPool) {
          NSDictionary *printerConfig = self.printerConfigs[printerIp];
          
          if (printerConfig) {
            NSMutableDictionary *printerInfo = [NSMutableDictionary dictionaryWithDictionary:printerConfig];
            printerInfo[@"printerName"] = printerConfig[@"ip"];
            printerInfo[@"printerIp"] = printerIp;
            
            [self.printerConnectionUtils isPrinterReachable:printerIp completion:^(BOOL isReachable) {
             
                printerInfo[@"isReachable"] = @(isReachable);
                [connectedPrinters addObject:printerInfo];
                
                if (connectedPrinters.count == self.printerPool.count) {
                  completion(connectedPrinters);
                }
              
            }];
          }
        }
        
        if (self.printerPool.count == 0) {
          completion(connectedPrinters);
        }
    });
}

- (void)sendPrinterUnreachableEventOnce:(NSString *)printerIp {
  @synchronized (self.reportedUnreachablePrinters) {
    if (![self.reportedUnreachablePrinters containsObject:printerIp]) {
      [self.reportedUnreachablePrinters addObject:printerIp];
      [self.thermalPrinterLibrary sendPrinterUnreachableEvent:printerIp];
    }
  }
}

- (void)resetPrinterUnreachableStatus:(NSString *)printerIp {
  @synchronized (self.reportedUnreachablePrinters) {
    [self.reportedUnreachablePrinters removeObject:printerIp];
  }
}

@end
