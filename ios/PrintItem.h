// PrintItem.h

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

typedef NS_ENUM(NSInteger, PrintItemType) {
    PrintItemTypeText,
    PrintItemTypeFeed,
    PrintItemTypeCut,
    PrintItemTypeColumn,
    PrintItemTypeImage,
    PrintItemTypeQRCode,
    PrintItemTypeCashBox
};

typedef NS_ENUM(NSInteger, TextAlignment) {
    TextAlignmentLeft,
    TextAlignmentCenter,
    TextAlignmentRight
};

typedef NS_ENUM(NSInteger, FontSize) {
    FontSizeNormal,
    FontSizeWide,
    FontSizeTall,
    FontSizeBig
};

@interface ColumnItem : NSObject

@property (nonatomic, strong) NSString *text;
@property (nonatomic, assign) TextAlignment alignment;
@property (nonatomic, assign) NSInteger width;
@property (nonatomic, strong) NSArray *lines;

/**
 * @brief Initializes a new ColumnItem instance.
 *
 * @param text The text content of the column.
 * @param alignment The text alignment within the column.
 * @param width The width of the column.
 * @param lines An array representing the lines of text in the column.
 * @return A new instance of ColumnItem.
 */

- (instancetype)initWithText:(NSString *)text
                   alignment:(TextAlignment)alignment
                       width:(NSInteger)width
                       lines:(NSArray *)lines;

@end

@interface PrintItem : NSObject

@property (nonatomic, assign) PrintItemType type;
@property (nonatomic, strong) NSString *text;
@property (nonatomic, assign) BOOL bold;
@property (nonatomic, assign) TextAlignment alignment;
@property (nonatomic, assign) int lines;
@property (nonatomic, assign) NSInteger units;
@property (nonatomic, strong) UIImage *bitmapImage;
@property (nonatomic, strong) NSArray<ColumnItem *> *columns;
@property (nonatomic, assign) FontSize fontSize;
@property (nonatomic, assign) BOOL wrapWords;

/**
 * @brief Initializes a new PrintItem instance.
 *
 * @param type The type of the print item.
 * @param text The text content of the print item.
 * @param bold A boolean indicating whether the text should be bold.
 * @param alignment The text alignment.
 * @param lines The number of lines to feed.
 * @param columns An array of ColumnItem objects for column-based printing.
 * @param fontSize The font size to use.
 * @param units The number of units (context-dependent, e.g., for feed or cut operations).
 * @return A new instance of PrintItem.
 */

- (instancetype)initWithType:(PrintItemType)type
                        text:(NSString *)text
                  fontWeight:(BOOL)bold
                   alignment:(TextAlignment)alignment
                   feedLines:(NSInteger)lines
                     columns:(NSArray<ColumnItem *> *)columns
                    fontSize:(FontSize)fontSize
                       units:(NSInteger)units
                   wrapWords:(BOOL)wrapWords;

/**
 * @brief Converts the alignment property to an integer value.
 *
 * @return An integer representing the alignment (0 for left, 1 for center, 2 for right).
 */
- (NSInteger)getAlignmentAsInt;

/**
 * @brief Sets the bitmap image for the print item.
 *
 * @param bitmapImage The UIImage to be set as the bitmap.
 */
- (void)setBitmap:(UIImage *)bitmapImage;

- (void)setWrapWords:(BOOL)wrapWords;

/**
 * @brief Retrieves the number of units for the print item.
 *
 * @return The number of units as an NSInteger.
 */
- (NSInteger)getUnits;

@end
