#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface PosThermalPrinter :RCTEventEmitter <RCTBridgeModule>

- (void)sendPrinterUnreachableEvent:(NSString *)printerIp;

@end
