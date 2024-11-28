import React, { useEffect } from 'react';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import { initializePrinterPool } from './printerModule';

const { PrinterReachability } = NativeModules;
const { PosThermalPrinter } = NativeModules;

const eventEmitter = new NativeEventEmitter(
  Platform.OS === 'android' ? PrinterReachability : PosThermalPrinter
);

type ReconnectFunction = (printerIp: string) => Promise<void>;

interface EventServiceProviderProps {
  children: React.ReactNode;
  onReconnect?: ReconnectFunction;
  onBeforePrint?: () => void;
}

export const EventServiceProvider: React.FC<EventServiceProviderProps> = ({
  children,
  onReconnect,
  onBeforePrint,
}) => {
  useEffect(() => {
    let isSubscribed = true;

    const initializePrinters = async () => {
      try {
        await initializePrinterPool();
      } catch (error) {
        console.error('Failed to initialize printer pool:', error);
      }
    };

    const handlePrinterUnreachable = async (event: { printerIp: string }) => {
      if (!isSubscribed) return;
      try {
        await onReconnect?.(event.printerIp);
      } catch (error) {
        console.error('Failed to reconnect printer:', error);
      }
    };

    const handlePrePrint = () => {
      if (!isSubscribed) return;
      onBeforePrint?.();
    };

    // Initialize printers
    initializePrinters();

    // Set up event listeners
    const unreachableSubscription = eventEmitter.addListener(
      'PrinterUnreachable',
      handlePrinterUnreachable
    );

    const prePrintSubscription = eventEmitter.addListener(
      'PrePrintCheck',
      handlePrePrint
    );

    // Cleanup function
    return () => {
      isSubscribed = false;
      unreachableSubscription.remove();
      prePrintSubscription.remove();
    };
  }, [onBeforePrint, onReconnect]);

  return <>{children}</>;
};

export default EventServiceProvider;
