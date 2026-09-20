package org.ebookdroid.droids;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

public class ZipDelegationCacheTest {
    @Test public void singleFb2ArchiveStillDelegatesAndReusesItsConversion() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "zip-fb2-").toFile();
        File archive = new File(root, "book.zip");
        String fb2 = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                + "<description><title-info><genre>fiction</genre><author><first-name>A</first-name>"
                + "<last-name>Writer</last-name></author><book-title>Fixture</book-title>"
                + "<lang>en</lang></title-info></description><body><section>"
                + "<p>Chapter text.</p></section></body></FictionBook>";
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("book.fb2"));
            output.write(fb2.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String css = BookCSS.get().customCSS2;
        BookCSS.get().customCSS2 = "";
        boolean cancelled = TempHolder.get().loadingCancelled.getAndSet(false);
        CodecDocument reader = null;
        try {
            reader = new ZipContext().openDocument(archive.getPath(), "");
            assertNotNull(reader);
            File first = output(reader);
            assertTrue(first.isFile());
            reader.recycle(); reader = null;
            reader = new ZipContext().openDocument(archive.getPath(), "");
            assertNotNull(reader);
            assertEquals(first, output(reader));
        } finally {
            if (reader != null) reader.recycle();
            BookCSS.get().customCSS2 = css;
            TempHolder.get().loadingCancelled.set(cancelled);
            archive.delete(); root.delete();
        }
    }

    @Test public void aSingleHtmlDocumentWithImageSiblingStillSelectsTheDocument() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "zip-sibling-").toFile();
        File archive = new File(root, "book.zip");
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("book.html"));
            output.write("<html><body><p>Chapter with image</p><img src='cover.png'></body></html>"
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new ZipEntry("cover.png"));
            output.write(new byte[]{1, 2, 3});
            output.closeEntry();
        }
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String css = BookCSS.get().customCSS2;
        BookCSS.get().customCSS2 = "";
        boolean cancelled = TempHolder.get().loadingCancelled.getAndSet(false);
        assertTrue(CacheZipUtils.isSingleAndSupportEntryInner(archive.getPath()).first);
        assertEquals("book.html", CacheZipUtils.isSingleAndSupportEntryInner(archive.getPath()).second);
        CodecDocument reader = null;
        try {
            reader = new ZipContext().openDocument(archive.getPath(), "");
            assertNotNull(reader);
            assertTrue(archiveContainsText(output(reader), "Chapter with image"));
        } finally {
            if (reader != null) reader.recycle();
            BookCSS.get().customCSS2 = css;
            TempHolder.get().loadingCancelled.set(cancelled);
            archive.delete(); root.delete();
        }
    }

    @Test public void sameMemberNameInDifferentArchivesNeverReusesTheOtherConversion() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "zip-delegation-").toFile();
        File a = new File(root, "a.zip"), b = new File(root, "b.zip");
        zipText(a, "Alpha chapter"); zipText(b, "Bravo chapter");
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        boolean preformatted = AppState.get().isPreText;
        String css = BookCSS.get().customCSS2;
        boolean cancelled = TempHolder.get().loadingCancelled.getAndSet(false);
        AppState.get().isPreText = true;
        BookCSS.get().customCSS2 = "";
        CodecDocument reader = null;
        try {
            reader = new ZipContext().openDocument(a.getPath(), "");
            assertNotNull(reader);
            File first = output(reader);
            assertTrue(archiveContainsText(first, "Alpha chapter"));
            reader.recycle(); reader = null;

            reader = new ZipContext().openDocument(b.getPath(), "");
            assertNotNull(reader);
            File second = output(reader);
            assertNotEquals(first, second);
            assertTrue(archiveContainsText(second, "Bravo chapter"));
            reader.recycle(); reader = null;

            reader = new ZipContext().openDocument(a.getPath(), "");
            assertNotNull(reader);
            File again = output(reader);
            assertEquals("Unchanged archive member should select its previous conversion", first, again);
            assertTrue(archiveContainsText(again, "Alpha chapter"));
            reader.recycle(); reader = null;

            zipText(a, "Changed Alpha chapter, longer");
            assertTrue(a.setLastModified(a.lastModified() + 2000));
            reader = new ZipContext().openDocument(a.getPath(), "");
            assertNotNull(reader);
            File revised = output(reader);
            assertNotEquals("Archive revision must invalidate delegated output", first, revised);
            assertTrue(archiveContainsText(revised, "Changed Alpha chapter"));
        } finally {
            if (reader != null) reader.recycle();
            AppState.get().isPreText = preformatted;
            BookCSS.get().customCSS2 = css;
            TempHolder.get().loadingCancelled.set(cancelled);
            a.delete(); b.delete(); root.delete();
        }
    }

    private static File output(CodecDocument reader) throws Exception {
        java.lang.reflect.Field path = MuPdfDocument.class.getDeclaredField("fname");
        path.setAccessible(true);
        return new File((String) path.get(reader));
    }

    private static boolean archiveContainsText(File archive, String expected) throws Exception {
        String converted = new String(Files.readAllBytes(archive.toPath()), StandardCharsets.UTF_8)
                .replace("&shy;", "").replace("\u00ad", "");
        assertTrue("Converted " + archive + " content: "
                + converted.substring(0, Math.min(converted.length(), 600)),
                converted.contains(expected));
        return true;
    }

    private static void zipText(File destination, String text) throws Exception {
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(destination))) {
            output.putNextEntry(new ZipEntry("book.txt"));
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
