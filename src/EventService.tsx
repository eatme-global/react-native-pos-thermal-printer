import React, { useEffect } from 'react';
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';

const { PrinterReachability } = NativeModules;

const { PosThermalPrinter } = NativeModules;
const eventEmitter = new NativeEventEmitter(
  Platform.OS === 'android' ? PrinterReachability : PosThermalPrinter
);

type ReconnectFunction = (printerIp: string) => Promise<void>;

interface EventServiceProviderProps {
  children: React.ReactNode;
  onReconnect?: ReconnectFunction;
}

export const EventServiceProvider: React.FC<EventServiceProviderProps> = ({
  children,
  onReconnect,
}) => {
  useEffect(() => {
    const subscription = eventEmitter.addListener(
      'PrinterUnreachable',
      async (event: { printerIp: string }) => {
        await onReconnect?.(event.printerIp);
      }
    );
    return () => subscription.remove();
  }, [onReconnect]);

  return <>{children}</>;
};
