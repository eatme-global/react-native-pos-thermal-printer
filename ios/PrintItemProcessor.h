#import <Foundation/Foundation.h>
#import "PrintItem.h"

@interface PrintItemProcessor : NSObject

/**
 * @brief Creates a PrintItem object from a dictionary representation.
 * @param itemDict A dictionary containing the print item's properties.
 * @return A PrintItem object, or nil if the dictionary is invalid.
 */
+ (PrintItem *)createPrintItemFromDictionary:(NSDictionary *)itemDict;

/**
 * @brief Processes an array of PrintItem objects into printer-ready data.
 * @param items An array of PrintItem objects to process.
 * @param completion A block to be called with the resulting NSData object containing the printer-ready data.
 */
+ (void)processItems:(NSArray<PrintItem *> *)items completion:(void (^)(NSData *printData))completion;

@end
