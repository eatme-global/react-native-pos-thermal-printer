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
    const initialize = async () => {
      // Initialize printer pool
      await initializePrinterPool();
    };

    initialize();

    const unreachableSubscription = eventEmitter.addListener(
      'PrinterUnreachable',
      async (event: { printerIp: string }) => {
        await onReconnect?.(event.printerIp);
      }
    );

    const prePrintSubscription = eventEmitter.addListener(
      'PrePrintCheck',
      async () => {
        onBeforePrint?.();
      }
    );

    return () => {
      unreachableSubscription.remove();
      prePrintSubscription.remove();
    };
  }, [onBeforePrint, onReconnect]);

  return <>{children}</>;
};
