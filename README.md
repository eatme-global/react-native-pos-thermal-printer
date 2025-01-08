# react-native-pos-thermal-printer

A React Native library for seamless integration with ESC/POS thermal printers. Supports network (TCP/IP) and internal printer connections, with comprehensive print job management and printer pool functionality.

## Features

## Supported Printers

### Network Printers

| Manufacturer | Model        | Interface     | Status | Notes                                                      |
| ------------ | ------------ | ------------- | ------ | ---------------------------------------------------------- |
| Epson        | TM-T88VI     | Ethernet/WiFi | Tested | Full feature support. QR code print depends on the printer |
| Epson        | TM-T20III    | Ethernet      | Tested | Full feature support. QR code print depends on the printer |
| Epson        | TM-m30       | Ethernet/WiFi | Tested | Full feature support. QR code print depends on the printer |
| Zywell       | ZY606 - WIFI | Ethernet/WiFi | Tested | Full feature support. QR code print depends on the printer |

### Internal/USB Printers (Android Only)

| Manufacturer | Model | Interface | Status | Notes                                                      |
| ------------ | ----- | --------- | ------ | ---------------------------------------------------------- |
| iMin         | D4    | Internal  | Tested | Full feature support. QR code print depends on the printer |

## Features

- 🖨️ Support for network (TCP/IP) and internal printers
- 🔄 Automatic printer pool management
- 📋 Comprehensive print job queue handling
- 🔌 Printer connection status monitoring
- 🛠️ Error recovery and job retry mechanisms - Pending ⚠️
- 💰 Cash drawer control support
- 🖼️ Text, image, and QR code printing capabilities
- 📱 Cross-platform support (iOS & Android)

## Installation

```bash
npm install react-native-esc-pos-printer
# or
yarn add react-native-esc-pos-printer
```

### iOS Setup

```bash
cd ios && pod install
```

## Basic Usage

```typescript
import {
  printText,
  IPosPrinter,
  PrintJobRowType,
  PrintAlignment,
  PrintFontSize,
} from "react-native-esc-pos-printer";

// Define your printer
const printer: IPosPrinter = {
  ip: "192.168.1.100",
  type: "NETWORK",
};

// Print a simple receipt
const printReceipt = async () => {
  const printJob = [
    {
      type: PrintJobRowType.TEXT,
      text: "Welcome to Our Store",
      bold: true,
      fontSize: PrintFontSize.BIG,
      alignment: PrintAlignment.CENTER,
      wrapWords: true,
    },
    {
      type: PrintJobRowType.FEED,
      lines: 1,
    },
    {
      type: PrintJobRowType.TEXT,
      text: "Thank you for shopping!",
      bold: false,
      fontSize: PrintFontSize.NORMAL,
      alignment: PrintAlignment.CENTER,
      wrapWords: true,
    },
  ];

  const metadata = {
    type: "Receipt",
    orderId: "12345",
  };

  await printText(printer, printJob, metadata);
};
```

## Advanced Features

### Printer Pool Management

```typescript
import {
  initializePrinterPool,
  addPrinterToPool,
  getPrinterPoolStatus,
} from "react-native-esc-pos-printer";

// Initialize printer pool
await initializePrinterPool();

// Add printer to pool
await addPrinterToPool({
  ip: "192.168.1.100",
  type: "NETWORK",
});

// Get printer pool status
const status = await getPrinterPoolStatus();
```

### Event Handling

This library supports to attach the `onReconnect` and `onBeforePrint` functions. `onBeforePrint` function will trigger before every print. `onReconnect` will trigger when ever there is a printer connection failed attempt. All these functions attached with the native side using `eventEmitters`.

```typescript
import { EventServiceProvider } from 'react-native-esc-pos-printer';

const App = () => {
  const handleReconnect = async (printerIp: string) => {
    // Handle printer reconnection
  };

  const handleBeforePrint = () => {
    // Pre-print operations
  };

  return (
    <EventServiceProvider
      onReconnect={handleReconnect}
      onBeforePrint={handleBeforePrint}
    >
      {/* Your app content */}
    </EventServiceProvider>
  );
};
```

### Print Job Types

The library supports various types of print content:

#### 1. Text Printing

```typescript
const textJob = {
  type: PrintJobRowType.TEXT,
  text: "Sample text",
  bold: true,
  fontSize: PrintFontSize.NORMAL,
  alignment: PrintAlignment.LEFT,
  wrapWords: true,
};
```

#### 2. Column Printing

```typescript
const columnJob = {
  type: PrintJobRowType.COLUMN,
  bold: false,
  fontSize: PrintFontSize.NORMAL,
  columns: [
    { text: "Item", width: 2, alignment: PrintAlignment.LEFT, wrapWords: true },
    { text: "Qty", width: 1, alignment: PrintAlignment.CENTER },
    { text: "Price", width: 1, alignment: PrintAlignment.RIGHT },
  ],
};
```

#### 3. Feed Line

```typescript
const feedJob = {
  type: PrintJobRowType.FEED,
  lines: 3, // Adds 3 blank lines
};
```

#### 4. Cut Paper

```typescript
const cutJob = {
  type: PrintJobRowType.CUT,
};
```

#### 5. QR Code

```typescript
const qrJob = {
  type: PrintJobRowType.QRCODE,
  text: "https://example.com",
  alignment: PrintAlignment.CENTER,
};
```

#### 6. Open Cash Drawer

```typescript
const cashboxJob = {
  type: PrintJobRowType.CASHBOX,
};
```

#### 7. Image Printing

Width will be calculated as the percentage of the print paper. Ex. `width: 80` => `80% paper width`. Default value will be `60%`.

```typescript
const imageJob = {
  type: PrintJobRowType.IMAGE,
  url: "file:///path/to/logo.png",
  width: 80,
  alignment: PrintAlignment.CENTER,
};
```

If you find any alignment mismatches on the image print, please use the `printerWidth` to limit or expand the print area to align the image properly.

```typescript
const imageJob = {
  type: PrintJobRowType.IMAGE,
  width: 100,
  url: "file:///path/to/logo.png",
  alignment: PrintAlignment.CENTER,
  printerWidth: 190.0,
};
```

#### Complete Receipt Example

```typescript
const printReceipt = async () => {
  const printJobs: PrintJobRow[] = [
    {
      type: PrintJobRowType.TEXT,
      text: "SAMPLE STORE",
      bold: true,
      fontSize: PrintFontSize.BIG,
      alignment: PrintAlignment.CENTER,
      wrapWords: true,
    },
    {
      type: PrintJobRowType.FEED,
      lines: 1,
    },
    {
      type: PrintJobRowType.IMAGE,
      url: "file:///path/to/logo.png",
      width: 200,
      alignment: PrintAlignment.CENTER,
    },
    {
      type: PrintJobRowType.FEED,
      lines: 1,
    },
    {
      type: PrintJobRowType.COLUMN,
      bold: false,
      fontSize: PrintFontSize.NORMAL,
      columns: [
        { text: "Product", width: 2, alignment: PrintAlignment.LEFT },
        { text: "Qty", width: 1, alignment: PrintAlignment.CENTER },
        { text: "Price", width: 1, alignment: PrintAlignment.RIGHT },
      ],
    },
    {
      type: PrintJobRowType.COLUMN,
      bold: false,
      fontSize: PrintFontSize.NORMAL,
      columns: [
        { text: "Coffee", width: 2, alignment: PrintAlignment.LEFT },
        { text: "2", width: 1, alignment: PrintAlignment.CENTER },
        { text: "$6.00", width: 1, alignment: PrintAlignment.RIGHT },
      ],
    },
    {
      type: PrintJobRowType.FEED,
      lines: 2,
    },
    {
      type: PrintJobRowType.QRCODE,
      text: "https://receipt.example.com/12345",
      alignment: PrintAlignment.CENTER,
    },
    {
      type: PrintJobRowType.FEED,
      lines: 1,
    },
    {
      type: PrintJobRowType.CUT,
    },
  ];

  const metadata = {
    type: "Receipt",
    orderId: "12345",
  };

  await printText(printer, printJobs, metadata);
  await openCashBox(printer);
};
```

Each job type can be combined to create complex print layouts. The printer will process the jobs in sequence, allowing you to build custom receipt templates.

## API Reference

### Available Methods

| Method                           | Description                                          | Parameters                                                                 | Return Type                   | Status    |
| -------------------------------- | ---------------------------------------------------- | -------------------------------------------------------------------------- | ----------------------------- | --------- |
| `reconnectPrinter`               | Attempts to reconnect to a printer                   | `printer: IPosPrinter`                                                     | `Promise<boolean>`            | Available |
| `printText`                      | Sends print job containing text, columns, and images | `printer: IPosPrinter, payload: PrintJobRow[], metadata: PrintJobMetadata` | `Promise<void>`               | Available |
| `openCashBox`                    | Opens the cash drawer                                | `printer: IPosPrinter`                                                     | `Promise<void>`               | Available |
| `printPendingJobsWithNewPrinter` | Prints pending jobs from old printer to new printer  | `oldPrinter: IPosPrinter, newPrinter: IPosPrinter`                         | `Promise<any>`                | Pending   |
| `addPrinterToPool`               | Adds a printer to the printer pool                   | `printer: IPosPrinter`                                                     | `Promise<any>`                | Available |
| `removePrinterFromPool`          | Removes a printer from the pool                      | `printer: IPosPrinter`                                                     | `Promise<any>`                | Available |
| `getPrinterPoolStatus`           | Gets current status of printer pool                  | None                                                                       | `Promise<PrinterStatus[]>`    | Available |
| `getPendingJobs`                 | Retrieves and parses pending print jobs              | None                                                                       | `Promise<ParsedPendingJob[]>` | Pending   |
| `deletePendingJob`               | Deletes a pending print job by ID                    | `jobId: string`                                                            | `Promise<boolean>`            | Pending   |
| `retryPendingJobFromNewPrinter`  | Retries pending job with new printer                 | `jobId: string, printer: IPosPrinter`                                      | `Promise<boolean>`            | Pending   |
| `initializePrinterPool`          | Initializes the printer pool                         | None                                                                       | `Promise<boolean>`            | Available |
| `getPendingPrinterJobs`          | Gets pending jobs for specific printer               | `printer: IPosPrinter`                                                     | `Promise<ParsedPendingJob[]>` | Available |
| `deletePrinterPendingJobs`       | Deletes all pending jobs for a printer               | `printer: IPosPrinter`                                                     | `Promise<boolean>`            | Pending   |
| `retryPendingJobsFromPrinter`    | Retries all pending jobs for a printer               | `printer: IPosPrinter`                                                     | `Promise<boolean>`            | Pending   |
| `getPrinterStatus`               | Gets current status of a specific printer            | `printer: IPosPrinter`                                                     | `Promise<void>`               | Pending   |

### Printer Types

```typescript
interface IPosPrinter {
  ip: string;
  type: PosPrinterType;
}

enum PosPrinterType {
  INTERNAL = "INTERNAL",
  NETWORK = "NETWORK",
}
```

### Print Job Configuration

```typescript
enum PrintFontSize {
  NORMAL = "NORMAL",
  WIDE = "WIDE",
  TALL = "TALL",
  BIG = "BIG",
}

enum PrintAlignment {
  LEFT = "LEFT",
  CENTER = "CENTER",
  RIGHT = "RIGHT",
}
```

### Core Functions

- `printText(printer: IPosPrinter, payload: PrintJobRow[], metadata: PrintJobMetadata): Promise<void>`
- `openCashBox(printer: IPosPrinter): Promise<void>`
- `getPrinterStatus(printer: IPosPrinter): Promise<void>`
- `reconnectPrinter(printer: IPosPrinter): Promise<boolean>`

## Error Handling

The library includes comprehensive error handling and recovery mechanisms:

```typescript
try {
  await printText(printer, printJob, metadata);
} catch (error) {
  console.error("Print error:", error);
  // Implement retry logic or user notification
}
```

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support, please [open an issue](https://github.com/yourusername/react-native-esc-pos-printer/issues) on our GitHub repository.
