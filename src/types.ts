export enum PRINTER_WIDTH {
  MM80 = '80mm',
  MM58 = '58mm',
}

export interface IPPrinter {
  ip: string;
}

export interface PrinterStatus {
  printerIp: string;
  isReachable: boolean;
  printerName: string;
}

// Enums for better structure
export enum PrintJobRowType {
  TEXT = 'TEXT',
  FEED = 'FEED',
  CUT = 'CUT',
  COLUMN = 'COLUMN',
  IMAGE = 'IMAGE',
  QRCODE = 'QRCODE',
  CASHBOX = 'CASHBOX',
}

export type PrintFontWeight = true | false;

export const PrintFontWeight = {
  NORMAL: false as const,
  BOLD: true as const,
};

export enum PrintFontSize {
  NORMAL = 'NORMAL',
  WIDE = 'WIDE',
  TALL = 'TALL',
  BIG = 'BIG',
}

export enum PrintAlignment {
  LEFT = 'LEFT',
  CENTER = 'CENTER',
  RIGHT = 'RIGHT',
}
interface Column {
  wrapWords?: boolean;
  text: string;
  width: number;
  alignment: PrintAlignment;
}

// Discriminated union for different PrintJobRow types
export type PrintJobRow =
  | TextPrintJobRow
  | FeedPrintJobRow
  | CutPrintJobRow
  | ColumnPrintJobRow
  | ImagePrintJobRow
  | QrCodePrintJobRow
  | CashboxPrintJobRow;

// Type definition when 'type' is 'TEXT'
export interface TextPrintJobRow {
  type: PrintJobRowType.TEXT;
  text: string | string[];
  bold: boolean;
  fontSize: PrintFontSize;
  alignment: PrintAlignment;
  wrapWords: boolean;
}

// Other types with optional fields
export interface FeedPrintJobRow {
  type: PrintJobRowType.FEED;
  lines: number;
}

export interface CutPrintJobRow {
  type: PrintJobRowType.CUT;
}

export interface ColumnPrintJobRow {
  type: PrintJobRowType.COLUMN;
  bold: PrintFontWeight;
  fontSize: PrintFontSize;
  columns: Column[];
}

export interface ImagePrintJobRow {
  type: PrintJobRowType.IMAGE;
  url: string;
  width?: number;
  alignment?: PrintAlignment;
}

export interface QrCodePrintJobRow {
  type: PrintJobRowType.QRCODE;
  text: string;
  alignment?: PrintAlignment;
}

export interface CashboxPrintJobRow {
  type: PrintJobRowType.CASHBOX;
}

/**
 * Represents the user-defined metadata for a print job.
 */
export interface PrintJobMetadata {
  type: string;
  [key: string]: any;
}

export interface ParsedPendingJob {
  metadata: PrintJobMetadata;
  printerIp: string;
  printerName: string;
  jobId: string;
}

export interface RawPendingJob {
  metadata: string;
  printerIp: string;
  printerName: string;
  jobId: string;
}
