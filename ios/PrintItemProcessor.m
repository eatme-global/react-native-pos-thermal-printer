#import "PrintItemProcessor.h"
#import "PosCommand.h"
#import "HelperFunctions.h"
#import "PrinterUtils.h"

@implementation PrintItemProcessor

+ (PrintItem *)createPrintItemFromDictionary:(NSDictionary *)itemDict {
    NSString *type = itemDict[@"type"];
    
    if ([type isEqualToString:@"IMAGE"]) {
        return [self createImagePrintItem:itemDict];
    } else {
        return [self createNonImagePrintItem:itemDict];
    }
}

+ (PrintItem *)createImagePrintItem:(NSDictionary *)item {
    NSString *imageUrl = item[@"url"] ?: @"";
    
    NSInteger maxWidth = 280;
    NSInteger defaultWidth = 280;
    NSInteger minWidth = 0;
    NSInteger imageWidth = 100;
       
    NSInteger width = [item[@"width"] integerValue] ?: defaultWidth;
    BOOL fullWidth = [item[@"fullWidth"] boolValue];
    CGFloat printerWidth = [item[@"printerWidth"] floatValue] ?: 290.0;


    if (fullWidth) {
       imageWidth = maxWidth;
    }else {
        // Ensure width is within 0-100 range
        width = MAX(minWidth, MIN(width, defaultWidth));
        imageWidth = MAX(minWidth, MIN(width, defaultWidth));
    }

    NSURL *url = [NSURL URLWithString:imageUrl];
    NSData *imageData = [NSData dataWithContentsOfURL:url];
    
    if (imageData == nil) {
        NSLog(@"Failed to download image from URL: %@", imageUrl);
        return nil;
    }
    
    
    UIImage *image = [UIImage imageWithData:imageData];
    UIImage *resizedImage = [PrinterUtils resizeImage:image toWidth:imageWidth];
    
    if (resizedImage == nil) {
        NSLog(@"Failed to resize image from URL: %@", imageUrl);
        return nil;
    }
    
    BOOL fontWeight = [item[@"bold"] boolValue];
    TextAlignment alignment = [HelperFunctions parseAlignment:item[@"alignment"] ?: @"LEFT"];
    NSInteger feedLines = [item[@"lines"] integerValue] ?: 0;
    FontSize fontSize = [HelperFunctions parseFontSize:item[@"fontSize"] ?: @"NORMAL"];
    NSInteger units = [item[@"unit"] integerValue] ?: 0;
    
    UIImage *alignedImage = [PrinterUtils alignImage:resizedImage alignment:alignment printerWidth:printerWidth];
    
    PrintItem *printItem = [[PrintItem alloc] initWithType:PrintItemTypeImage
                                                      text:imageUrl
                                                fontWeight:fontWeight
                                                 alignment:alignment
                                                 feedLines:feedLines
                                                   columns:[NSArray array]
                                                  fontSize:fontSize
                                                     units:units];
    
    [printItem setBitmap:alignedImage];
    return printItem;
}

+ (PrintItem *)createNonImagePrintItem:(NSDictionary *)item {
    NSString *type = item[@"type"];
    NSString *text = item[@"text"] ?: @"";
    BOOL fontWeight = [item[@"bold"] boolValue];
    TextAlignment alignment = [HelperFunctions parseAlignment:item[@"alignment"] ?: @"LEFT"];
    NSInteger feedLines = [item[@"lines"] integerValue] ?: 0;
    FontSize fontSize = [HelperFunctions parseFontSize:item[@"fontSize"] ?: @"NORMAL"];
    NSInteger units = [item[@"unit"] integerValue] ?: 0;
    
    PrintItemType printItemType;
    
    if ([type isEqualToString:@"TEXT"]) {
        printItemType = PrintItemTypeText;
    } else if ([type isEqualToString:@"QRCODE"]) {
        printItemType = PrintItemTypeQRCode;
    } else if ([type isEqualToString:@"FEED"]) {
        printItemType = PrintItemTypeFeed;
    } else if ([type isEqualToString:@"CUT"]) {
        printItemType = PrintItemTypeCut;
    } else if ([type isEqualToString:@"CASHBOX"]) {
        printItemType = PrintItemTypeCashBox;
    } else if ([type isEqualToString:@"COLUMN"]) {
        return [self createColumnPrintItem:item fontWeight:fontWeight fontSize:fontSize];
    } else {
        NSLog(@"Unknown print item type: %@", type);
        return nil;
    }
    
    return [[PrintItem alloc] initWithType:printItemType
                                      text:text
                                fontWeight:fontWeight
                                 alignment:alignment
                                 feedLines:feedLines
                                   columns:[NSArray array]
                                  fontSize:fontSize
                                     units:units];
}

+ (PrintItem *)createColumnPrintItem:(NSDictionary *)item fontWeight:(BOOL)fontWeight fontSize:(FontSize)fontSize {
    NSArray *columnArray = [item objectForKey:@"columns"];
    NSMutableArray<ColumnItem *> *columns = [NSMutableArray array];
    
    if (columnArray) {
        for (NSDictionary *columnItem in columnArray) {
            NSString *columnText = columnItem[@"text"] ?: @"";
            NSInteger columnWidth = [columnItem[@"width"] integerValue] ?: 10;
            BOOL wrapWordsColumn = [columnItem[@"wrapWords"] boolValue] ?: false;
            
            TextAlignment columnAlignment = [HelperFunctions parseAlignment:columnItem[@"alignment"]];
            NSArray<NSString *> *lines = [PrinterUtils splitTextIntoLines:columnText width:columnWidth wrapWords:wrapWordsColumn];
            
            ColumnItem *columnItemNew = [[ColumnItem alloc] initWithText:columnText alignment:columnAlignment width:columnWidth lines:lines];
            
            [columns addObject:columnItemNew];
        }
    }
    
    return [[PrintItem alloc] initWithType:PrintItemTypeColumn
                                      text:@""
                                fontWeight:fontWeight
                                 alignment:TextAlignmentLeft
                                 feedLines:0
                                   columns:columns
                                  fontSize:fontSize
                                     units:0];
}

+ (void)processItems:(NSArray<PrintItem *> *)items completion:(void (^)(NSData *printData))completion {
    NSMutableData *dataM = [NSMutableData dataWithData:[PosCommand initializePrinter]];
    
    for (PrintItem *item in items) {
        [self processItem:item intoData:dataM];
    }
    
    completion(dataM);
}

+ (void)processItem:(PrintItem *)item intoData:(NSMutableData *)data {
    switch (item.fontSize) {
        case FontSizeNormal:
            [data appendData:[HelperFunctions selectFont:1 width:1]];
            break;
        case FontSizeWide:
            [data appendData:[HelperFunctions selectFont:1 width:2]];
            break;
        case FontSizeTall:
            [data appendData:[HelperFunctions selectFont:2 width:1]];
            break;
        case FontSizeBig:
            [data appendData:[HelperFunctions selectFont:2 width:2]];
            break;
    }
    
    switch (item.type) {
        case PrintItemTypeText:
            [self addTextToPrintData:data item:item];
            break;
        case PrintItemTypeFeed:
            [data appendData:[PosCommand printAndFeedForwardWhitN:item.lines]];
            break;
        case PrintItemTypeImage:
            [data appendData:[PosCommand printRasteBmpWithM:RasterNolmorWH andImage:item.bitmapImage andType:Dithering]];
            break;
        case PrintItemTypeColumn:
            [self addColumnToPrintData:data item:item];
            break;
        case PrintItemTypeQRCode:
            [self addQRCodeToPrintData:data item:item];
            break;
        case PrintItemTypeCashBox:
            [data appendData:[PosCommand openCashBoxRealTimeWithM:0 andT:2]];
            break;
        case PrintItemTypeCut:
            [data appendData:[PosCommand selectCutPageModelAndCutpage:0]];
            break;
    }
}

+ (void)addTextToPrintData:(NSMutableData *)data item:(PrintItem *)item {
    NSStringEncoding encodeCharset = [PrinterUtils containsChineseCharacter:item.text] ?
        CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000) :
        CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingDOSLatinUS);
    
    [data appendData:[PosCommand selectAlignment:item.getAlignmentAsInt]];
    
    if (item.bold) {
        [data appendData:[PosCommand selectOrCancleBoldModel:1]];
    }
    
    NSData *textData = [item.text dataUsingEncoding:encodeCharset];
    [data appendData:textData];
    
    if (item.bold) {
        [data appendData:[PosCommand selectOrCancleBoldModel:0]];
    }
    
    [data appendData:[PosCommand printAndFeedLine]];
}

+ (void)addColumnToPrintData:(NSMutableData *)data item:(PrintItem *)item {
    if (item.columns) {
        NSStringEncoding encodeCharset = [PrinterUtils columnsContainChineseCharacters:item.columns] ?
            CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000) :
            CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingDOSLatinUS);
        
        NSInteger maxLines = [[item.columns valueForKeyPath:@"@max.lines.@count"] integerValue];
        
        if (item.bold) {
            [data appendData:[PosCommand selectOrCancleBoldModel:1]];
        }
        
        for (NSInteger lineIndex = 0; lineIndex < maxLines; lineIndex++) {
            NSMutableString *lineBuilder = [NSMutableString string];
            
            for (ColumnItem *column in item.columns) {
                NSString *line = (lineIndex < column.lines.count) ? column.lines[lineIndex] : @"";
                NSString *formattedText = [PrinterUtils padText:line width:column.width alignment:column.alignment];
                [lineBuilder appendString:formattedText];
            }
            
            NSData *lineData = [lineBuilder dataUsingEncoding:encodeCharset];
            [data appendData:lineData];
        }
        
        if (item.bold) {
            [data appendData:[PosCommand selectOrCancleBoldModel:0]];
        }
    }
}

+ (void)addQRCodeToPrintData:(NSMutableData *)data item:(PrintItem *)item {
    [data appendData:[PosCommand selectAlignment:item.getAlignmentAsInt]];
    [data appendData:[PosCommand setQRcodeUnitsize:item.getUnits]];
    [data appendData:[PosCommand setErrorCorrectionLevelForQrcode:48]];
    [data appendData:[PosCommand sendDataToStoreAreaWitQrcodeConent:item.text usEnCoding:CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000)]];
    [data appendData:[PosCommand printTheQRcodeInStore]];
}

@end
