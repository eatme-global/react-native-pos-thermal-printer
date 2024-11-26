// PrinterController.h

#import <Foundation/Foundation.h>

// PrinterStatus class declaration
@interface PrinterStatus : NSObject

@property (nonatomic, assign) BOOL offline;
@property (nonatomic, assign) BOOL error;
@property (nonatomic, assign) BOOL coverOpen;
@property (nonatomic, assign) BOOL cutterError;
@property (nonatomic, assign) BOOL receiptPaperEmpty;
@property (nonatomic, assign) BOOL receiptPaperNearEmptyInner;
@property (nonatomic, assign) BOOL cashdrawerOpen;

@end

// PrinterController class declaration
@interface PrinterController : NSObject

// Properties
@property (nonatomic, strong, readonly) PrinterStatus *status;
@property (nonatomic, assign, readonly) BOOL isConnected;
@property (nonatomic, copy, readonly) NSString *printerIP;
@property (nonatomic, assign, readonly) NSInteger printerPort;

// Initialization
- (id)initWithIP:(NSString *)ip port:(NSInteger)port;

// Main functionality
- (BOOL)connectToPrinter;
- (void)disconnectPrinter;
- (BOOL)isPrinterOffline;

// Optional: Status check with completion handler
- (void)checkPrinterStatusWithCompletion:(void(^)(BOOL isOffline, PrinterStatus *status, NSError *error))completion;




@end

// Error domain and codes for printer operations
extern NSString * const PrinterControllerErrorDomain;

typedef NS_ENUM(NSInteger, PrinterControllerErrorCode) {
    PrinterControllerErrorConnectionFailed = 1000,
    PrinterControllerErrorWriteFailed,
    PrinterControllerErrorReadFailed,
    PrinterControllerErrorTimeout
};
