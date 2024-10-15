#import "PrinterJob.h"
#import "PrintItem.h"

@implementation PrinterJob

// Custom initializer method implementation
- (instancetype)initWithPrinterIp:(NSString *)printerIp jobContent:(NSArray<PrintItem *> *)content printerName:(NSString *)printerName metadata:(NSString *)metadata jobId:(NSString *)jobId {
    self = [super init];
    if (self) {
        // Assign the passed-in arguments to the instance's properties
        self.targetPrinterIp = printerIp;
        self.jobContent = content;
        self.printerName = printerName;
        self.metadata = metadata;
        self.jobId = jobId;
    }
    return self;
}

@end

