package org.ebookdroid.droids;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EpubProcessingSettings;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConversionCacheCollisionTest {
    @Test public void everyFilenameConverterSeparatesLegacyHashCollisions() {
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String first = "/Books/Aa|revision" + EpubProcessingSettings.key();
        String second = "/Books/BB|revision" + EpubProcessingSettings.key();
        assertEquals(first.hashCode(), second.hashCode());
        for (AbstractCodecContext codec : new AbstractCodecContext[]{new EpubContext(), new Fb2Context(),
                new MobiContext(), new DocContext(), new DocxContext(), new OdtContext(), new RtfContext(), new CbrContext()}) {
            java.io.File one = codec.getCacheFileName(first), two = codec.getCacheFileName(second);
            assertNotEquals(codec.getClass().getSimpleName(), one, two);
        }
    }
    @Test public void collidingBookNamesKeepTheirOwnRetainedConvertedContents() throws Exception {
        android.content.Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(target);
        java.io.File root = java.nio.file.Files.createTempDirectory(target.getCacheDir().toPath(), "collision-books-").toFile();
        java.io.File first = new java.io.File(root, "Aa.rtf"), second = new java.io.File(root, "BB.rtf");
        java.nio.file.Files.write(first.toPath(), "{\\rtf1\\ansi Orange}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.write(second.toPath(), "{\\rtf1\\ansi Purple}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(first.setLastModified(200000)); assertTrue(second.setLastModified(200000));
        assertEquals((first.getPath() + AbstractCodecContext.sourceRevisionKey(first.getPath())).hashCode(),
                (second.getPath() + AbstractCodecContext.sourceRevisionKey(second.getPath())).hashCode());
        boolean cancelled = com.foobnix.sys.TempHolder.get().loadingCancelled.getAndSet(false);
        boolean hyphens = com.foobnix.pdf.info.model.BookCSS.get().isAutoHypens;
        com.foobnix.pdf.info.model.BookCSS.get().isAutoHypens = false;
        String css = com.foobnix.pdf.info.model.BookCSS.get().customCSS2;
        com.foobnix.pdf.info.model.BookCSS.get().customCSS2 = "";
        RtfContext one = new RtfContext(), two = new RtfContext();
        org.ebookdroid.core.codec.CodecDocument firstReader = null, secondReader = null;
        try {
            firstReader = one.openDocument(first.getPath(), "");
            secondReader = two.openDocument(second.getPath(), "");
            assertNotNull(firstReader); assertNotNull(secondReader);
            assertTrue(firstReader.getPageCount() > 0); assertTrue(secondReader.getPageCount() > 0);
            String firstHtml = new String(java.nio.file.Files.readAllBytes(one.cacheFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            String secondHtml = new String(java.nio.file.Files.readAllBytes(two.cacheFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(firstHtml.contains("Orange")); assertFalse(firstHtml.contains("Purple"));
            assertTrue(secondHtml.contains("Purple")); assertFalse(secondHtml.contains("Orange"));
        } finally {
            if (firstReader != null) firstReader.recycle();
            if (secondReader != null) secondReader.recycle();
            if (one.cacheFile != null) com.foobnix.pdf.info.BookCacheLeases.evictTree(one.cacheFile.getParentFile());
            if (two.cacheFile != null) com.foobnix.pdf.info.BookCacheLeases.evictTree(two.cacheFile.getParentFile());
            com.foobnix.sys.TempHolder.get().loadingCancelled.set(cancelled);
            com.foobnix.pdf.info.model.BookCSS.get().customCSS2 = css;
            com.foobnix.pdf.info.model.BookCSS.get().isAutoHypens = hyphens;
            first.delete(); second.delete(); root.delete();
        }
    }

}
