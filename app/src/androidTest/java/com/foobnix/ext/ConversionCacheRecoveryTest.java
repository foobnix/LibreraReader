package com.foobnix.ext;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordRequiredException;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import static org.junit.Assert.*;

public class ConversionCacheRecoveryTest {
    private File oldRoot, root;
    private boolean cancelled;
    private String css;
    @Before public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(context);
        oldRoot = CacheZipUtils.CACHE_BOOK_DIR;
        root = Files.createTempDirectory(context.getCacheDir().toPath(), "conversion-recovery-").toFile();
        CacheZipUtils.CACHE_BOOK_DIR = root;
        css = BookCSS.get().customCSS2; BookCSS.get().customCSS2 = "";
        cancelled = TempHolder.get().loadingCancelled.getAndSet(false);
    }
    @After public void tearDown() {
        CacheZipUtils.CACHE_BOOK_DIR = oldRoot;
        BookCacheLeases.evictTree(root);
        TempHolder.get().loadingCancelled.set(cancelled);
        BookCSS.get().customCSS2 = css;
    }
    private File bytes(String name) throws Exception {
        File file = new File(root, name); Files.write(file.toPath(), new byte[]{1, 2, 3}); return file;
    }
    @Test public void invalidationRemovesOnlyTheFailedUnitAndItsCompanions() throws Exception {
        File bad = bytes("bad.epub"), notes = bytes("bad.epub.json"), corrected = bytes("bad.epub-fixed.epub"), healthy = bytes("healthy.epub");
        ConversionCache.invalidate(bad, new IllegalArgumentException("unusable"));
        try (BookCacheLeases.PublishedFile rebuilt = ConversionCache.buildFile(bad,
                temporary -> Files.write(temporary.toPath(), new byte[]{9}))) {
            assertArrayEquals(new byte[]{9}, Files.readAllBytes(rebuilt.file.toPath()));
            assertFalse(notes.exists()); assertFalse(corrected.exists()); assertTrue(healthy.exists());
        }
    }
    @Test public void activeReadersFinishBeforeARejectedUnitIsRebuilt() throws Exception {
        File bad = bytes("book.epub"), notes = bytes("book.epub.json");
        try (AutoCloseable reader = BookCacheLeases.acquire(bad)) {
            ConversionCache.invalidate(bad, null);
            assertThrows(IllegalStateException.class, () -> ConversionCache.prepare(bad));
            assertTrue(bad.exists()); assertTrue(notes.exists());
        }
        ConversionCache.prepare(bad);
        assertFalse(bad.exists()); assertFalse(notes.exists());
    }
    @Test public void persistedFailureMarkerIsHonoredWithoutAnInMemoryInvalidation() throws Exception {
        File bad = bytes("restart.epub"); bytes("restart.epub.invalid");
        ConversionCache.prepare(bad); assertFalse(bad.exists());
        assertFalse(new File(root, "restart.epub.invalid").exists());
    }
    @Test public void passwordAndCancellationNeverInvalidateCompletedConversions() throws Exception {
        File book = bytes("private.epub");
        ConversionCache.invalidate(book, new IllegalStateException(new MuPdfPasswordRequiredException()));
        ConversionCache.prepare(book); assertTrue(book.exists());
        ConversionCache.invalidate(book, new CancellationException());
        ConversionCache.prepare(book); assertTrue(book.exists());
        TempHolder.get().loadingCancelled.set(true);
        ConversionCache.invalidate(book, new IllegalArgumentException());
        TempHolder.get().loadingCancelled.set(false);
        ConversionCache.prepare(book); assertTrue(book.exists());
    }
    @Test public void originalBooksOutsideTheConversionDirectoryAreNeverDeleted() throws Exception {
        File original = File.createTempFile("original-", ".epub", root.getParentFile());
        try {
            ConversionCache.invalidate(original, new IllegalArgumentException());
            ConversionCache.prepare(original); assertTrue(original.exists());
        } finally { original.delete(); }
    }
    @Test public void aNativeOpenExceptionRejectsThePublishedConversion() throws Exception {
        File invalid = new File(root, UUID.randomUUID() + ".epub");
        Files.write(invalid.toPath(), "not an EPUB archive".getBytes(StandardCharsets.UTF_8));
        assertThrows(RuntimeException.class, () -> new MuPdfDocument(new PdfContext(), MuPdfDocument.FORMAT_PDF, invalid.getPath(), ""));
        assertTrue(new File(invalid.getPath() + ".invalid").isFile());
        ConversionCache.prepare(invalid); assertFalse(invalid.exists());
    }
    @Test public void zeroPagesRejectTheConversionWithoutEvictingAnActiveReader() throws Exception {
        File empty = new File(root, UUID.randomUUID() + ".pdf");
        Files.write(empty.toPath(), ("%PDF-1.4\n1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Count 0 /Kids [] >> endobj\n"
                + "trailer << /Root 1 0 R >>\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
        MuPdfDocument document = new MuPdfDocument(new PdfContext(), MuPdfDocument.FORMAT_PDF, empty.getPath(), "");
        try {
            assertEquals(0, document.getPageCount());
            assertTrue(new File(empty.getPath() + ".invalid").isFile());
            assertThrows(IllegalStateException.class, () -> ConversionCache.prepare(empty));
            assertTrue(empty.exists());
        } finally { document.recycle(); }
        ConversionCache.prepare(empty); assertFalse(empty.exists());
    }
    @Test public void retainedConversionsUseInternalAppStorage() {
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        assertEquals(new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "Book"), CacheZipUtils.CACHE_BOOK_DIR);
    }
}
