package com.foobnix.ext;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.pdf.info.BookCacheLeases;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class BookCacheRetentionTest {
    @Test public void cancelledOpenKeepsCompletedOutputsFromOtherBooks() throws Exception {
        android.content.Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(target);
        File oldBookDirectory = CacheZipUtils.CACHE_BOOK_DIR;
        File oldTempDirectory = CacheZipUtils.CACHE_TEMP;
        File root = Files.createTempDirectory(target.getCacheDir().toPath(), "cancelled-open-").toFile();
        File books = new File(root, "Book"), temp = new File(root, "Temp");
        assertTrue(books.mkdir()); assertTrue(temp.mkdir());
        File completed = new File(books, "processed-epub-other.epub");
        Files.write(completed.toPath(), new byte[]{1, 2, 3});
        boolean previousCancellation = com.foobnix.sys.TempHolder.get().loadingCancelled.getAndSet(true);
        try {
            CacheZipUtils.CACHE_BOOK_DIR = books;
            CacheZipUtils.CACHE_TEMP = temp;
            new org.ebookdroid.droids.EpubContext().removeTempFilesIfCancel();
            assertTrue("Cancelling one reader discarded another book's completed conversion",
                    completed.isFile());
        } finally {
            com.foobnix.sys.TempHolder.get().loadingCancelled.set(previousCancellation);
            CacheZipUtils.CACHE_BOOK_DIR = oldBookDirectory;
            CacheZipUtils.CACHE_TEMP = oldTempDirectory;
            completed.delete(); books.delete(); temp.delete(); root.delete();
        }
    }
    @Test public void failedNotesAreNotPublishedAsAReusableSidecar() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "notes-publication-").toFile();
        File notes = new File(root, "book.epub.json");
        try {
            com.foobnix.pdf.info.JsonHelper.mapToFile(notes, null);
            assertFalse(notes.exists());
            java.util.Map<String, String> complete = new java.util.HashMap<>();
            complete.put("chapter", "note");
            com.foobnix.pdf.info.JsonHelper.mapToFile(notes, complete);
            assertEquals("note", com.foobnix.pdf.info.JsonHelper.fileToMap(notes).get("chapter"));
            Files.write(notes.toPath(), "{invalid".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(com.foobnix.pdf.info.JsonHelper.isValidMapFile(notes));
            com.foobnix.pdf.info.JsonHelper.mapToCacheFile(notes, complete);
            assertEquals("note", com.foobnix.pdf.info.JsonHelper.fileToMap(notes).get("chapter"));
            java.util.Map<String, String> newer = new java.util.HashMap<>();
            newer.put("chapter", "newer");
            com.foobnix.pdf.info.JsonHelper.mapToCacheFile(notes, newer);
            assertEquals("note", com.foobnix.pdf.info.JsonHelper.fileToMap(notes).get("chapter"));
            com.foobnix.pdf.info.JsonHelper.mapToFile(notes, newer);
            assertEquals("newer", com.foobnix.pdf.info.JsonHelper.fileToMap(notes).get("chapter"));
            assertEquals(0, root.listFiles((directory, name) -> name.endsWith(".part")).length);
        } finally {
            notes.delete(); root.delete();
        }
    }
    @Test public void evictionKeepsActiveOutputsAndBoundsInactiveConversions() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "converted-cache-").toFile();
        File old = new File(root, "old.epub");
        File oldNotes = new File(root, "old.epub.json");
        File oldFixed = new File(root, "old.epub-fixed.epub");
        File oldResources = new File(root, "old.epub-source");
        File activeDirectory = new File(root, "active-docx");
        File active = new File(activeDirectory, "book.html");
        File abandoned = new File(root, "unfinished.part");
        File newest = new File(root, "new.fb2");
        assertTrue(activeDirectory.mkdir());
        assertTrue(oldResources.mkdir());
        Files.write(old.toPath(), new byte[8]);
        Files.write(oldNotes.toPath(), new byte[4]);
        Files.write(oldFixed.toPath(), new byte[4]);
        Files.write(new File(oldResources, "image.png").toPath(), new byte[4]);
        File oldImage = new File(oldResources, "image.png");
        Files.write(active.toPath(), new byte[8]);
        Files.write(abandoned.toPath(), new byte[8]);
        Files.write(newest.toPath(), new byte[8]);
        old.setLastModified(1000); oldNotes.setLastModified(1000);
        oldFixed.setLastModified(1000); oldResources.setLastModified(1000);
        activeDirectory.setLastModified(2000);
        abandoned.setLastModified(3000); newest.setLastModified(4000);
        BookCacheLeases.registerImmutableRevisionNamedSource(old);
        BookCacheLeases.registerImmutableRevisionNamedSource(oldImage);
        BookCacheLeases.registerImmutableRevisionNamedSource(active);
        try (AutoCloseable lease = BookCacheLeases.acquire(active)) {
            CacheZipUtils.pruneBookCache(root, 16);
            assertFalse(old.exists());
            assertFalse("Eviction must remove the output's notes too", oldNotes.exists());
            assertFalse(oldFixed.exists());
            assertFalse(oldResources.exists());
            assertFalse(abandoned.exists());
            assertTrue(active.isFile());
            assertTrue(BookCacheLeases.isImmutableRevisionNamedSource(active));
            assertTrue(newest.isFile());
            Files.write(old.toPath(), new byte[]{9});
            assertTrue(oldResources.mkdir());
            Files.write(oldImage.toPath(), new byte[]{9});
            assertFalse("Evicted file path retained its immutable registration",
                    BookCacheLeases.isImmutableRevisionNamedSource(old));
            assertFalse("Evicted directory retained a descendant registration",
                    BookCacheLeases.isImmutableRevisionNamedSource(oldImage));
        } finally {
            CacheZipUtils.pruneBookCache(root, 0);
            assertFalse(activeDirectory.exists());
            assertFalse(newest.exists());
            root.delete();
        }
    }

    @Test public void explicitCacheClearWaitsForTheLastReaderLease() throws Exception {
        android.content.Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(target);
        File oldBookDirectory = CacheZipUtils.CACHE_BOOK_DIR;
        File root = Files.createTempDirectory(target.getCacheDir().toPath(), "explicit-book-clear-").toFile();
        File active = new File(root, "active.epub");
        File activeDirectory = new File(root, "active-html");
        File activeHtml = new File(activeDirectory, "book.html");
        File inactiveDirectory = new File(root, "inactive-docx");
        File inactiveChild = new File(inactiveDirectory, "book.html");
        assertTrue(activeDirectory.mkdir());
        assertTrue(inactiveDirectory.mkdir());
        Files.write(active.toPath(), new byte[]{1, 2, 3});
        Files.write(activeHtml.toPath(), new byte[]{4});
        Files.write(inactiveChild.toPath(), new byte[]{5});
        BookCacheLeases.registerImmutableRevisionNamedSource(activeHtml);
        BookCacheLeases.registerImmutableRevisionNamedSource(inactiveChild);
        try {
            CacheZipUtils.CACHE_BOOK_DIR = root;
            try (AutoCloseable lease = BookCacheLeases.acquire(active);
                 AutoCloseable directoryLease = BookCacheLeases.acquire(activeHtml)) {
                CacheZipUtils.emptyAllCacheDirs();
                assertTrue(active.isFile());
                assertTrue(activeHtml.isFile());
                assertTrue(BookCacheLeases.isImmutableRevisionNamedSource(activeHtml));
                assertFalse(inactiveDirectory.exists());
                assertTrue(inactiveDirectory.mkdir());
                Files.write(inactiveChild.toPath(), new byte[]{6});
                assertFalse("Explicit directory clear retained a descendant registration",
                        BookCacheLeases.isImmutableRevisionNamedSource(inactiveChild));
            }
            CacheZipUtils.emptyAllCacheDirs();
            assertFalse(active.exists());
            assertFalse(activeDirectory.exists());
        } finally {
            CacheZipUtils.CACHE_BOOK_DIR = oldBookDirectory;
            active.delete(); activeHtml.delete(); activeDirectory.delete();
            inactiveDirectory.delete(); root.delete();
        }
    }
}
