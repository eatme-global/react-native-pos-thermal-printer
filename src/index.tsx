export * from './types';
export * from './printerModule';
export { useThermalPrinter } from './useThermalPrinter';
export { EventServiceProvider } from './EventService';

// import { NativeModules, Platform } from 'react-native';

// import EventService from './EventService';

// export { EventService };

// const LINKING_ERROR =
//   `The package 'react-native-esc-pos-printer' doesn't seem to be linked. Make sure: \n\n` +
//   Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
//   '- You rebuilt the app after installing the package\n' +
//   '- You are not using Expo Go\n';

// const EscPosPrinter = NativeModules.EscPosPrinter
//   ? NativeModules.EscPosPrinter
//   : new Proxy(
//       {},
//       {
//         get() {
//           throw new Error(LINKING_ERROR);
//         },
//       }
//     );

// export enum PRINTER_WIDTH {
//   MM80 = '80mm',
//   MM58 = '58mm',
// }

// type IPPrinter = {
//   ip: string;
// };

// export interface PrinterStatus {
//   printerIp: string;
//   isReachable: boolean;
//   printerName: string;
// }

// export type { IPPrinter };

// export async function reconnectPrinter(ip: string) {
//   try {
//     const result = await EscPosPrinter.retryPrinterConnection(ip);
//     console.log('Printer connection retry successful: ', result);
//     return result;
//   } catch (error) {
//     console.error('Error retrying printer connection: ', error);
//     return false;
//   }
// }

// type PrintJob = {
//   type: 'TEXT' | 'FEED' | 'CUT' | 'COLUMN' | 'IMAGE' | 'QRCODE' | 'CASHBOX';
//   text?: string | string[];
//   bold?: boolean;
//   fontSize?: 'NORMAL' | 'WIDE' | 'TALL' | 'BIG';
//   alignment?: 'LEFT' | 'CENTER' | 'RIGHT';
//   url?: string;
//   width?: number;
//   chineseCharacters?: boolean;
//   lines?: number;
//   wrapWords?: boolean;
//   columns?: Array<{
//     wrapWords?: boolean;
//     text: string;
//     width: number;
//     alignment: 'LEFT' | 'CENTER' | 'RIGHT';
//   }>;
// };

// export async function printText(ip: String) {
//   const result: PrintJob[] = [
//     {
//       type: 'TEXT',
//       text: 'String with ƒäñçÿ çhåråctérs, ä, ö, ü, and ß',
//       bold: true,
//       alignment: 'CENTER',
//       fontSize: 'NORMAL',
//     },
//     // {
//     //   type: 'TEXT',
//     //   text: '欢迎光临欢光欢迎光临欢光',
//     //   bold: true,
//     //   alignment: 'LEFT',
//     //   fontSize: 'NORMAL',
//     // },
//     {
//       type: 'TEXT',
//       text: 'String with ƒäñçÿ çhåråctérs, German uses the same ä, ö, ü, and ß',
//       bold: true,
//       wrapWords: true,
//       alignment: 'CENTER',
//       fontSize: 'NORMAL',
//     },
//     // { type: 'FEED', lines: 1 },
//     // {
//     //   type: 'TEXT',
//     //   text: 'String with ƒäñçÿ çhåråctérs, German uses the same ä, ö, ü, and ß',
//     //   bold: true,
//     //   wrapWords: false,
//     //   alignment: 'CENTER',
//     //   fontSize: 'NORMAL',
//     // },
//     { type: 'FEED', lines: 1 },
//     {
//       type: 'COLUMN',
//       bold: false,
//       fontSize: 'NORMAL',
//       columns: [
//         { text: '123456789|123456789|', width: 20, alignment: 'LEFT' },
//         { text: '', width: 2, alignment: 'LEFT' },
//         { text: 'Qty', width: 5, alignment: 'CENTER' },
//         { text: '', width: 2, alignment: 'LEFT' },
//         { text: 'Price', width: 19, alignment: 'RIGHT' },
//       ],
//     },
//     {
//       type: 'COLUMN',
//       bold: true,
//       fontSize: 'NORMAL',
//       columns: [
//         { text: 'TRY', width: 20, alignment: 'LEFT' },
//         { text: '', width: 2, alignment: 'LEFT' },
//         { text: 'Qty', width: 5, alignment: 'CENTER' },
//         { text: '', width: 2, alignment: 'LEFT' },
//         { text: 'Price', width: 19, alignment: 'RIGHT' },
//       ],
//     },
//     // {
//     //   type: 'COLUMN',
//     //   // bold: false,
//     //   fontSize: 'NORMAL',
//     //   columns: [
//     //     { text: '欢迎光临欢光欢', width: 20, alignment: 'LEFT' },
//     //     { text: '', width: 2, alignment: 'LEFT' },
//     //     {
//     //       text: 'Him therfd  eateaster',
//     //       width: 19,
//     //       alignment: 'LEFT',
//     //       wrapWords: true,
//     //     },
//     //     { text: '', width: 2, alignment: 'LEFT' },
//     //     { text: '$10.00', width: 5, alignment: 'RIGHT' },
//     //   ],
//     // },
//     // {
//     //   type: 'COLUMN',
//     //   bold: false,
//     //   fontSize: 'NORMAL',
//     //   columns: [
//     //     { text: '欢迎光临欢光欢', width: 20, alignment: 'LEFT' },
//     //     { text: '', width: 2, alignment: 'LEFT' },
//     //     {
//     //       text: 'Him thersd eateaster',
//     //       width: 10,
//     //       alignment: 'LEFT',
//     //       wrapWords: false,
//     //     },
//     //     { text: '', width: 2, alignment: 'LEFT' },
//     //     { text: '$10.00', width: 14, alignment: 'RIGHT' },
//     //   ],
//     // },
//     { type: 'FEED', lines: 1 },
//     {
//       type: 'IMAGE',
//       // url: 'https://upload.wikimedia.org/wikipedia/commons/2/24/Adidas_logo.png',
//       url: 'https://png.pngtree.com/png-clipart/20190921/original/pngtree-beautiful-black-and-white-butterfly-png-image_4699516.jpg',
//       alignment: 'CENTER',
//       // width: 50,
//     },
//     {
//       type: 'QRCODE',
//       text: 'https://upload.wikimedia.org/wikipedia/commons/2/24/Adidas_logo.png',
//       alignment: 'RIGHT',
//     },
//     {
//       type: 'CASHBOX',
//     },
//     { type: 'CUT' },
//   ];

//   await EscPosPrinter.setPrintJobs(ip, result);
// }

// export async function printPendingJobsWithNewPrinter(
//   oldPrinterIp: string,
//   newPrinterIp: string
// ) {
//   const status = await EscPosPrinter.printFromNewPrinter(
//     oldPrinterIp,
//     newPrinterIp
//   );
//   return status;
// }

// export async function addPrinterToPool(printer: IPPrinter) {
//   const result = await EscPosPrinter.addPrinterToPool(printer);

//   console.log('Printer added to pool: ', result);
//   return result;
// }

// export async function removePrinterFromPool(ip: string) {
//   console.log('remove printer from pool', ip);
//   const result = await EscPosPrinter.removePrinterFromPool(ip);
//   return result;
// }

// export async function printImage(base64Image: String) {
//   await EscPosPrinter.printImage(base64Image);
// }

// export async function getPrinterPoolStatus() {
//   const status: PrinterStatus[] = await EscPosPrinter.getPrinterPoolStatus();
//   return status;
// }

// export async function getBluetoothDevices() {
//   EscPosPrinter.checkBluetoothAvailableDevices();
// }

// export async function initializePrinterPool(): Promise<boolean> {
//   try {
//     return await EscPosPrinter.initializePrinterPool();
//   } catch (error) {
//     console.error('Error initializing printer pool:', error);
//     return false;
//   }
// }

// // const result: PrintJob[] = [
// // {
// //   type: 'TEXT',
// //   text: 'String with ƒäñçÿ çhåråctérs, ä, ö, ü, and ß',
// //   bold: true,
// //   alignment: 'CENTER',
// //   fontSize: 'NORMAL',
// // },
// // {
// //   type: 'TEXT',
// //   text: '欢迎光临欢光欢迎光临欢光',
// //   bold: true,
// //   alignment: 'LEFT',
// //   fontSize: 'NORMAL',
// // },
// // {
// //   type: 'TEXT',
// //   text: 'String with ƒäñçÿ çhåråctérs, German uses the same ä, ö, ü, and ß',
// //   bold: true,
// //   wrapWords: true,
// //   alignment: 'CENTER',
// //   fontSize: 'NORMAL',
// // },
// // { type: 'FEED', lines: 1 },
// // {
// //   type: 'TEXT',
// //   text: 'String with ƒäñçÿ çhåråctérs, German uses the same ä, ö, ü, and ß',
// //   bold: true,
// //   wrapWords: false,
// //   alignment: 'CENTER',
// //   fontSize: 'NORMAL',
// // },
// // { type: 'FEED', lines: 1 },
// // {
// //   type: 'COLUMN',
// //   bold: false,
// //   fontSize: 'NORMAL',
// //   columns: [
// //     { text: '123456789|123456789|', width: 20, alignment: 'LEFT' },
// //     { text: '', width: 2, alignment: 'LEFT' },
// //     { text: 'Qty', width: 5, alignment: 'CENTER' },
// //     { text: '', width: 2, alignment: 'LEFT' },
// //     { text: 'Price', width: 19, alignment: 'RIGHT' },
// //   ],
// // },
// // {
// //   type: 'COLUMN',
// //   bold: true,
// //   fontSize: 'NORMAL',
// //   columns: [
// //     { text: 'TRY', width: 20, alignment: 'LEFT' },
// //     { text: '', width: 2, alignment: 'LEFT' },
// //     { text: 'Qty', width: 5, alignment: 'CENTER' },
// //     { text: '', width: 2, alignment: 'LEFT' },
// //     { text: 'Price', width: 19, alignment: 'RIGHT' },
// //   ],
// // },
// //   {
// //     type: 'COLUMN',
// //     // bold: false,
// //     fontSize: 'NORMAL',
// //     columns: [
// //       { text: '欢迎光临欢光欢', width: 20, alignment: 'LEFT' },
// //       { text: '', width: 2, alignment: 'LEFT' },
// //       {
// //         text: 'Him therfd  eateaster',
// //         width: 19,
// //         alignment: 'LEFT',
// //         wrapWords: true,
// //       },
// //       { text: '', width: 2, alignment: 'LEFT' },
// //       { text: '$10.00', width: 5, alignment: 'RIGHT' },
// //     ],
// //   },
// //   {
// //     type: 'COLUMN',
// //     // bold: false,
// //     fontSize: 'NORMAL',
// //     columns: [
// //       { text: '欢迎光临欢光欢', width: 20, alignment: 'LEFT' },
// //       { text: '', width: 2, alignment: 'LEFT' },
// //       {
// //         text: 'Him thersd eateaster',
// //         width: 10,
// //         alignment: 'LEFT',
// //         wrapWords: false,
// //       },
// //       { text: '', width: 2, alignment: 'LEFT' },
// //       { text: '$10.00', width: 14, alignment: 'RIGHT' },
// //     ],
// //   },
// //   { type: 'FEED', lines: 1 },
// //   {
// //     type: 'IMAGE',
// //     // url: 'https://upload.wikimedia.org/wikipedia/commons/2/24/Adidas_logo.png',
// //     url: 'https://png.pngtree.com/png-clipart/20190921/original/pngtree-beautiful-black-and-white-butterfly-png-image_4699516.jpg',
// //     alignment: 'CENTER',
// //     // width: 50,
// //   },
// //   {
// //     type: 'QRCODE',
// //     text: 'https://upload.wikimedia.org/wikipedia/commons/2/24/Adidas_logo.png',
// //     alignment: 'RIGHT',
// //   },
// //   {
// //     type: 'CASHBOX',
// //   },
// //   { type: 'CUT' },
// // ];
