#import "PrinterUtils.h"

@implementation PrinterUtils

+ (NSString *)generateUniqueJobId {
    NSUUID *uuid = [NSUUID UUID];
    NSString *uuidString = [uuid UUIDString];
    
    NSDate *now = [NSDate date];
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"yyyyMMddHHmmss"];
    [formatter setTimeZone:[NSTimeZone systemTimeZone]];
    NSString *timestamp = [formatter stringFromDate:now];
    
    return [NSString stringWithFormat:@"PJ-%@-%@", timestamp, [uuidString substringToIndex:8]];
}

+ (UIImage *)resizeImage:(UIImage *)image toWidth:(CGFloat)width {
    CGSize originalSize = image.size;
    CGFloat aspectRatio = originalSize.height / originalSize.width;
    CGSize newSize = CGSizeMake(width, width * aspectRatio);
    
    UIGraphicsBeginImageContextWithOptions(newSize, NO, 0.0);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *resizedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return resizedImage;
}

+ (UIImage *)alignImage:(UIImage *)originalImage alignment:(TextAlignment)alignment printerWidth:(CGFloat)printerWidth {
    CGSize printerSize = CGSizeMake(printerWidth, originalImage.size.height);
    
    UIGraphicsBeginImageContextWithOptions(printerSize, YES, 0.0);
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    [[UIColor whiteColor] setFill];
    CGContextFillRect(context, CGRectMake(0, 0, printerSize.width, printerSize.height));
    
    CGFloat imageWidth = MIN(originalImage.size.width, printerWidth);
    CGFloat left = 0;
    
    switch (alignment) {
        case TextAlignmentCenter:
            left = (printerWidth - imageWidth) / 2.0;
            break;
        case TextAlignmentRight:
            left = printerWidth - imageWidth;
            break;
        default: // TextAlignmentLeft
            left = 0;
            break;
    }
    
    [originalImage drawInRect:CGRectMake(left, 0, imageWidth, originalImage.size.height)];
    
    UIImage *alignedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return alignedImage;
}

+ (NSArray<NSString *> *)splitTextIntoLines:(NSString *)text width:(NSInteger)width wrapWords:(BOOL)wrapWords {
    if (wrapWords) {
        return [self splitTextIntoLinesWithWordWrap:text width:width];
    } else {
        return [self splitTextIntoLinesWithoutWordWrap:text width:width];
    }
}

+ (NSArray<NSString *> *)splitTextIntoLinesWithWordWrap:(NSString *)text width:(NSInteger)width {
    NSMutableArray<NSString *> *lines = [NSMutableArray array];
    NSArray<NSString *> *words = [text componentsSeparatedByCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
    NSMutableString *currentLine = [NSMutableString string];
    NSInteger currentLineWidth = 0;

    for (NSString *word in words) {
        NSInteger wordWidth = [self getVisualWidth:word];

        if (wordWidth > width) {
            if (currentLineWidth > 0) {
                [lines addObject:[currentLine stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]]];
                currentLine = [NSMutableString string];
                currentLineWidth = 0;
            }

            NSString *remainingWord = word;
            while (remainingWord.length > 0) {
                NSString *chunk = [self takeChunkOfVisualWidth:remainingWord maxWidth:width];
                [lines addObject:chunk];
                remainingWord = [remainingWord substringFromIndex:chunk.length];
            }
        } else if (currentLineWidth + wordWidth + (currentLineWidth > 0 ? 1 : 0) <= width) {
            if (currentLineWidth > 0) {
                [currentLine appendString:@" "];
                currentLineWidth++;
            }
            [currentLine appendString:word];
            currentLineWidth += wordWidth;
        } else {
            [lines addObject:[currentLine stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]]];
            currentLine = [NSMutableString stringWithString:word];
            currentLineWidth = wordWidth;
        }
    }

    if (currentLine.length > 0) {
        [lines addObject:[currentLine stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]]];
    }

    return lines;
}

//+ (NSArray<NSString *> *)splitTextIntoLinesWithoutWordWrap:(NSString *)text width:(NSInteger)width {
//    NSMutableArray<NSString *> *lines = [NSMutableArray array];
//    for (NSInteger i = 0; i < text.length; i += width) {
//        NSInteger end = MIN(i + width, text.length);
//        [lines addObject:[text substringWithRange:NSMakeRange(i, end - i)]];
//    }
//    return lines;
//}


+ (NSArray<NSString *> *)splitTextIntoLinesWithoutWordWrap:(NSString *)text width:(NSInteger)width {
    NSMutableArray<NSString *> *lines = [NSMutableArray array];
    NSMutableString *currentLine = [NSMutableString string];
    NSInteger currentLineWidth = 0;

    for (NSUInteger i = 0; i < text.length; i++) {
        unichar character = [text characterAtIndex:i];
        NSInteger charWidth = [self isHanCharacter:character] ? 2 : 1;

        if (currentLineWidth + charWidth > width) {
            [lines addObject:[currentLine copy]];
            [currentLine setString:@""];
            currentLineWidth = 0;
        }

        [currentLine appendString:[NSString stringWithCharacters:&character length:1]];
        currentLineWidth += charWidth;
    }

    if (currentLine.length > 0) {
        [lines addObject:[currentLine copy]];
    }

    return lines;
}

+ (NSString *)takeChunkOfVisualWidth:(NSString *)str maxWidth:(NSInteger)maxWidth {
    NSInteger currentWidth = 0;
    NSInteger endIndex = 0;
    
    for (NSInteger i = 0; i < str.length; i++) {
        unichar c = [str characterAtIndex:i];
        NSInteger charWidth = [self isHanCharacter:c] ? 2 : 1;
        if (currentWidth + charWidth > maxWidth) break;
        currentWidth += charWidth;
        endIndex = i + 1;
    }
    
    return [str substringToIndex:endIndex];
}

+ (BOOL)containsChineseCharacter:(NSString *)text {
    NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:@"\\p{Han}" options:0 error:nil];
    NSRange range = NSMakeRange(0, text.length);
    NSTextCheckingResult *match = [regex firstMatchInString:text options:0 range:range];
    return (match != nil);
}

+ (BOOL)columnsContainChineseCharacters:(NSArray<ColumnItem *> *)columns {
    for (ColumnItem *column in columns) {
        if (column.lines != nil) {
            for (NSString *line in column.lines) {
                if ([self containsChineseCharacter:line]) {
                    return YES;
                }
            }
        }
    }
    return NO;
}

+ (NSString *)padText:(NSString *)text width:(NSInteger)width alignment:(TextAlignment)alignment {
    NSInteger visualWidth = [self getVisualWidth:text];
    
    if (visualWidth <= width) {
        NSString *padding = [@"" stringByPaddingToLength:width - visualWidth withString:@" " startingAtIndex:0];
        switch (alignment) {
            case TextAlignmentLeft:
                return [text stringByAppendingString:padding];
            case TextAlignmentRight:
                return [padding stringByAppendingString:text];
            case TextAlignmentCenter: {
                NSInteger leftPad = padding.length / 2;
                NSInteger rightPad = padding.length - leftPad;
                NSString *leftPadding = [@"" stringByPaddingToLength:leftPad withString:@" " startingAtIndex:0];
                NSString *rightPadding = [@"" stringByPaddingToLength:rightPad withString:@" " startingAtIndex:0];
                return [NSString stringWithFormat:@"%@%@%@", leftPadding, text, rightPadding];
            }
        }
    } else {
        // Text overflow handling
        switch (alignment) {
            case TextAlignmentLeft:
                return [self truncateToVisualWidth:text maxWidth:width];
            case TextAlignmentRight:
                return [text substringFromIndex:MAX(0, text.length - width)];
            case TextAlignmentCenter: {
                NSInteger startIndex = (text.length - width) / 2;
                return [text substringWithRange:NSMakeRange(startIndex, MIN(width, text.length - startIndex))];
            }
        }
    }
    
    return text;
}

+ (NSString *)truncateToVisualWidth:(NSString *)text maxWidth:(NSInteger)maxWidth {
    NSInteger currentWidth = 0;
    NSInteger endIndex = 0;
    for (NSInteger i = 0; i < text.length; i++) {
        unichar c = [text characterAtIndex:i];
        NSInteger charWidth = [self isHanCharacter:c] ? 2 : 1;
        if (currentWidth + charWidth > maxWidth) break;
        currentWidth += charWidth;
        endIndex = i + 1;
    }
    return [text substringToIndex:endIndex];
}

+ (NSInteger)getVisualWidth:(NSString *)str {
    NSInteger visualWidth = 0;
    
    for (NSInteger i = 0; i < str.length; i++) {
        unichar c = [str characterAtIndex:i];
        visualWidth += [self isHanCharacter:c] ? 2 : 1;
    }
    
    return visualWidth;
}

+ (BOOL)isHanCharacter:(unichar)character {
    return (character >= 0x4E00 && character <= 0x9FFF);
}

@end
