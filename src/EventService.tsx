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
  reconnectFunction: ReconnectFunction;
}

export const EventServiceProvider: React.FC<EventServiceProviderProps> = ({
  children,
  reconnectFunction,
}) => {
  useEffect(() => {
    const subscription = eventEmitter.addListener(
      'PrinterUnreachable',
      async (event: { printerIp: string }) => {
        console.log(
          `Printer unreachable event received for IP: ${event.printerIp}`
        );
        await reconnectFunction(event.printerIp);
      }
    );
    return () => subscription.remove();
  }, [reconnectFunction]);

  return <>{children}</>;
};
