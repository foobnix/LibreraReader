package com.foobnix.pdf.info;

import com.foobnix.ext.CacheZipUtils;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class BookCacheLeasesTest {
    @Test public void startupRemovesAbandonedPartsButConcurrentRequestsKeepActiveParts() throws Exception {
        File dir = directory(), abandoned = new File(dir, "old.part");
        Files.write(abandoned.toPath(), new byte[]{1});
        File first = BookCacheLeases.temporary(dir, "first-");
        File second = BookCacheLeases.temporary(dir, "second-");
        try {
            assertFalse(abandoned.exists());
            assertNotEquals(first, second);
            assertTrue(first.exists()); assertTrue(second.exists());
        } finally { first.delete(); second.delete(); abandoned.delete(); dir.delete(); }
    }

    @Test public void crashTreesAreDetachedBeforeBackgroundDeletion() throws Exception {
        File root = directory(), abandoned = new File(root, "crashed-cbr.part");
        assertTrue(abandoned.mkdir());
        Files.write(new File(abandoned, "page.jpg").toPath(), new byte[]{1});
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1);
        int[] cleanupPriority = new int[1];
        BookCacheLeases.CLEANUP.execute(() -> {
            cleanupPriority[0] = android.os.Process.getThreadPriority(android.os.Process.myTid());
            started.countDown();
            try { finish.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        });
        File active = null;
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(cleanupPriority[0] >= android.os.Process.THREAD_PRIORITY_BACKGROUND);
            CountDownLatch pageTask = new CountDownLatch(1);
            AppsConfig.executorServiceSingle.execute(pageTask::countDown);
            assertTrue("Document/page tasks must run while deletion is blocked",
                    pageTask.await(5, TimeUnit.SECONDS));
            active = BookCacheLeases.temporary(root, "active-");
            assertFalse(abandoned.exists());
            File[] tombstones = root.listFiles(file -> file.getName().startsWith(".evicted-"));
            assertNotNull(tombstones); assertEquals(1, tombstones.length);
            assertTrue(new File(tombstones[0], "page.jpg").isFile());
            try (AutoCloseable lease = BookCacheLeases.acquire(active)) {
                assertFalse(BookCacheLeases.evict(active));
            }
        } finally {
            finish.countDown();
            CountDownLatch drained = new CountDownLatch(1);
            BookCacheLeases.CLEANUP.execute(drained::countDown);
            assertTrue(drained.await(5, TimeUnit.SECONDS));
            if (active != null) active.delete();
            BookCacheLeases.evictTree(root);
        }
    }

    private File directory() throws Exception {
        return Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "book-cache-leases-").toFile();
    }

    @Test public void failedAtomicPublicationKeepsPreviousGoodOutput() throws Exception {
        File dir = directory(), output = new File(dir, "processed.epub"), absent = new File(dir, "missing.part");
        Files.write(output.toPath(), new byte[]{2,3});
        try {
            assertThrows(java.io.IOException.class, () -> BookCacheLeases.publish(absent, output));
            assertArrayEquals(new byte[]{2,3}, Files.readAllBytes(output.toPath()));
        } finally { output.delete(); dir.delete(); }
    }
    @Test public void replacementIsCompleteAndDoesNotEvictAnActiveOutput() throws Exception {
        File dir = directory(), output = new File(dir, "processed.epub"), partial = new File(dir, "output.part");
        Files.write(output.toPath(), new byte[]{1}); Files.write(partial.toPath(), new byte[]{2,3});
        try (AutoCloseable lease = BookCacheLeases.acquire(output)) {
            BookCacheLeases.publish(partial, output);
            assertArrayEquals(new byte[]{2,3}, Files.readAllBytes(output.toPath()));
            assertFalse(BookCacheLeases.evict(output));
        } finally { output.delete(); partial.delete(); dir.delete(); }
    }

    @Test public void cacheCleanupRetainsAnActiveFileAndItsSiblingResources() throws Exception {
        File root = directory(), book = new File(root, "book"), page = new File(book, "page.html"),
                image = new File(book, "image.png");
        assertTrue(book.mkdir());
        Files.write(page.toPath(), new byte[]{1});
        Files.write(image.toPath(), new byte[]{2});
        try (AutoCloseable lease = BookCacheLeases.acquire(page)) {
            CacheZipUtils.deleteDir(book);
            assertTrue(page.exists());
            assertTrue(image.exists());
            CacheZipUtils.removeFiles(new File[]{page});
            assertTrue(page.exists());
        }
        CacheZipUtils.deleteDir(book);
        assertFalse(book.exists());
        assertTrue(root.delete());
    }





    @Test public void asynchronousMetadataKeepsItsPathUntilWorkFinishes() throws Exception {
        File dir = directory(), source = new File(dir, "converted.epub");
        Files.write(source.toPath(), new byte[]{1});
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1);
        Thread worker = BookCacheLeases.startLeasedThread("metadata-test", Thread.NORM_PRIORITY, () -> {
            started.countDown();
            try { finish.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, source);
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertFalse(BookCacheLeases.evict(source));
        } finally {
            finish.countDown();
            worker.join(5000);
        }
        assertFalse(worker.isAlive());
        assertTrue(BookCacheLeases.evict(source));
        assertTrue(dir.delete());
    }
    @Test public void startupSweepRemovesCrashDirectoriesWithoutFollowingLinksOrDeletingLeases() throws Exception {
        File dir = directory();
        File abandoned = new File(dir, ".evicted-orphan-directory"); assertTrue(abandoned.mkdir());
        Files.write(new File(abandoned, "Book.epub").toPath(), new byte[]{1});
        File active = new File(dir, ".evicted-active-directory"); assertTrue(active.mkdir());
        File detached = new File(dir, ".evicted-orphan"); assertTrue(detached.mkdir());
        File outside = directory();
        File safe = new File(outside, "keep"); Files.write(safe.toPath(), new byte[]{1});
        android.system.Os.symlink(outside.getPath(), new File(detached, "link").getPath());
        try (AutoCloseable lease = BookCacheLeases.acquire(active)) {
            BookCacheLeases.sweepAbandoned(dir);
            assertFalse(abandoned.exists()); assertFalse(detached.exists());
            assertTrue(active.exists()); assertTrue(safe.exists());
        } finally { BookCacheLeases.evictTree(dir); safe.delete(); outside.delete(); }
    }


    @Test public void startupSweepLeavesUnrelatedNestedCachesAlone() throws Exception {
        File root = directory();
        File glide = new File(root, "image_manager_disk_cache"); assertTrue(glide.mkdir());
        File nested = new File(glide, ".evicted-unrelated"); assertTrue(nested.mkdir());
        File owned = new File(root, "saf-open"); assertTrue(owned.mkdir());
        File tombstone = new File(owned, ".evicted-source"); assertTrue(tombstone.mkdir());
        try {
            BookCacheLeases.sweepAbandoned(root);
            assertTrue(nested.exists()); assertTrue(tombstone.exists());
            BookCacheLeases.sweepAbandoned(owned);
            assertFalse(tombstone.exists()); assertTrue(nested.exists());
        } finally { BookCacheLeases.evictTree(root); }
    }

}
