package com.foobnix.work;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class BoundedTasksTest {
    @Test public void taskFailurePropagatesAndInterruptsOtherWork() throws Exception {
        CountDownLatch blockingStarted = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        java.util.concurrent.ExecutionException failure = assertThrows(java.util.concurrent.ExecutionException.class,
                () -> BoundedTasks.run(Arrays.asList(1, 2), 2, () -> false, item -> {
                    try {
                        if (item == 1) {
                            blockingStarted.countDown(); new CountDownLatch(1).await();
                        } else {
                            if (!blockingStarted.await(2, TimeUnit.SECONDS)) throw new AssertionError("Worker not started");
                            throw new IllegalStateException("provider failed");
                        }
                    } catch (InterruptedException stop) { interrupted.countDown(); Thread.currentThread().interrupt(); }
                    return item;
                }, item -> fail("Failed batch must not publish")));
        assertEquals("provider failed", failure.getCause().getMessage());
        assertTrue("Executor leaked blocked worker", interrupted.await(2, TimeUnit.SECONDS));
    }

    @Test public void concurrencyNeverExceedsLimitAndEveryBookPublishesOnce() throws Exception {
        AtomicInteger active = new AtomicInteger(), maximum = new AtomicInteger();
        List<Integer> input = new ArrayList<>(), output = new ArrayList<>();
        for (int i=0; i<500; i++) input.add(i);
        assertTrue(BoundedTasks.run(input, 3, () -> false, item -> {
            int running = active.incrementAndGet(); maximum.accumulateAndGet(running, Math::max);
            try { Thread.yield(); return item; } finally { active.decrementAndGet(); }
        }, output::add));
        assertTrue(maximum.get() <= 3); assertEquals(0, active.get());
        assertEquals(500, output.size()); assertEquals(new java.util.HashSet<>(input), new java.util.HashSet<>(output));
    }

    @Test public void alreadyCancelledRunStartsNoWork() throws Exception {
        assertFalse(BoundedTasks.run(java.util.Collections.emptyList(), 2, () -> true,
                item -> item, item -> fail("Cancelled empty run published")));
        assertFalse(BoundedTasks.run(Arrays.asList(1, 2), 2, () -> true,
                item -> { fail("Cancelled work started"); return item; }, item -> fail("Cancelled work published")));
    }

    @Test public void zeroParallelismStillCompletesWork() throws Exception {
        List<Integer> result = new ArrayList<>();
        assertTrue(BoundedTasks.run(Arrays.asList(1,2), 0, () -> false, item -> item, result::add));
        assertEquals(Arrays.asList(1,2), result);
    }

    @Test public void callerInterruptionStopsBlockedWork() throws Exception {
        CountDownLatch interrupted = new CountDownLatch(1), started = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> result = new java.util.concurrent.atomic.AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                BoundedTasks.run(Arrays.asList(1), 1, () -> false, item -> {
                    started.countDown();
                    try { new CountDownLatch(1).await(); } catch (InterruptedException stop) { interrupted.countDown(); }
                    return item;
                }, item -> fail("Interrupted result published"));
            } catch (Throwable failure) { result.set(failure); }
        });
        caller.start();
        try {
            assertTrue(started.await(2, TimeUnit.SECONDS)); caller.interrupt(); caller.join(2000);
            assertFalse(caller.isAlive()); assertTrue(result.get() instanceof InterruptedException);
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
        } finally { caller.interrupt(); caller.join(2000); }
    }
    @Test public void publishesFastBookBeforeSlowBookFinishes() throws Exception {
        CountDownLatch releaseSlow = new CountDownLatch(1);
        List<Integer> published = new ArrayList<>();
        assertTrue(BoundedTasks.run(Arrays.asList(1, 2), 2, () -> false, item -> {
            if (item == 1) {
                try {
                    if (!releaseSlow.await(2, TimeUnit.SECONDS)) throw new AssertionError("Slow book blocked publication");
                } catch (InterruptedException e) { throw new RuntimeException(e); }
            }
            return item;
        }, item -> {
            published.add(item);
            if (item == 2) releaseSlow.countDown();
        }));
        assertEquals(Arrays.asList(2, 1), published);
    }

    @Test public void cancelledRunDoesNotQueueTheEntireLibrary() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicInteger started = new AtomicInteger();
        List<Integer> books = new ArrayList<>();
        for (int i = 0; i < 10000; i++) books.add(i);
        assertFalse(BoundedTasks.run(books, 2, cancelled::get, item -> {
            started.incrementAndGet();
            return item;
        }, item -> cancelled.set(true)));
        assertTrue(started.get() <= 2);
    }

    @Test public void singleThreadCompletesAllBooksOnce() throws Exception {
        List<Integer> published = new ArrayList<>();
        assertTrue(BoundedTasks.run(Arrays.asList(1, 2, 3), 1, () -> false,
                item -> item, published::add));
        assertEquals(Arrays.asList(1, 2, 3), published);
    }
}
