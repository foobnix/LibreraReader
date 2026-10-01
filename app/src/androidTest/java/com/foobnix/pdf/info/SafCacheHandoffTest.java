package com.foobnix.pdf.info;

import com.foobnix.ext.CacheZipUtils;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class SafCacheHandoffTest {
    private File directory() throws Exception {
        return Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "saf-handoff-").toFile();
    }
    @Test public void epubContextHandoffCannotConsumeAnotherReadersReservation() throws Exception {
        File dir = directory(), file = new File(dir, "source.epub");
        Files.write(file.toPath(), new byte[]{1});
        try {
            BookCacheLeases.reserve(file); BookCacheLeases.reserve(file);
            try (AutoCloseable context = BookCacheLeases.acquire(file)) {
                BookCacheLeases.cancelReservation(file);
                BookCacheLeases.readerOpened(file, false);
            }
            BookCacheLeases.readerClosed(file);
            assertFalse("Another pending open was consumed", BookCacheLeases.evict(file));
            BookCacheLeases.cancelReservation(file);
            assertTrue(BookCacheLeases.evict(file));
        } finally { file.delete(); dir.delete(); }
    }
    @Test public void readersAndPendingOpensPreventEvictionUntilLastReaderCloses() throws Exception {
        File dir = directory(), file = new File(dir, "source.epub");
        Files.write(file.toPath(), new byte[]{1});
        try {
            BookCacheLeases.reserve(file);
            BookCacheLeases.reserve(file);
            assertFalse(BookCacheLeases.evict(file));
            BookCacheLeases.readerOpened(file);
            BookCacheLeases.readerClosed(file);
            assertFalse("Second pending reader lost its file", BookCacheLeases.evict(file));
            BookCacheLeases.readerOpened(file);
            try (AutoCloseable metadata = BookCacheLeases.acquire(file)) {
                BookCacheLeases.readerClosed(file);
                assertFalse("Metadata extraction lost its source", BookCacheLeases.evict(file));
            }
            assertTrue(BookCacheLeases.evict(file));
        } finally {
            file.delete(); dir.delete();
        }
    }
    @Test public void reservationProtectsParentDirectoryUntilHandoffEnds() throws Exception {
        File root = directory(), book = new File(root, "book"), source = new File(book, "source.epub");
        assertTrue(book.mkdir());
        Files.write(source.toPath(), new byte[]{1});
        BookCacheLeases.reserve(source);
        try {
            CacheZipUtils.deleteDir(book);
            assertTrue(source.exists());
        } finally {
            BookCacheLeases.cancelReservation(source);
        }
        CacheZipUtils.deleteDir(book);
        assertFalse(book.exists());
        assertTrue(root.delete());
    }
    @Test public void delegatedOpenConsumesReservationOnlyAtTheOuterBoundary() throws Exception {
        File dir = directory(), source = new File(dir, "source.epub");
        Files.write(source.toPath(), new byte[]{1});
        BookCacheLeases.reserve(source);
        BookCacheLeases.beginManagedOpen();
        try {
            BookCacheLeases.readerOpened(source);
            BookCacheLeases.readerClosed(source);
            assertFalse("Nested reader consumed the outer handoff", BookCacheLeases.evict(source));
        } finally {
            BookCacheLeases.endManagedOpen();
            BookCacheLeases.cancelReservation(source);
        }
        assertTrue(BookCacheLeases.evict(source));
        assertTrue(dir.delete());
    }
    @Test public void startupSweepRemovesCrashDirectoriesWithoutFollowingLinksOrDeletingLeases() throws Exception {
        File dir = directory();
        File abandoned = new File(dir, "saf-book-orphan"); assertTrue(abandoned.mkdir());
        Files.write(new File(abandoned, "Book.epub").toPath(), new byte[]{1});
        File active = new File(dir, "saf-book-active"); assertTrue(active.mkdir());
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
}
