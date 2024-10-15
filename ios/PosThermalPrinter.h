
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNPosThermalPrinterSpec.h"

@interface PosThermalPrinter : NSObject <NativePosThermalPrinterSpec>
#else
#import <React/RCTBridgeModule.h>

@interface PosThermalPrinter : NSObject <RCTBridgeModule>
#endif

@end
