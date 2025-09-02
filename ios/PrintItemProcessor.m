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
  NSInteger imageWidth = 280;
  NSInteger maxHeight = 130;
  
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
  UIImage *resizedImage = [PrinterUtils resizeImage:image
                                           maxWidth:imageWidth
                                          maxHeight:maxHeight];
  
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
                                                   units:units
                                               wrapWords:false];
  
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
  NSInteger units = [item[@"unit"] integerValue] ?: 6;
  BOOL wrapWords = [item[@"wrapWords"] boolValue];
  
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
                                   units:units
                               wrapWords:wrapWords];
  
}

+ (PrintItem *)createColumnPrintItem:(NSDictionary *)item fontWeight:(BOOL)fontWeight fontSize:(FontSize)fontSize {
  NSArray *columnArray = [item objectForKey:@"columns"];
  NSMutableArray<ColumnItem *> *columns = [NSMutableArray array];
  
  if (columnArray) {
    for (NSDictionary *columnItem in columnArray) {
      NSString *columnText = columnItem[@"text"] ?: @"";
      
      NSString *processedColumnText = [PrinterUtils sanitizeStringForPrinter:columnText];
      
      NSInteger columnWidth = [columnItem[@"width"] integerValue] ?: 10;
      BOOL wrapWordsColumn = [columnItem[@"wrapWords"] boolValue] ?: false;
      
      TextAlignment columnAlignment = [HelperFunctions parseAlignment:columnItem[@"alignment"]];
      NSArray<NSString *> *lines = [PrinterUtils splitTextIntoLines:processedColumnText width:columnWidth wrapWords:wrapWordsColumn];
      
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
                                   units:0
                               wrapWords:false];
}
@end
