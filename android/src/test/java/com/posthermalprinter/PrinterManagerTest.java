package com.posthermalprinter;

import android.os.Build;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.posthermalprinter.helper.PrintJobHandler;
import com.posthermalprinter.helper.PrinterUtils;
import com.posthermalprinter.util.PrintItem;
import com.posthermalprinter.util.PrinterJob;
import com.posthermalprinter.util.PrinterStatus;
import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.TaskCallback;
import net.posprinter.utils.PosPrinterDev;
import net.posprinter.utils.PosPrinterDev.PrinterInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.verify;
import com.posthermalprinter.helper.PrintQueueProcessor;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P)
public class PrinterManagerTest {

    @Mock
    private ReactApplicationContext mockReactContext;
    @Mock
    private IMyBinder mockBinder;

    private PrinterManager printerManager;
    private List<String> printerPool;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        printerPool = new ArrayList<>();
        printerManager = new PrinterManager(printerPool, mockReactContext);
        PosThermalPrinterModule.Companion.setBinder(mockBinder);
    }

    // Existing tests
    @Test
    public void testPrinterPool_InitialState() {
        assertTrue("Printer pool should be empty initially", printerPool.isEmpty());
    }

    @Test
    public void testAddPrintJob_Success() {
        PrinterJob mockJob = mock(PrinterJob.class);
        when(mockJob.getTargetPrinterIp()).thenReturn("192.168.1.100");
        Boolean result = printerManager.addPrintJob(mockJob);
        assertTrue(result);
    }

    @Test
    public void testAddPrintJob_Failure() {
        PrinterJob mockJob = null;
        Boolean result = printerManager.addPrintJob(mockJob);
        assertFalse(result);
    }

    @Test
    public void testAddPrintJobWithInvalidIP() {
        PrinterJob mockJob = mock(PrinterJob.class);
        when(mockJob.getTargetPrinterIp()).thenReturn("");
        Boolean result = printerManager.addPrintJob(mockJob);
        assertTrue(result);
    }

    @Test
    public void testShutdown() {
        try {
            printerManager.shutdown();
            assertTrue(true);
        } catch (Exception e) {
            fail("Shutdown should not throw an exception");
        }
    }

    @Test
    public void testAddPrinterAsync_Failure() {
        String printerIp = "invalid_ip";
        CompletableFuture<Boolean> result = printerManager.addPrinterAsync(printerIp);
        assertFalse(result.join());
        assertFalse(printerPool.contains(printerIp));
    }

@Test
public void testRemovePrinterAsync_Success() {
    String printerIp = "192.168.1.100";
    ArrayList<PosPrinterDev.PrinterInfo> printerList = new ArrayList<>();

    // Mock PrinterInfo
    PosPrinterDev.PrinterInfo printerInfo = Mockito.mock(PosPrinterDev.PrinterInfo.class);
    printerInfo.portInfo = printerIp;
    printerInfo.printerName = "TestPrinter";

    printerList.add(printerInfo);

    when(mockBinder.GetPrinterInfoList()).thenReturn(printerList);
    doAnswer(invocation -> {
        TaskCallback callback = invocation.getArgument(1);
        callback.OnSucceed();
        return null;
    }).when(mockBinder).RemovePrinter(anyString(), any(TaskCallback.class));

    printerPool.add(printerIp);
    CompletableFuture<Boolean> result = printerManager.removePrinterAsync(printerIp);
    assertTrue(result.join());
    assertFalse(printerPool.contains(printerIp));
}

    @Test
    public void testRemovePrinterAsync_PrinterNotFound() {
        String printerIp = "192.168.1.100";
        when(mockBinder.GetPrinterInfoList()).thenReturn(new ArrayList<>());

        CompletableFuture<Boolean> result = printerManager.removePrinterAsync(printerIp);
        assertFalse(result.join());
    }

    @Test
    public void testGetPrinterPoolStatus() throws ExecutionException, InterruptedException {
        when(mockBinder.GetPrinterInfoList()).thenReturn(new ArrayList<>());
        List<PrinterStatus> status = printerManager.getPrinterPoolStatus();
        assertNotNull(status);
    }

    @Test
    public void testGetPendingJobs() {
        // Create a mock WritableArray
        WritableArray mockArray = mock(WritableArray.class);

        // Mock the QueueProcessor to avoid native method calls
        PrintQueueProcessor mockQueueProcessor = mock(PrintQueueProcessor.class);
        when(mockQueueProcessor.getPendingJobsForJS(any())).thenReturn(mockArray);

        // Use reflection to set the mocked QueueProcessor
        try {
            Field queueProcessorField = PrinterManager.class.getDeclaredField("queueProcessor");
            queueProcessorField.setAccessible(true);
            queueProcessorField.set(printerManager, mockQueueProcessor);
        } catch (Exception e) {
            fail("Failed to set mock QueueProcessor: " + e.getMessage());
        }

        // Test the method
        WritableArray result = printerManager.getPendingJobs();

        // Verify
        assertNotNull(result);
        verify(mockQueueProcessor).getPendingJobsForJS(any());
    }

    @Test
    public void testDeletePendingJobs_Success() {
        String jobId = "test_job_id";
        Boolean result = printerManager.deletePendingJobs(jobId);
        assertNotNull(result);
    }

    @Test
    public void testChangePendingPrintJobsPrinter() {
        String oldPrinterIp = "192.168.1.100";
        String newPrinterIp = "192.168.1.101";
        printerManager.changePendingPrintJobsPrinter(oldPrinterIp, newPrinterIp);
        // Verify no exceptions thrown
        assertTrue(true);
    }

    @Test
    public void testRetryPendingJobFromNewPrinter() {
        String jobId = "test_job_id";
        String printerIp = "192.168.1.100";
        printerManager.retryPendingJobFromNewPrinter(jobId, printerIp);
        // Verify no exceptions thrown
        assertTrue(true);
    }
}
