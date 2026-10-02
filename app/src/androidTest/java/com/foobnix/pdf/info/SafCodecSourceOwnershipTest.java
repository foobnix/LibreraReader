package com.foobnix.pdf.info;

import android.graphics.RectF;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.file.Files;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.Collections;
import java.util.List;
import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the same outer handoff used by direct, delegated and converted readers. */
public class SafCodecSourceOwnershipTest {
    private static class Context extends AbstractCodecContext {
        boolean fail;
        @Override public CodecDocument openDocumentInner(String path, String password) {
            return fail ? null : new Document(this);
        }
    }

    private static class Document extends AbstractCodecDocument {
        Document(Context context) { super(context, 1); }
        @Override public int getPageCount() { return 1; }
        @Override public int getPageCount(int width, int height, int fontSize) { return 1; }
        @Override public CodecPage getPageInner(int page) { return null; }
        @Override public List<? extends RectF> searchText(int page, String pattern) { return Collections.emptyList(); }
        @Override public List<String> getMediaAttachments() { return Collections.emptyList(); }
        @Override public BookType getBookType() { return BookType.PDF; }
        @Override public boolean hasChanges() { return false; }
        @Override public void deleteAnnotation(long page, int index) { }
        @Override public void saveAnnotations(String path) { }
        @Override public String documentToHtml() { return ""; }
        @Override public String getMeta(String option) { return ""; }
        @Override public void setMeta(String key, String value) { }
    }

    private File source() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "codec-source-").toFile();
        File file = new File(root, "source.zip");
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            zip.putNextEntry(new ZipEntry("book.fb2"));
            zip.write(new byte[]{1});
            zip.closeEntry();
        }
        return file;
    }

    @Test public void stagedSourceTransfersFromReservationToDocument() throws Exception {
        File file = source();
        BookCacheLeases.reserve(file);
        CodecDocument document = new Context().openDocument(file.getPath(), "");
        assertNotNull(document);
        assertFalse("Active document lost its source", BookCacheLeases.evict(file));
        document.recycle();
        assertTrue("Closed document left a source lease", BookCacheLeases.evict(file));
        assertTrue(file.getParentFile().delete());
    }

    @Test public void failedOpenReleasesTheStagedReservation() throws Exception {
        File file = source();
        BookCacheLeases.reserve(file);
        Context context = new Context();
        context.fail = true;
        assertNull(context.openDocument(file.getPath(), ""));
        assertTrue("Failed open pinned its source", BookCacheLeases.evict(file));
        assertTrue(file.getParentFile().delete());
    }
}
