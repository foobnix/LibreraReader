package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.foliate.FoliateHostActivity;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * {@link FontSizes#normalizeEpub(File)} on a book shaped like a converted Microsoft Reader one:
 * Word's HTML, every line of text in a span at {@code font-size:12.0pt}, which foliate would draw
 * at 12pt whatever the reader's font size.
 */
@RunWith(AndroidJUnit4.class)
public class FontSizesDeviceTest {

    private static final String SENTENCE = "The sample speaks of beginnings and of carefully"
            + " numbered pages, written out at length so that the engine has something to paginate.";

    private ActivityScenario<FoliateHostActivity> host;

    private File dir;

    private int engineBefore;

    private int sizeBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);
        host = ActivityScenario.launch(FoliateHostActivity.class);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        engineBefore = AppState.get().epubEngine;
        sizeBefore = BookCSS.get().fontSizeSpFoliate;
        dir = new File(context.getCacheDir(), "font-sizes-test");
        dir.mkdirs();
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        BookCSS.get().fontSizeSpFoliate = sizeBefore;
        final File[] files = dir.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        dir.delete();
        if (host != null) {
            host.close();
        }
    }

    @Test
    public void wordsPointSizesBecomeTheReadersSize() throws Exception {
        final File epub = book("word.epub");
        assertEquals("12pt, the body text, is the base", 16f, FontSizes.normalizeEpub(epub), 0.01f);

        final ZipFile zip = new ZipFile(epub);
        try {
            assertEquals("the mimetype stays first", "mimetype", zip.entries().nextElement().getName());
            final String page = read(zip, "OEBPS/text.xhtml");
            assertFalse("no point size is left: " + page, page.matches("(?s).*font-size:\\s*[\\d.]+pt.*"));
            assertTrue("the text is the reader's size", page.contains("style=\"font-size:1rem;font-family:&quot;Bookman Old Style&quot;\""));
            assertTrue("the heading keeps its proportion", page.contains("font-size:1.167rem"));
            assertTrue("the style block too", page.contains("font-size:1rem;font-family:\"Times New Roman\""));
            assertTrue("<font size> becomes a style: " + page, page.contains("<font style=\"font-size:1.5rem\" color=\"red\">"));
            final DocumentBuilderFactory xml = DocumentBuilderFactory.newInstance();
            xml.setNamespaceAware(true);
            xml.newDocumentBuilder().parse(zip.getInputStream(zip.getEntry("OEBPS/text.xhtml")));
        } finally {
            zip.close();
        }

        // A book with relative sizes only is not touched
        final File plain = new File(dir, "plain.epub");
        write(plain, "<p style=\"font-size:1.1em\">" + SENTENCE + "</p>", false);
        final long before = plain.length();
        assertEquals(0f, FontSizes.normalizeEpub(plain), 0f);
        assertEquals(before, plain.length());
    }

    /** foliate lays the text out at the reader's size: twice the size, many more pages. */
    @Test
    public void foliateTextFollowsTheFontSize() throws Exception {
        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
        final File epub = book("pages.epub");
        FontSizes.normalizeEpub(epub);
        BookCSS.get().fontSizeSpFoliate = 16;
        final int small = pagesOf(epub);
        BookCSS.get().fontSizeSpFoliate = 32;
        final int large = pagesOf(epub);
        assertTrue("twice the font size, many more pages: " + small + " -> " + large, large > small * 1.6);
    }

    private int pagesOf(final File epub) {
        final CodecDocument document = BookType.getCodecContextByPath(epub.getPath()).openDocument(epub.getPath(), "");
        assertNotNull(document);
        try {
            assertTrue("foliate laid it out: " + document, document instanceof FoliateDocument);
            return document.getPageCount(1080, 1800, BookCSS.get().fontSizeSp);
        } finally {
            document.recycle();
        }
    }

    // ---------------------------------------------------------------- the book

    private File book(final String name) throws Exception {
        final StringBuilder body = new StringBuilder();
        body.append("<p class=\"MsoNormal\"><span style=\"font-size:14.0pt;font-family:&quot;Bookman Old Style&quot;\"><b>The Heading</b></span></p>");
        body.append("<p><font size=\"5\" color=\"red\">A larger line</font></p>");
        for (int i = 1; i <= 60; i++) {
            body.append("<p class=\"MsoNormal\"><span style=\"font-size:12.0pt;font-family:&quot;Bookman Old Style&quot;\">Paragraph ")
                    .append(i).append(". ").append(SENTENCE).append("</span></p>");
        }
        final File epub = new File(dir, name);
        write(epub, body.toString(), true);
        return epub;
    }

    private static void write(final File epub, final String body, final boolean word) throws Exception {
        final String page = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>Word</title>"
                + (word ? "<style>p.MsoNormal{margin:0in;font-size:12.0pt;font-family:\"Times New Roman\";}</style>" : "")
                + "</head><body>" + body + "</body></html>";
        final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(epub));
        try {
            final byte[] mimetype = "application/epub+zip".getBytes(StandardCharsets.UTF_8);
            final ZipEntry m = new ZipEntry("mimetype");
            m.setMethod(ZipEntry.STORED);
            m.setSize(mimetype.length);
            m.setCompressedSize(mimetype.length);
            final CRC32 crc = new CRC32();
            crc.update(mimetype);
            m.setCrc(crc.getValue());
            zip.putNextEntry(m);
            zip.write(mimetype);
            put(zip, "META-INF/container.xml", "<?xml version=\"1.0\"?><container version=\"1.0\""
                    + " xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"><rootfiles>"
                    + "<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>"
                    + "</rootfiles></container>");
            put(zip, "OEBPS/content.opf", "<?xml version=\"1.0\"?><package xmlns=\"http://www.idpf.org/2007/opf\""
                    + " version=\"2.0\" unique-identifier=\"id\"><metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
                    + "<dc:identifier id=\"id\">font-sizes-test</dc:identifier><dc:title>Word</dc:title>"
                    + "<dc:language>en</dc:language></metadata><manifest>"
                    + "<item id=\"t\" href=\"text.xhtml\" media-type=\"application/xhtml+xml\"/></manifest>"
                    + "<spine><itemref idref=\"t\"/></spine></package>");
            put(zip, "OEBPS/text.xhtml", page);
        } finally {
            zip.close();
        }
    }

    private static void put(final ZipOutputStream zip, final String name, final String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String read(final ZipFile zip, final String name) throws Exception {
        final InputStream in = zip.getInputStream(zip.getEntry(name));
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }
}
