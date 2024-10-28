#import <Foundation/Foundation.h>
#import "PrintItem.h"

@interface PrintItemProcessor : NSObject

/**
 * @brief Creates a PrintItem object from a dictionary representation.
 * @param itemDict A dictionary containing the print item's properties.
 * @return A PrintItem object, or nil if the dictionary is invalid.
 */
+ (PrintItem *)createPrintItemFromDictionary:(NSDictionary *)itemDict;


@end
