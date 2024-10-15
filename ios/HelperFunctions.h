#import "PrintItem.h"

@interface HelperFunctions : NSObject

/**
 * @brief Splits a given text into lines based on a specified width.
 *
 * @param text The input text to be split.
 * @param width The maximum width of each line.
 * @return An array of strings, each representing a line of text.
 */
+ (NSArray<NSString *> *)splitTextIntoLines:(NSString *)text width:(NSInteger)width;

/**
 * @brief Parses a string representation of text alignment into a TextAlignment enum value.
 *
 * @param alignment A string representing the desired alignment ("CENTER", "RIGHT", or any other value for left alignment).
 * @return The corresponding TextAlignment enum value.
 */
+ (TextAlignment)parseAlignment:(NSString *)alignment;

/**
 * @brief Parses a string representation of font size into a FontSize enum value.
 *
 * @param fontSize A string representing the desired font size ("WIDE", "TALL", "BIG", or any other value for normal size).
 * @return The corresponding FontSize enum value.
 */
+ (FontSize)parseFontSize:(NSString *)fontSize;

/**
 * @brief Generates font selection command data based on height and width multipliers.
 *
 * @param heightMultiplier The multiplier for font height (1-8).
 * @param widthMultiplier The multiplier for font width (1-8).
 * @return NSData object containing the font selection command.
 */
+ (NSData *)selectFont:(int)heightMultiplier width:(int)widthMultiplier;

/**
 * @brief Asynchronously fetches an image from a given URL and converts it to PNG data.
 *
 * @param urlString The URL of the image to fetch.
 * @param completion A block to be executed when the operation completes.
 *                   It takes two parameters:
 *                   - imageData: The fetched image data in PNG format, or nil if an error occurred.
 *                   - error: An error object if an error occurred, or nil if the operation was successful.
 */
+ (void)fetchImageFromURL:(NSString *)urlString
               completion:(void (^)(NSData *imageData, NSError *error))completion;

@end
