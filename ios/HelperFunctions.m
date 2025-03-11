// HelperFunctions.m
#import "HelperFunctions.h"
#import "PrintItem.h"
#import "PosCommand.h"


@implementation HelperFunctions

// Placeholder comment


+ (NSArray<NSString *> *)splitTextIntoLines:(NSString *)text width:(NSInteger)width {
  
  
  NSArray *words = [text componentsSeparatedByString:@" "];
  NSMutableArray *lines = [NSMutableArray array];
  NSMutableString *currentLine = [NSMutableString string];
  
  for (NSString *word in words) {
    if ([currentLine length] + [word length] + 1 <= width) {
      if ([currentLine length] > 0) {
        [currentLine appendString:@" "];
      }
      [currentLine appendString:word];
    } else {
      if ([currentLine length] > 0) {
        [lines addObject:[currentLine copy]];
        [currentLine setString:@""];
      }
      if ([word length] > width) {
        // If a single word is longer than the width, split it
        NSString *remaining = word;
        while ([remaining length] > 0) {
          [lines addObject:[remaining substringToIndex:MIN(width, [remaining length])]];
          remaining = [remaining substringFromIndex:MIN(width, [remaining length])];
        }
      } else {
        [currentLine appendString:word];
      }
    }
  }
  if ([currentLine length] > 0) {
    [lines addObject:[currentLine copy]];
  }
  return [lines copy];
  
}

+ (TextAlignment)parseAlignment:(NSString *)alignment {
  if ([alignment isEqualToString:@"CENTER"]) {
    return TextAlignmentCenter;
  } else if ([alignment isEqualToString:@"RIGHT"]) {
    return TextAlignmentRight;
  } else {
    return TextAlignmentLeft;
  }
}

+ (FontSize)parseFontSize:(NSString *)fontSize {
  if ([fontSize isEqualToString:@"WIDE"]) {
    return FontSizeWide;
  } else if ([fontSize isEqualToString:@"TALL"]) {
    return FontSizeTall;
  } else if ([fontSize isEqualToString:@"BIG"]) {
    return FontSizeBig;
  } else {
    return FontSizeNormal;
  }
}


+ (NSData *)selectFont:(int)heightMultiplier width:(int)widthMultiplier {
  // Clamp the multipliers to the range 1-8
  widthMultiplier = MAX(1, MIN(widthMultiplier, 8));
  heightMultiplier = MAX(1, MIN(heightMultiplier, 8));
  
  // Calculate n as per the Kotlin function
  int n = (heightMultiplier - 1) | ((widthMultiplier - 1) << 4);
  
  Byte data[3] = {29, 33, (Byte)n};
  return [NSData dataWithBytes:data length:sizeof(data)];
}

+ (void)fetchImageFromURL:(NSString *)urlString
               completion:(void (^)(NSData *imageData, NSError *error))completion {
  
  NSURL *url = [NSURL URLWithString:urlString];
  if (!url) {
    NSError *error = [NSError errorWithDomain:@"ImageFetcherErrorDomain"
                                         code:1001
                                     userInfo:@{NSLocalizedDescriptionKey: @"Invalid URL"}];
    completion(nil, error);
    return;
  }
  
  NSURLSession *session = [NSURLSession sharedSession];
  NSURLSessionDataTask *dataTask = [session dataTaskWithURL:url
                                          completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
    
    if (error) {
      dispatch_async(dispatch_get_main_queue(), ^{
        completion(nil, error);
      });
      return;
    }
    
    NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
    if (httpResponse.statusCode != 200) {
      NSError *statusError = [NSError errorWithDomain:@"ImageFetcherErrorDomain"
                                                 code:httpResponse.statusCode
                                             userInfo:@{NSLocalizedDescriptionKey: @"HTTP Error"}];
      dispatch_async(dispatch_get_main_queue(), ^{
        completion(nil, statusError);
      });
      return;
    }
    
    UIImage *image = [UIImage imageWithData:data];
    if (!image) {
      NSError *imageError = [NSError errorWithDomain:@"ImageFetcherErrorDomain"
                                                code:1002
                                            userInfo:@{NSLocalizedDescriptionKey: @"Failed to create image from data"}];
      dispatch_async(dispatch_get_main_queue(), ^{
        completion(nil, imageError);
      });
      return;
    }
    
    // Convert image to PNG data
    NSData *pngData = UIImagePNGRepresentation(image);
    if (!pngData) {
      NSError *conversionError = [NSError errorWithDomain:@"ImageFetcherErrorDomain"
                                                     code:1003
                                                 userInfo:@{NSLocalizedDescriptionKey: @"Failed to convert image to PNG data"}];
      dispatch_async(dispatch_get_main_queue(), ^{
        completion(nil, conversionError);
      });
      return;
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
      completion(pngData, nil);
    });
    
    
  }];
  
  [dataTask resume];
}



@end
