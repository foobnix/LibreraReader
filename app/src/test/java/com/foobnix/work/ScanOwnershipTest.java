package com.foobnix.work;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ScanOwnershipTest {
    @Test public void replacedWorkerCannotWrite() {
        long old = ScanOwnership.claim();
        AtomicInteger writes = new AtomicInteger();
        assertTrue(ScanOwnership.write(old, () -> false, writes::incrementAndGet));
        long current = ScanOwnership.claim();
        assertFalse(ScanOwnership.write(old, () -> false, writes::incrementAndGet));
        assertTrue(ScanOwnership.write(current, () -> false, writes::incrementAndGet));
        assertEquals(2, writes.get());
    }
    @Test public void progressDoesNotWaitForBackgroundTransaction() throws Exception {
        long owner = ScanOwnership.claim();
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        Thread writer = new Thread(() -> ScanOwnership.write(owner, () -> false, () -> {
            entered.countDown();
            try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }));
        writer.start();
        try {
            assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
            AtomicInteger progress = new AtomicInteger();
            java.util.concurrent.FutureTask<Boolean> ui = new java.util.concurrent.FutureTask<>(() -> {
                assertTrue(ScanOwnership.isCurrent(owner, () -> false));
                ScanOwnership.tryProgress(owner, () -> false, progress::incrementAndGet);
                return true;
            });
            new Thread(ui).start();
            assertTrue(ui.get(1, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(0, progress.get());
        } finally { release.countDown(); writer.join(2000); }
    }
}
