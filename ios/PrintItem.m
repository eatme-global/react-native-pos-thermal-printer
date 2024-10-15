// PrintItem.m

#import "PrintItem.h"

@implementation ColumnItem

- (instancetype)initWithText:(NSString *)text
                   alignment:(TextAlignment)alignment
                       width:(NSInteger)width
                       lines:(NSArray *)lines {
    self = [super init];
    if (self) {
        _text = text;
        _alignment = alignment;
        _width = width;
        _lines = lines;
    }
    return self;
}

@end

@implementation PrintItem

- (instancetype)initWithType:(PrintItemType)type
                        text:(NSString *)text
                  fontWeight:(BOOL)bold
                   alignment:(TextAlignment)alignment
                   feedLines:(NSInteger)lines
                     columns:(NSArray<ColumnItem *> *)columns
                    fontSize:(FontSize)fontSize
                       units:(NSInteger)units{
    self = [super init];
    if (self) {
        _type = type;
        _text = text;
        _bold = bold;
        _alignment = alignment;
        _lines = lines;
        _columns = columns;
        _fontSize = fontSize;
        _units = units;
    }
    return self;
}

- (NSInteger)getAlignmentAsInt {
    switch (self.alignment) {
        case TextAlignmentLeft:
            return 0;
        case TextAlignmentCenter:
            return 1;
        case TextAlignmentRight:
            return 2;
        default:
            return 0;
    }
}

- (void)setBitmap:(UIImage *)bitmapImage {
    self.bitmapImage = bitmapImage;
}

- (NSInteger)getUnits {
    return _units;
}

@end
