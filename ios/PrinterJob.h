#import "PrintItem.h"

@interface PrinterJob : NSObject

/**
 * @property targetPrinterIp
 * @brief The IP address of the target printer.
 */
@property (nonatomic, strong) NSString *targetPrinterIp;

/**
 * @property printerName
 * @brief The name of the printer.
 */
@property (nonatomic, strong) NSString *printerName;

/**
 * @property jobId
 * @brief A unique identifier for the print job.
 */
@property (nonatomic, strong) NSString *jobId;

/**
 * @property jobContent
 * @brief An array of PrintItem objects representing the content to be printed.
 */
@property (nonatomic, strong) NSArray<PrintItem *> *jobContent;

/**
 * @property pending
 * @brief Indicates whether the print job is pending or not.
 */
@property (nonatomic, assign) BOOL pending;

/**
 * @property metadata
 * @brief Additional metadata associated with the print job.
 */
@property (nonatomic, strong) NSString *metadata;

/**
 * @brief Initializes a new PrinterJob instance.
 * @param printerIp The IP address of the target printer.
 * @param content An array of PrintItem objects representing the job content.
 * @param printerName The name of the printer.
 * @param metadata Additional metadata for the job.
 * @param jobId A unique identifier for the job.
 * @return An initialized PrinterJob instance.
 */
- (instancetype)initWithPrinterIp:(NSString *)printerIp jobContent:(NSArray<PrintItem *> *)content printerName:(NSString *)printerName metadata:(NSString *)metadata jobId:(NSString *)jobId;

@end
