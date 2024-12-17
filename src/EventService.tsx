import React, { useEffect, useRef } from "react";
import { NativeEventEmitter, NativeModules, Platform } from "react-native";
import { initializePrinterPool } from "./printerModule";

const { PrinterReachability } = NativeModules;
const { PosThermalPrinter } = NativeModules;

const eventEmitter = new NativeEventEmitter(
  Platform.OS === "android" ? PrinterReachability : PosThermalPrinter,
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
  const isInitialized = useRef<boolean>(false);

  useEffect(() => {
    const initializePrinters = async () => {
      if (!isInitialized.current) {
        try {
          await initializePrinterPool();
          isInitialized.current = true;
        } catch (error) {
          console.error("Failed to initialize printer pool:", error);
        }
      }
    };

    const handlePrinterUnreachable = async (event: { printerIp: string }) => {
      try {
        await onReconnect?.(event.printerIp);
      } catch (error) {
        console.error("Failed to reconnect printer:", error);
      }
    };

    const handlePrePrint = () => {
      onBeforePrint?.();
    };

    // Initialize printers
    initializePrinters();

    // Set up event listeners
    const unreachableSubscription = eventEmitter.addListener(
      "PrinterUnreachable",
      handlePrinterUnreachable,
    );

    const prePrintSubscription = eventEmitter.addListener(
      "PrePrintCheck",
      handlePrePrint,
    );

    // Cleanup function
    return () => {
      unreachableSubscription.remove();
      prePrintSubscription.remove();
    };
  }, [onBeforePrint, onReconnect]);

  return <>{children}</>;
};

export default EventServiceProvider;
