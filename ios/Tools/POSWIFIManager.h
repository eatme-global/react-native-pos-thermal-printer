//
//  PosWIFIManager.h
//  Printer
//
//  Created by ding on 2022/12/23
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
@class POSWIFIManager;
typedef void(^POSWIFIBlock)(BOOL isConnect);
typedef void(^POSWIFICallBackBlock)(NSData *data);



enum {
    SocketOfflineByServer,// 服务器掉线，默认为0
    SocketOfflineByUser,  // 用户主动cut
};

/**
 *Connect multiple devices.
 *Use POSWIFIManager *manager = [[POSWIFIManager alloc] init] to instantiate the object,
 *Save the manager and specify the corresponding manager when sending the command to send the command
 */
@protocol POSWIFIManagerDelegate <NSObject>

/// Successfully connected to the host
/// @param managerWiFi connection object management
/// @param host ip address
/// @param port The port number
- (void)POSWIFIManager:(POSWIFIManager *)manager didConnectedToHost:(NSString *)host port:(UInt16)port;
/// Disconnect
/// @param manager WiFi connection object management
/// @param error error
- (void)POSWIFIManager:(POSWIFIManager *)manager willDisconnectWithError:(NSError *)error;
// Data written successfully
- (void)POSWIFIManager:(POSWIFIManager *)manager didWriteDataWithTag:(long)tag;
/// Receive the data returned by the printer
/// @param manager Management object
/// @param data Returned data
/// @param tag tag
- (void)POSWIFIManager:(POSWIFIManager *)manager didReadData:(NSData *)data tag:(long)tag;
// 断开连接
- (void)POSWIFIManagerDidDisconnected:(POSWIFIManager *)manager;
@end

@interface POSWIFIManager : NSObject
{
    int commandSendMode; //命令发送模式 0:立即发送 1：批量发送
}
#pragma mark - 基本属性
// 主机地址
@property (nonatomic,copy) NSString *hostStr;
// 端口
@property (nonatomic,assign) UInt16 port;
// 是否连接成功
@property (nonatomic,assign) BOOL connectOK;
// 是自动断开连接 还是 手动断开
@property (nonatomic,assign) BOOL isAutoDisconnect;

@property (nonatomic,weak) id<POSWIFIManagerDelegate> delegate;
// 连接回调
@property (nonatomic,copy) POSWIFIBlock callBack;
// 接收服务端返回的数据
@property (nonatomic,copy) POSWIFICallBackBlock callBackBlock;
@property (nonatomic,strong) NSMutableArray *commandBuffer;
//是否连接成功
@property (nonatomic,assign) BOOL isConnected;
//接收到的数据
@property(nonatomic,copy)NSData* receivedData;
//是否成功监控打印机的状态
@property (nonatomic,assign) BOOL isReceivedData;
//打印机类型
@property(nonatomic,assign)NSUInteger printerType;
@property(strong,nonatomic) NSTimer *timer;
@property(nonatomic,assign)bool isFirstRece;
//发送队列数组
#pragma mark - 基本方法
+ (instancetype)shareWifiManager;
//连接主机
-(void)POSConnectWithHost:(NSString *)hostStr port:(UInt16)port completion:(POSWIFIBlock)block;
// 断开主机
- (void)POSDisConnect;

-(BOOL)ReturnIsDisconnectedOrNot;

//修改版本的推荐使用发送数据的两个方法
-(void)POSWriteCommandWithData:(NSData *)data;

-(void)POSWriteCommandWithData:(NSData *)data withResponse:(POSWIFICallBackBlock)block;

// 发送TSC完整指令
- (void)POSSendMSGWith:(NSString *)str;

- (void)POSSetCommandMode:(BOOL)Mode;

/*
 * 74.设置打印机发送命令模式
 * 范围：0，1
 ＊ 0:立即发送
 ＊ 1:批量发送
 */

-(NSArray*)POSGetBuffer;
/*
 * 75.返回等待发送指令队列
 */

-(void)POSClearBuffer;
/*
 * 76.清空等待发送指令队列
 */

-(void)POSSendCommandBuffer;
/*
 * 77.发送指令队列
 */


@end
