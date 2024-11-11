/* eslint-disable react-native/no-inline-styles */
import {
  Alert,
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';

import {
  EventServiceProvider,
  PrintAlignment,
  PrintFontSize,
  PrintFontWeight,
  PrintJobRowType,
  useThermalPrinter,
} from 'react-native-pos-thermal-printer';

import type {
  IPPrinter,
  ParsedPendingJob,
  PrintJobRow,
  PrinterStatus,
} from 'react-native-pos-thermal-printer';

import { useCallback, useEffect, useState } from 'react';

export default function App() {
  const customRetryFunction = useCallback(
    async (printerIp: string | { ip: string }) => {
      // Implement a reconnect mechanism [reconnect printer]
      Alert.alert(
        'Printer Unreachable',
        `Unable to connect to printer at IP ${printerIp}`
      );
    },
    []
  );
  return (
    <EventServiceProvider onReconnect={customRetryFunction}>
      <PrinterContent />
    </EventServiceProvider>
  );
}

const PrinterContent: React.FC = () => {
  const [printerPoolStatus, setPrinterPoolStatus] = useState<
    PrinterStatus[] | []
  >([]);

  interface PrinterIPs {
    [jobId: string]: string;
  }

  const [printerIPs, setPrinterIPs] = useState<{ [jobId: string]: string }>({});

  const handleIPChange = (jobId: string, text: string): void => {
    setPrinterIPs((prevState: PrinterIPs) => ({
      ...prevState,
      [jobId]: text,
    }));
  };

  const [pendingJobs, setPendingJobs] = useState<ParsedPendingJob[]>([]);

  const {
    reconnectPrinter,
    printText,
    addPrinterToPool,
    getPrinterPoolStatus,
    removePrinterFromPool,
    printPendingJobsWithNewPrinter,
    isInitialized,
    // getPendingJobs,
    getPendingPrinterJobs,
    deletePendingJob,
    retryPendingJobFromNewPrinter,
    deletePrinterPendingJobs,
    retryPendingJobsFromPrinter,
  } = useThermalPrinter();

  useEffect(() => {}, [isInitialized]);

  const handlePrintTest = (ip: string) => {
    const result: PrintJobRow[] = [
      // {
      //   type: PrintJobRowType.TEXT,
      //   text: 'String with ƒäñçÿ çhåråctérs, ä, ö, ü, and ß',
      //   bold: PrintFontWeight.BOLD,
      //   alignment: PrintAlignment.CENTER,
      //   fontSize: PrintFontSize.NORMAL,
      //   wrapWords: false,
      // },
      // {
      //   type: PrintJobRowType.TEXT,
      //   text: 'String with ƒäñçÿ çhåråctérs, German uses the same ä, ö, ü, and ß',
      //   bold: PrintFontWeight.BOLD,
      //   alignment: PrintAlignment.CENTER,
      //   fontSize: PrintFontSize.NORMAL,
      //   wrapWords: false,
      // },
      { type: PrintJobRowType.FEED, lines: 1 },
      {
        type: PrintJobRowType.COLUMN,
        bold: PrintFontWeight.NORMAL,
        fontSize: PrintFontSize.NORMAL,
        columns: [
          {
            text: '123456789|123456789|',
            width: 20,
            alignment: PrintAlignment.LEFT,
          },
          { text: '', width: 2, alignment: PrintAlignment.LEFT },
          { text: 'Qty', width: 5, alignment: PrintAlignment.CENTER },
          { text: '', width: 2, alignment: PrintAlignment.LEFT },
          { text: 'Price', width: 19, alignment: PrintAlignment.RIGHT },
        ],
      },
      {
        type: PrintJobRowType.COLUMN,
        bold: true,
        fontSize: PrintFontSize.NORMAL,
        columns: [
          { text: 'TRY', width: 20, alignment: PrintAlignment.LEFT },
          { text: '', width: 2, alignment: PrintAlignment.LEFT },
          { text: 'Qty', width: 5, alignment: PrintAlignment.CENTER },
          { text: '', width: 2, alignment: PrintAlignment.LEFT },
          { text: 'Price', width: 19, alignment: PrintAlignment.RIGHT },
        ],
      },
      // {
      //   type: PrintJobRowType.IMAGE,
      //   url: 'https://i.pinimg.com/736x/fa/66/73/fa66736df84509ac13e05c9372131550.jpg',
      //   width: 100,
      //   alignment: PrintAlignment.LEFT,
      // },
      {
        type: PrintJobRowType.CASHBOX,
      },
      {
        type: PrintJobRowType.IMAGE,
        width: 50,
        url: 'https://png.pngtree.com/png-clipart/20190921/original/pngtree-beautiful-black-and-white-butterfly-png-image_4699516.jpg',
        alignment: PrintAlignment.CENTER,
        printerWidth: 190.0,
      },
      // {
      //   type: PrintJobRowType.QRCODE,
      //   text: 'https://upload.wikimedia.org/wikipedia/commons/2/24/Adidas_logo.png',
      //   alignment: PrintAlignment.RIGHT,
      // },
      {
        type: PrintJobRowType.FEED,
        lines: 5,
      },
      { type: PrintJobRowType.CUT },
    ];

    printText(ip, result, { type: 'Receipt', category: 'Drinks' });
  };

  const addNewPrinterToPool = async (ip: string) => {
    const printer: IPPrinter = {
      ip: ip,
    };

    const status = await addPrinterToPool(printer);
    if (status) {
      handlePrinterPoolStatus();
    }
  };

  const handleReConnectPrinter = async (ip: string) => {
    const status = await reconnectPrinter(ip);
    if (status) {
      handlePrinterPoolStatus();
    }
  };

  const handlePrinterPoolStatus = async () => {
    const status = await getPrinterPoolStatus();
    setPrinterPoolStatus(status);
  };

  const handleRemovePrinter = async (ip: string) => {
    const status = await removePrinterFromPool(ip);
    if (status) {
      handlePrinterPoolStatus();
    }
  };

  const handleDeleteJob = async (jobId: string) => {
    const status = await deletePendingJob(jobId);
    if (status) {
      // fetchPendingJobs();
    }
  };

  // const fetchPendingJobs = async () => {
  // const result = await getPendingJobs();
  // setPendingJobs(result);
  // };

  const fetchPrinterPendingJobs = async (ip: string) => {
    const result = await getPendingPrinterJobs(ip);
    console.log(result.length);
    setPendingJobs(result);
  };

  const deletePendingJobs = async (ip: string) => {
    const result = await deletePrinterPendingJobs(ip);
    if (result) {
      setPendingJobs([]);
    }
  };

  const retryPendingPrinterJobs = async (ip: string) => {
    const result = await retryPendingJobsFromPrinter(ip);
    if (result) {
      // fetchPendingJobs();
      fetchPrinterPendingJobs(ip);
    }
  };

  const handlePendingJobPrintFromNewPrinter = async (jobId: string) => {
    const newPrinterIp = printerIPs[jobId];
    if (newPrinterIp) {
      const status = await retryPendingJobFromNewPrinter(jobId, newPrinterIp);
      if (status) {
        // fetchPendingJobs();
      }
    }
  };

  const [ipOld, setOldIp] = useState<string>('');
  const [ipNew, setNewIp] = useState<string>('');
  const [ip, setIp] = useState<string>('');

  return (
    <SafeAreaView>
      <ScrollView style={{}}>
        <View style={styles.container}>
          <TextInput
            style={styles.textInput}
            onChange={(e) => setIp(e.nativeEvent.text)}
            placeholder="Enter IP"
          />

          <Button title="Add Printer" onPress={() => addNewPrinterToPool(ip)} />
          <View style={styles.printerPoolStatusContainer}>
            <Text>
              Printer Pool Connection Status:{' '}
              {isInitialized ? 'initialized' : 'not initialized'}
            </Text>
            <Text style={styles.printerPoolHeading}>Printer Pool Status</Text>

            <Button
              title="Get Printer Pool Status"
              onPress={handlePrinterPoolStatus}
            />
            <View>
              {printerPoolStatus.length > 0 &&
                printerPoolStatus.map((printerStatus, index) => (
                  <View
                    key={index}
                    style={{
                      flexDirection: 'column',
                      borderWidth: 1,
                      borderColor: 'gray',
                      backgroundColor: printerStatus.isReachable
                        ? '#c8f7d6'
                        : '#ffd4d9',
                      marginBottom: 10,
                      padding: 10,
                    }}
                  >
                    <Text>Printer Name:{printerStatus.printerName}</Text>
                    <Text>Printer IP:{printerStatus.printerIp}</Text>
                    <Text>
                      IP Status:{' '}
                      {printerStatus.isReachable
                        ? 'Reachable'
                        : 'Not Reachable'}
                    </Text>
                    <View style={{ marginTop: 10, flexDirection: 'column' }}>
                      <TouchableOpacity
                        onPress={() => handlePrintTest(printerStatus.printerIp)}
                        style={{
                          backgroundColor: 'gray',
                          paddingVertical: 10,
                          paddingHorizontal: 20,
                        }}
                      >
                        <Text style={{ color: 'white', textAlign: 'center' }}>
                          Printer Test
                        </Text>
                      </TouchableOpacity>

                      <TouchableOpacity
                        onPress={() =>
                          handleRemovePrinter(printerStatus.printerIp)
                        }
                        style={{
                          backgroundColor: 'red',
                          paddingVertical: 10,
                          paddingHorizontal: 20,
                        }}
                      >
                        <Text style={{ color: 'white', textAlign: 'center' }}>
                          Printer Remove
                        </Text>
                      </TouchableOpacity>

                      {!printerStatus.isReachable && (
                        <TouchableOpacity
                          onPress={() =>
                            handleReConnectPrinter(printerStatus.printerIp)
                          }
                          style={{
                            backgroundColor: 'orange',
                            paddingVertical: 10,
                            paddingHorizontal: 20,
                          }}
                        >
                          <Text style={{ color: 'white', textAlign: 'center' }}>
                            Reconnect Printer
                          </Text>
                        </TouchableOpacity>
                      )}
                    </View>
                  </View>
                ))}
            </View>
            <View
              style={{
                height: 2,
                backgroundColor: 'black',
                width: 300,
                marginVertical: 20,
              }}
            />
            <View style={{}}>
              <Text style={styles.printerPoolHeading}>
                Reroute pending jobs
              </Text>
              <TextInput
                style={{ ...styles.textInput, width: '100%' }}
                onChange={(e) => setOldIp(e.nativeEvent.text)}
                placeholder="Pending jobs IP"
              />

              <TextInput
                style={{ ...styles.textInput, width: '100%' }}
                onChange={(e) => setNewIp(e.nativeEvent.text)}
                placeholder="New printer IP"
              />
            </View>

            <Button
              title="Print Pending Jobs with New Printer"
              onPress={() => printPendingJobsWithNewPrinter(ipOld, ipNew)}
            />
            <View
              style={{
                height: 2,
                backgroundColor: 'black',
                width: 300,
                marginVertical: 20,
              }}
            />

            <Button
              title="delete pending Jobs 127"
              onPress={() => deletePendingJobs('192.168.8.127')}
            />
            {pendingJobs.length > 0 ? (
              <Button
                title="retry Pending Jobs 127"
                onPress={() => retryPendingPrinterJobs('192.168.8.127')}
              />
            ) : (
              <View />
            )}
            <View>
              {pendingJobs.length > 0 &&
                pendingJobs.map((job, index) => {
                  return (
                    <View
                      key={index}
                      style={{
                        flexDirection: 'column',
                        borderWidth: 1,
                        borderColor: 'gray',
                        backgroundColor: '#ffd4d9',
                        padding: 10,
                      }}
                    >
                      <Text>Job ID:{job.jobId}</Text>
                      <Text>Printer Name:{job.printerName}</Text>
                      <Text>Printer IP:{job.printerName}</Text>
                      <Text>Print Type:{job.metadata.type}</Text>
                      <Text>Print Metadata:{JSON.stringify(job.metadata)}</Text>
                      <TouchableOpacity
                        onPress={() => handleDeleteJob(job.jobId)}
                        style={{
                          backgroundColor: 'red',
                          paddingVertical: 10,
                          paddingHorizontal: 20,
                        }}
                      >
                        <Text style={{ color: 'white', textAlign: 'center' }}>
                          Remove Job
                        </Text>
                      </TouchableOpacity>

                      <TextInput
                        style={{
                          borderWidth: 1,
                          borderColor: 'gray',
                          padding: 10,
                          marginVertical: 5,
                        }}
                        placeholder="Enter new printer IP"
                        value={printerIPs[job.jobId] || ''}
                        onChangeText={(text) => handleIPChange(job.jobId, text)}
                      />

                      <TouchableOpacity
                        onPress={() =>
                          handlePendingJobPrintFromNewPrinter(job.jobId)
                        }
                        style={{
                          backgroundColor: 'blue',
                          paddingVertical: 10,
                          paddingHorizontal: 20,
                          marginVertical: 5,
                        }}
                      >
                        <Text style={{ color: 'white', textAlign: 'center' }}>
                          Print Job From New Printer
                        </Text>
                      </TouchableOpacity>
                    </View>
                  );
                })}
            </View>
          </View>
          <Button
            title="Get Pending Jobs 127"
            onPress={() => fetchPrinterPendingJobs('192.168.8.127')}
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginVertical: 50,
    alignItems: 'center',
    justifyContent: 'center',
  },
  textInput: {
    width: '80%',
    borderColor: 'gray',
    borderWidth: 1,
    marginBottom: 10,
    padding: 10,
  },
  printerPoolStatusContainer: { gap: 5, marginTop: 20, marginBottom: 10 },
  printerPoolHeading: {
    textAlign: 'center',
    fontWeight: 'bold',
    marginTop: 10,
    marginBottom: 10,
  },
});
