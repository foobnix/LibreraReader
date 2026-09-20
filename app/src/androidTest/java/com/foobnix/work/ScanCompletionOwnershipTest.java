package com.foobnix.work;

import static org.junit.Assert.*;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import com.foobnix.ui2.BooksService;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.coroutines.EmptyCoroutineContext;
import org.junit.Test;

/** Controlled replacement with the same terminal ownership hooks as the two scan workers. */
public class ScanCompletionOwnershipTest {
    private WorkerParameters parameters() {
        return new WorkerParameters(UUID.randomUUID(), Data.EMPTY, Collections.emptyList(),
                new WorkerParameters.RuntimeExtras(), 0, 0, Runnable::run,
                EmptyCoroutineContext.INSTANCE, new WorkManagerTaskExecutor(Runnable::run),
                new WorkerFactory() {
                    @Override public ListenableWorker createWorker(Context context, String name,
                                                                    WorkerParameters parameters) {
                        throw new AssertionError("Unexpected worker creation");
                    }
                }, (context, id, data) -> { throw new AssertionError("Unexpected progress API"); },
                (context, id, info) -> { throw new AssertionError("Unexpected foreground API"); });
    }

    private final class ControlledScan extends MessageWorker {
        final long owner;
        final boolean success;
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        ControlledScan(Context context, long owner, boolean success) {
            super(context, parameters()); this.owner = owner; this.success = success;
        }
        @Override boolean doWorkInner() throws InterruptedException {
            started.countDown(); release.await(); return success;
        }
        @Override protected boolean reportsOwnCompletion() { return true; }
        @Override protected boolean publishCompletion(Runnable action) {
            return ScanOwnership.write(owner, this::isStopped, action);
        }
        @Override protected boolean publishFailure(Runnable action) {
            return ScanOwnership.write(owner, () -> false, action);
        }
    }

    @Test public void replacementPublishesOnceEvenIfOldWorkerFinishesLast() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicInteger finishes = new AtomicInteger();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) {
                if (BooksService.RESULT_SEARCH_FINISH.equals(intent.getStringExtra(Intent.EXTRA_TEXT)))
                    finishes.incrementAndGet();
            }
        };
        LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(context);
        broadcasts.registerReceiver(receiver, new IntentFilter(BooksService.INTENT_NAME));
        ControlledScan old = new ControlledScan(context, ScanOwnership.claim(), false);
        Thread oldThread = new Thread(old::doWork);
        try {
            oldThread.start();
            assertTrue(old.started.await(3, TimeUnit.SECONDS));
            ControlledScan replacement = new ControlledScan(context, ScanOwnership.claim(), true);
            replacement.release.countDown();
            assertEquals(ListenableWorker.Result.success(), replacement.doWork());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Current worker must report even while old worker is active", 1, finishes.get());
            old.release.countDown(); oldThread.join(3000);
            assertFalse(oldThread.isAlive());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals("Obsolete worker published a terminal event", 1, finishes.get());
        } finally {
            old.release.countDown(); oldThread.join(3000);
            broadcasts.unregisterReceiver(receiver);
        }
    }

    @Test public void cancelledCurrentScanStillSendsOneTerminalEvent() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AtomicInteger finishes = new AtomicInteger();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) {
                if (BooksService.RESULT_SEARCH_FINISH.equals(intent.getStringExtra(Intent.EXTRA_TEXT)))
                    finishes.incrementAndGet();
            }
        };
        LocalBroadcastManager broadcasts = LocalBroadcastManager.getInstance(context);
        broadcasts.registerReceiver(receiver, new IntentFilter(BooksService.INTENT_NAME));
        try {
            ControlledScan cancelled = new ControlledScan(context, ScanOwnership.claim(), true);
            cancelled.stop(androidx.work.WorkInfo.STOP_REASON_CANCELLED_BY_APP);
            cancelled.release.countDown();
            cancelled.doWork();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, finishes.get());
        } finally {
            broadcasts.unregisterReceiver(receiver);
        }
    }
}
