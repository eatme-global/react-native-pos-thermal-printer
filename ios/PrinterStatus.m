// PrinterController.m

#import "PrinterStatus.h"

NSString * const PrinterControllerErrorDomain = @"com.yourcompany.printercontroller";

@interface PrinterController () {
    NSInteger index;
    BOOL monitorPrinter;
    NSInteger PrinterType;
}

@property (nonatomic, strong, readwrite) PrinterStatus *status;
@property (nonatomic, assign, readwrite) BOOL isConnected;
@property (nonatomic, copy, readwrite) NSString *printerIP;
@property (nonatomic, assign, readwrite) NSInteger printerPort;
@property (nonatomic, strong) NSInputStream *inputStream;
@property (nonatomic, strong) NSOutputStream *outputStream;

@end

@implementation PrinterController

#pragma mark - Initialization

- (id)initWithIP:(NSString *)ip port:(NSInteger)port {
    self = [super init];
    if (self) {
        _printerIP = [ip copy];
        _printerPort = port;
        _status = [[PrinterStatus alloc] init];
        _isConnected = NO;
        monitorPrinter = YES;
        index = 0;
        PrinterType = 0;
    }
    return self;
}

#pragma mark - Connection Management

- (BOOL)connectToPrinter {
    if (self.isConnected) return YES;
    
    CFReadStreamRef readStream;
    CFWriteStreamRef writeStream;
    
    // Create streams
    CFStreamCreatePairWithSocketToHost(kCFAllocatorDefault,
                                     (__bridge CFStringRef)self.printerIP,
                                     (UInt32)self.printerPort,
                                     &readStream,
                                     &writeStream);
    
    if (!readStream || !writeStream) {
        return NO;
    }
    
    // Transfer ownership to ARC
    self.inputStream = (__bridge_transfer NSInputStream *)readStream;
    self.outputStream = (__bridge_transfer NSOutputStream *)writeStream;
    
    // Open streams
    [self.inputStream open];
    [self.outputStream open];
    
    // Check stream status
    if ([self.inputStream streamStatus] == NSStreamStatusError ||
        [self.outputStream streamStatus] == NSStreamStatusError) {
        [self disconnectPrinter];
        return NO;
    }
    
    self.isConnected = YES;
    return YES;
}

- (void)disconnectPrinter {
    if (self.inputStream) {
        [self.inputStream close];
        self.inputStream = nil;
    }
    
    if (self.outputStream) {
        [self.outputStream close];
        self.outputStream = nil;
    }
    
    self.isConnected = NO;
}

#pragma mark - Printer Status

- (BOOL)isPrinterOffline {
    if (!self.isConnected) {
        if (![self connectToPrinter]) {
            return YES;
        }
    }
    
    uint8_t commandBytes[] = {16, 4, 1, 16, 4, 2, 16, 4, 3, 16, 4, 4};
    NSData *command = [NSData dataWithBytes:commandBytes length:sizeof(commandBytes)];
    
    if (![self writeData:command]) {
        return YES;
    }
    
    uint8_t buffer[4] = {0};
    NSInteger bytesRead = [self readBytes:buffer length:4];
    
    if (bytesRead != 4) {
        return YES;
    }
    
    // Check if printer is not responding
    if (buffer[0] == 0 && buffer[1] == 0 && buffer[2] == 0 && buffer[3] == 0) {
        monitorPrinter = NO;
        self.status.error = NO;
        self.status.offline = NO;
        
        if (index > 1) {
            PrinterType = 1;
            return NO;
        }
    }
    
    // Update status
    BOOL isOffline = (buffer[0] & 0x08) != 0;
    if (buffer[0] == 0) {
        isOffline = YES;
    }
    
    self.status.offline = isOffline;
    self.status.cashdrawerOpen = (buffer[0] & 0x04) == 0;
    self.status.coverOpen = (buffer[1] & 0x04) != 0;
    self.status.cutterError = (buffer[2] & 0x08) != 0;
    self.status.receiptPaperEmpty = (buffer[3] & 0x60) != 0;
    self.status.receiptPaperNearEmptyInner = (buffer[3] & 0x0C) != 0;
    
    if (!self.status.offline &&
        !self.status.coverOpen &&
        !self.status.cutterError &&
        !self.status.receiptPaperEmpty &&
        !self.status.receiptPaperNearEmptyInner) {
        self.status.error = NO;
    } else {
        self.status.error = YES;
    }
    
    return isOffline;
}

- (void)checkPrinterStatusWithCompletion:(void(^)(BOOL isOffline, PrinterStatus *status, NSError *error))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        __block NSError *error = nil;
        __block BOOL isOffline = YES;  // Default to offline
        
        dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
        
        // Create a separate queue for the status check
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
            if ([self connectToPrinter]) {
                isOffline = [self isPrinterOffline];
            } else {
                error = [NSError errorWithDomain:PrinterControllerErrorDomain
                                          code:PrinterControllerErrorConnectionFailed
                                      userInfo:@{NSLocalizedDescriptionKey: @"Failed to connect to printer"}];
            }
            dispatch_semaphore_signal(semaphore);
        });
        
        // Wait for 0.5 seconds
        dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1 * NSEC_PER_SEC));
        if (dispatch_semaphore_wait(semaphore, timeout) != 0) {
            // Timeout occurred
            isOffline = YES;
            error = [NSError errorWithDomain:PrinterControllerErrorDomain
                                      code:PrinterControllerErrorTimeout
                                  userInfo:@{NSLocalizedDescriptionKey: @"Printer status check timed out"}];
            [self disconnectPrinter];
        }
        
        // Return result on main thread
        dispatch_async(dispatch_get_main_queue(), ^{
            completion(isOffline, self.status, error);
        });
    });
}

#pragma mark - Private Methods

- (BOOL)writeData:(NSData *)data {
    if (!self.isConnected) return NO;
    
    NSInteger bytesWritten = [self.outputStream write:data.bytes maxLength:data.length];
    
    if (bytesWritten == -1) {
        [self disconnectPrinter];
        return NO;
    }
    
    return (bytesWritten == data.length);
}

- (NSInteger)readBytes:(uint8_t *)buffer length:(NSInteger)length {
    if (!self.isConnected) return -1;
    
    NSInteger totalBytesRead = 0;
    NSInteger bytesRead = 0;
    NSInteger attempts = 0;
    const NSInteger maxAttempts = 5;
    const NSTimeInterval timeout = 1.0;
    
    while (totalBytesRead < length && attempts < maxAttempts) {
        bytesRead = [self.inputStream read:buffer + totalBytesRead
                                maxLength:length - totalBytesRead];
        
        if (bytesRead < 0) {
            [self disconnectPrinter];
            return -1;
        } else if (bytesRead == 0) {
            [NSThread sleepForTimeInterval:timeout/maxAttempts];
            attempts++;
        } else {
            totalBytesRead += bytesRead;
            attempts = 0;
        }
    }
    
    return totalBytesRead;
}

@end

@implementation PrinterStatus

@synthesize offline = _offline;
@synthesize error = _error;
@synthesize coverOpen = _coverOpen;
@synthesize cutterError = _cutterError;
@synthesize receiptPaperEmpty = _receiptPaperEmpty;
@synthesize receiptPaperNearEmptyInner = _receiptPaperNearEmptyInner;
@synthesize cashdrawerOpen = _cashdrawerOpen;

- (id)init {
    self = [super init];
    if (self) {
        _offline = YES;
        _error = NO;
        _coverOpen = NO;
        _cutterError = NO;
        _receiptPaperEmpty = NO;
        _receiptPaperNearEmptyInner = NO;
        _cashdrawerOpen = NO;
    }
    return self;
}

@end
