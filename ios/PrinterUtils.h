#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "PrintItem.h"

@interface PrinterUtils : NSObject

/**
 * @brief Generates a unique job ID for print jobs.
 * @return A string containing a unique job ID.
 */
+ (NSString *)generateUniqueJobId;

/**
 * @brief Generates a unique job ID for print jobs.
 * @return A string containing a unique job ID.
 */
+ (UIImage *)resizeImage:(UIImage *)image toWidth:(CGFloat)width;

/**
 * @brief Aligns an image within the printer's width based on the specified alignment.
 * @param originalImage The original UIImage to align.
 * @param alignment The desired TextAlignment for the image.
 * @return A new UIImage aligned according to the specified alignment.
 */
+ (UIImage *)alignImage:(UIImage *)originalImage alignment:(TextAlignment)alignment printerWidth:(CGFloat)printerWidth;

/**
 * @brief Splits text into lines based on a specified width.
 * @param text The text to split into lines.
 * @param width The maximum width of each line.
 * @param wrapWords Whether to wrap words or split them.
 * @return An array of strings, each representing a line of text.
 */
+ (NSArray<NSString *> *)splitTextIntoLines:(NSString *)text width:(NSInteger)width wrapWords:(BOOL)wrapWords;

/**
 * @brief Checks if the given text contains any Chinese characters.
 * @param text The text to check for Chinese characters.
 * @return YES if the text contains Chinese characters, NO otherwise.
 */
+ (BOOL)containsChineseCharacter:(NSString *)text;

/**
 * @brief Checks if any of the columns contain Chinese characters.
 * @param columns An array of ColumnItem objects to check.
 * @return YES if any column contains Chinese characters, NO otherwise.
 */
+ (BOOL)columnsContainChineseCharacters:(NSArray<ColumnItem *> *)columns;

/**
 * @brief Pads text to a specified width and aligns it according to the given alignment.
 * @param text The text to pad and align.
 * @param width The desired width of the padded text.
 * @param alignment The desired TextAlignment for the text.
 * @return A string padded and aligned to the specified width.
 */
+ (NSString *)padText:(NSString *)text width:(NSInteger)width alignment:(TextAlignment)alignment;

/**
 * @brief Calculates the visual width of a string, accounting for double-width characters.
 * @param str The string to calculate the visual width for.
 * @return The visual width of the string.
 */
+ (NSInteger)getVisualWidth:(NSString *)str;

@end
