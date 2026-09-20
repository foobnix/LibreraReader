package org.ebookdroid.droids;

import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.model.AppState;
import com.foobnix.sys.TempHolder;
import org.ebookdroid.core.codec.CodecDocument;
import org.junit.Test;
import org.junit.Assume;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

/** Opens disposable books through the same conversion and MuPDF handoff as the reader. */
public class ConvertedReaderLifetimeTest {
    @Test public void txtAndMobiConversionsSurviveUnrelatedBookOpens() throws Exception {
        boolean preformatted = AppState.get().isPreText;
        try {
            AppState.get().isPreText = true;
            assertConvertedReuse(TxtContext::new, "book.txt",
                    "A short text book.".getBytes(StandardCharsets.UTF_8));
            assertConvertedReuse(MobiContext::new, "book.mobi", assetBytes("book.mobi"));
            assertConvertedReuse(Fb2Context::new, "book.fb2", (
                    "<?xml version='1.0' encoding='UTF-8'?>"
                    + "<FictionBook xmlns='http://www.gribuser.ru/xml/fictionbook/2.0'>"
                    + "<description><title-info><genre>fiction</genre><author><first-name>A</first-name>"
                    + "<last-name>Writer</last-name></author><book-title>Fixture</book-title>"
                    + "<lang>en</lang></title-info></description><body><section>"
                    + "<p>Chapter text.</p></section></body></FictionBook>")
                    .getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream comic = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(comic)) {
                zip.putNextEntry(new ZipEntry("page.png"));
                zip.write(android.util.Base64.decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg==",
                        android.util.Base64.DEFAULT));
                zip.closeEntry();
            }
            assertConvertedReuse(CbrContext::new, "book.cbr", comic.toByteArray());
        } finally {
            AppState.get().isPreText = preformatted;
        }
    }

    @Test public void htmlSiblingImageRevisionInvalidatesConversionDirectory() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "html-image-revision-").toFile();
        File source = new File(root, "book.html"), image = new File(root, "cover.png");
        byte[] firstImage = android.util.Base64.decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg==",
                android.util.Base64.DEFAULT);
        byte[] revisedImage = java.util.Arrays.copyOf(firstImage, firstImage.length + 1);
        revisedImage[revisedImage.length - 1] = 7;
        Files.write(source.toPath(), "<html><body><img src='cover.png'>Text</body></html>"
                .getBytes(StandardCharsets.UTF_8));
        Files.write(image.toPath(), firstImage);
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String previousCss = BookCSS.get().customCSS2;
        boolean previousCancellation = TempHolder.get().loadingCancelled.getAndSet(false);
        BookCSS.get().customCSS2 = "";
        CodecDocument reader = null;
        File firstDirectory = null, secondDirectory = null;
        try {
            HtmlContext first = new HtmlContext();
            reader = first.openDocument(source.getPath(), ""); assertNotNull(reader);
            firstDirectory = first.cacheDirectory;
            assertArrayEquals(firstImage,
                    Files.readAllBytes(new File(firstDirectory, "cover.png").toPath()));
            reader.recycle(); reader = null;
            Files.write(image.toPath(), revisedImage);
            image.setLastModified(image.lastModified() + 5000);
            HtmlContext second = new HtmlContext();
            reader = second.openDocument(source.getPath(), ""); assertNotNull(reader);
            secondDirectory = second.cacheDirectory;
            assertNotEquals(firstDirectory, secondDirectory);
            assertArrayEquals(revisedImage,
                    Files.readAllBytes(new File(secondDirectory, "cover.png").toPath()));
        } finally {
            if (reader != null) reader.recycle();
            if (firstDirectory != null) BookCacheLeases.evictTree(firstDirectory);
            if (secondDirectory != null) BookCacheLeases.evictTree(secondDirectory);
            BookCSS.get().customCSS2 = previousCss;
            TempHolder.get().loadingCancelled.set(previousCancellation);
            source.delete(); image.delete(); root.delete();
        }
    }

    private void assertConvertedReuse(
            java.util.function.Supplier<org.ebookdroid.core.codec.AbstractCodecContext> factory,
            String name, byte[] content) throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "conversion-reuse-").toFile();
        File a = new File(root, "a-" + name), b = new File(root, "b-" + name);
        Files.write(a.toPath(), content); Files.write(b.toPath(), content);
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String previousCss = BookCSS.get().customCSS2;
        boolean previousCancellation = TempHolder.get().loadingCancelled.getAndSet(false);
        BookCSS.get().customCSS2 = "";
        CodecDocument reader = null;
        File firstOutput = null, otherOutput = null;
        try {
            org.ebookdroid.core.codec.AbstractCodecContext first = factory.get();
            reader = first.openDocument(a.getPath(), ""); assertNotNull(reader);
            firstOutput = conversionOutput(first); assertTrue(firstOutput.exists());
            long inode = android.system.Os.stat(firstOutput.getPath()).st_ino;
            reader.recycle(); reader = null;

            org.ebookdroid.core.codec.AbstractCodecContext other = factory.get();
            reader = other.openDocument(b.getPath(), ""); assertNotNull(reader);
            otherOutput = conversionOutput(other);
            reader.recycle(); reader = null;
            assertTrue("Opening B removed A's completed conversion", firstOutput.exists());

            org.ebookdroid.core.codec.AbstractCodecContext again = factory.get();
            reader = again.openDocument(a.getPath(), ""); assertNotNull(reader);
            assertEquals(firstOutput, conversionOutput(again));
            assertEquals("A was converted a second time", inode,
                    android.system.Os.stat(firstOutput.getPath()).st_ino);
        } finally {
            if (reader != null) reader.recycle();
            if (firstOutput != null) BookCacheLeases.evictTree(firstOutput);
            if (otherOutput != null) BookCacheLeases.evictTree(otherOutput);
            BookCSS.get().customCSS2 = previousCss;
            TempHolder.get().loadingCancelled.set(previousCancellation);
            a.delete(); b.delete(); root.delete();
        }
    }

    private static File conversionOutput(org.ebookdroid.core.codec.AbstractCodecContext context) {
        if (context instanceof TxtContext) return ((TxtContext) context).cacheDirectory;
        if (context instanceof MobiContext) return ((MobiContext) context).cacheFile;
        if (context instanceof Fb2Context) {
            File nominal = ((Fb2Context) context).cacheFile;
            return nominal.isFile() ? nominal
                    : new File(nominal.getParentFile(), nominal.getName() + "-fixed.epub");
        }
        if (context instanceof CbrContext) return ((CbrContext) context).cacheFile;
        throw new IllegalArgumentException(context.getClass().getName());
    }
    @Test public void txtEditAtSamePathCreatesNewConversionWhileOldReaderIsOpen() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "txt-revision-").toFile();
        File source = new File(root, "book.txt");
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String previousCss = BookCSS.get().customCSS2;
        boolean previousMode = AppState.get().isPreText;
        boolean previousCancellation = TempHolder.get().loadingCancelled.getAndSet(false);
        BookCSS.get().customCSS2 = "";
        AppState.get().isPreText = true;
        TxtContext first = new TxtContext(), second = new TxtContext();
        CodecDocument firstDocument = null, secondDocument = null;
        try {
            Files.write(source.toPath(), "First edition.".getBytes(StandardCharsets.UTF_8));
            assertTrue(source.setLastModified(2_000_000L));
            long oldSum = source.length() + source.lastModified();
            firstDocument = first.openDocument(source.getPath(), "");
            assertNotNull(firstDocument);
            File firstOutput = new File(first.cacheDirectory, "truetxt.html");
            assertTrue(firstOutput.isFile());
            Files.write(source.toPath(), "Second edition with more text.".getBytes(StandardCharsets.UTF_8));
            assertTrue(source.setLastModified(oldSum - source.length()));
            assertEquals("The old length+mtime key would collide", oldSum,
                    source.length() + source.lastModified());
            secondDocument = second.openDocument(source.getPath(), "");
            assertNotNull(secondDocument);
            assertNotEquals(first.cacheDirectory, second.cacheDirectory);
            assertTrue(new String(Files.readAllBytes(
                    new File(second.cacheDirectory, "truetxt.html").toPath()), StandardCharsets.UTF_8)
                    .contains("Second edition"));
            assertFalse(BookCacheLeases.evictTree(first.cacheDirectory));
        } finally {
            if (secondDocument != null) secondDocument.recycle();
            if (firstDocument != null) firstDocument.recycle();
            if (second.cacheDirectory != null) awaitEviction(second.cacheDirectory);
            if (first.cacheDirectory != null) awaitEviction(first.cacheDirectory);
            BookCSS.get().customCSS2 = previousCss;
            AppState.get().isPreText = previousMode;
            TempHolder.get().loadingCancelled.set(previousCancellation);
            awaitEviction(source);
            root.delete();
        }
    }

    @Test public void txtReaderKeepsBothConvertedModesUntilRecycle() throws Exception {
        boolean previous = AppState.get().isPreText;
        try {
            AppState.get().isPreText = true;
            checkConvertedReader(new TxtContext(), "book.txt", "A short text book.");
            AppState.get().isPreText = false;
            checkConvertedReader(new TxtContext(), "book.txt", "A short text book.");
        } finally {
            AppState.get().isPreText = previous;
        }
    }

    @Test public void htmlReaderKeepsConvertedOutputUntilRecycle() throws Exception {
        checkConvertedReader(new HtmlContext(), "book.html", "<html><body><p>Hello</p></body></html>");
    }

    @Test public void mhtReaderKeepsConvertedOutputUntilRecycle() throws Exception {
        checkConvertedReader(new MhtContext(), "book.mht",
                "MIME-Version: 1.0\nContent-Type: text/html\n\n<html><body>Hello from MHT</body></html>");
    }

    @Test public void markdownReaderKeepsConvertedOutputUntilRecycle() throws Exception {
        checkConvertedReader(new MdContext(), "book.md", "# Chapter\n\nA short markdown book.");
    }

    @Test public void cbrZipDelegationKeepsSourceUntilRecycle() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("page.png"));
            zip.write(android.util.Base64.decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL/nwAAAABJRU5ErkJggg==",
                    android.util.Base64.DEFAULT));
            zip.closeEntry();
        }
        checkConvertedReader(new CbrContext(), "book.cbr", bytes.toByteArray());
    }

    @Test public void cbrRarExtractionKeepsConvertedPagesUntilRecycle() throws Exception {
        Assume.assumeFalse("F-Droid ships a no-op RAR extractor", "fdroid".equals(AppsConfig.FLAVOR));
        // CC0 fixture: ssokolow/rar-test-files, build/testfile.rar3.cbr.
        checkConvertedReader(new CbrContext(), "book.cbr", assetBytes("testfile.rar3.cbr"));
    }

    @Test public void fdroidRarStubCannotPublishEmptyComicCache() throws Exception {
        Assume.assumeTrue("fdroid".equals(AppsConfig.FLAVOR));
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "fdroid-rar-").toFile();
        File source = new File(root, "book.cbr");
        Files.write(source.toPath(), assetBytes("testfile.rar3.cbr"));
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        boolean previousCancellation = TempHolder.get().loadingCancelled.getAndSet(false);
        CbrContext context = new CbrContext();
        try {
            assertNull(context.openDocument(source.getPath(), ""));
            assertNotNull(context.cacheFile);
            assertFalse(context.cacheFile.exists());
        } finally {
            TempHolder.get().loadingCancelled.set(previousCancellation);
            if (context.cacheFile != null) BookCacheLeases.evict(context.cacheFile);
            awaitEviction(source);
            root.delete();
        }
    }

    @Test public void docxReaderKeepsConvertedHtmlUntilRecycle() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zipEntry(zip, "[Content_Types].xml",
                    "<Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'>"
                    + "<Default Extension='rels' ContentType='application/vnd.openxmlformats-package.relationships+xml'/>"
                    + "<Default Extension='xml' ContentType='application/xml'/>"
                    + "<Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/>"
                    + "</Types>");
            zipEntry(zip, "_rels/.rels",
                    "<Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'>"
                    + "<Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/>"
                    + "</Relationships>");
            zipEntry(zip, "word/document.xml",
                    "<w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'>"
                    + "<w:body><w:p><w:r><w:t>Hello from DOCX</w:t></w:r></w:p></w:body></w:document>");
        }
        checkConvertedReader(new DocxContext(), "book.docx", bytes.toByteArray());
    }

    @Test public void odtReaderKeepsTranslatedHtmlUntilRecycle() throws Exception {
        checkConvertedReader(new OdtContext(), "book.odt", assetBytes("book.odt"));
    }

    @Test public void mobiAndAzw3KeepConvertedEpubUntilRecycle() throws Exception {
        checkConvertedReader(new MobiContext(), "book.mobi", assetBytes("book.mobi"));
        checkConvertedReader(new MobiContext(), "book.azw3", assetBytes("book.azw3"));
    }

    @Test public void processedEpubStaysProtectedUntilReaderCloses() throws Exception {
        boolean previous = AppState.get().isEnableTextReplacement;
        try {
            AppState.get().isEnableTextReplacement = true;
            checkConvertedReader(new EpubContext(), "book.epub", assetBytes("book.epub"));
        } finally {
            AppState.get().isEnableTextReplacement = previous;
        }
    }

    @Test public void docWrapperPublishesCompleteNativeOutputAndRetainsIt() throws Exception {
        checkConvertedReader(new DocContext((source, output) -> {
            try {
                Files.write(new File(output).toPath(),
                        "<html><body><p>Converted DOC</p></body></html>"
                                .getBytes(StandardCharsets.UTF_8));
                return 1;
            } catch (Exception failure) { throw new IllegalStateException(failure); }
        }), "book.doc", new byte[]{(byte) 0xd0, (byte) 0xcf, 0, 0});
    }

    @Test public void failedDocConversionDoesNotPublishPartialOutput() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "doc-failure-").toFile();
        File source = new File(root, "book.doc");
        Files.write(source.toPath(), new byte[]{(byte) 0xd0, (byte) 0xcf, 0, 0});
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        boolean previousCancellation = TempHolder.get().loadingCancelled.getAndSet(false);
        DocContext context = new DocContext((input, output) -> {
            try {
                Files.write(new File(output).toPath(), "partial".getBytes(StandardCharsets.UTF_8));
            } catch (Exception failure) { throw new IllegalStateException(failure); }
            throw new IllegalStateException("native conversion failed");
        });
        try {
            assertNull(context.openDocument(source.getPath(), ""));
            assertFalse(context.cacheFile.getParentFile().exists());
        } finally {
            TempHolder.get().loadingCancelled.set(previousCancellation);
            awaitEviction(source);
            root.delete();
        }
    }

    private static byte[] assetBytes(String name) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (java.io.InputStream input = InstrumentationRegistry.getInstrumentation()
                .getContext().getAssets().open("reader-fixtures/" + name)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    private static void zipEntry(ZipOutputStream zip, String name, String contents) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(contents.replace('\'', '"').getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void checkConvertedReader(org.ebookdroid.core.codec.AbstractCodecContext context,
                                      String name, String contents) throws Exception {
        checkConvertedReader(context, name, contents.getBytes(StandardCharsets.UTF_8));
    }

    private void checkConvertedReader(org.ebookdroid.core.codec.AbstractCodecContext context,
                                      String name, byte[] contents) throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "converted-reader-").toFile();
        File source = new File(root, name);
        Files.write(source.toPath(), contents);
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String previousCss = BookCSS.get().customCSS2;
        BookCSS.get().customCSS2 = "";
        boolean previousCancellation = TempHolder.get().loadingCancelled.getAndSet(false);
        CodecDocument document = null;
        File output = null;
        try {
            document = context.openDocument(source.getPath(), "");
            assertNotNull(name + " failed to open", document);
            assertFalse(BookCacheLeases.evict(source));
            output = context instanceof TxtContext ? ((TxtContext) context).cacheDirectory
                    : context instanceof HtmlContext ? ((HtmlContext) context).cacheDirectory
                    : context instanceof MhtContext ? ((MhtContext) context).cacheDirectory
                    : context instanceof MdContext ? ((MdContext) context).cacheDirectory
                    : context instanceof DocxContext ? ((DocxContext) context).cacheFile.getParentFile()
                    : context instanceof DocContext ? ((DocContext) context).cacheFile.getParentFile()
                    : context instanceof OdtContext ? ((OdtContext) context).cacheFile.getParentFile()
                    : context instanceof MobiContext ? ((MobiContext) context).cacheFile
                    : context instanceof EpubContext ? ((EpubContext) context).cacheFile
                    : context instanceof CbrContext ? ((CbrContext) context).cacheFile : null;
            assertNotNull("Test must locate converted output for " + name, output);
            assertTrue("Converted output must exist for " + name, output.exists());
            assertFalse("Active converted output was evicted for " + name,
                    BookCacheLeases.evictTree(output));
        } finally {
            if (document != null) document.recycle();
            BookCSS.get().customCSS2 = previousCss;
            TempHolder.get().loadingCancelled.set(previousCancellation);
            if (output != null) awaitEviction(output);
            awaitEviction(source);
            root.delete();
        }
    }

    private static void awaitEviction(File file) throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (!file.exists() || BookCacheLeases.evictTree(file)) return;
            Thread.sleep(50);
        }
        fail("File stayed pinned after its last consumer: " + file);
    }

    @Test public void rtfReaderKeepsConvertedHtmlAndSiblingDirectoryUntilRecycle() throws Exception {
        File root = Files.createTempDirectory(InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getCacheDir().toPath(), "rtf-reader-").toFile();
        File source = new File(root, "book.rtf");
        Files.write(source.toPath(), "{\\rtf1\\ansi Hello world.}".getBytes(StandardCharsets.UTF_8));
        CacheZipUtils.init(InstrumentationRegistry.getInstrumentation().getTargetContext());
        String previousCss = BookCSS.get().customCSS2;
        BookCSS.get().customCSS2 = "";
        RtfContext context = new RtfContext();
        CodecDocument document = null;
        try {
            assertTrue(source.isFile());
            document = context.openDocument(source.getPath(), "");
            assertNotNull("RTF conversion produced " + context.cacheFile + " (exists="
                    + (context.cacheFile != null && context.cacheFile.isFile()) + ")", document);
            assertTrue(context.cacheFile.isFile());
            assertFalse(BookCacheLeases.evictTree(context.cacheFile.getParentFile()));
        } finally {
            if (document != null) document.recycle();
            if (context.cacheFile != null) BookCacheLeases.evictTree(context.cacheFile.getParentFile());
            BookCSS.get().customCSS2 = previousCss;
            source.delete(); root.delete();
        }
        assertFalse(context.cacheFile.getParentFile().exists());
    }
}
