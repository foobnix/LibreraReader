package org.ebookdroid.droids.chm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.chm.ChmToEpub;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Engines;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.droids.ChmContext;
import org.ebookdroid.droids.foliate.FoliateHostActivity;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * .chm books on a device: converted to an EPUB by {@link ChmToEpub} and read from it by
 * whichever engine the build has - MuPDF, or foliate-js alone in the JS flavours.
 * <p>
 * The book is written by the test itself ({@link ChmSample}, {@link ChmWriter}) as a real CHM:
 * a directory, a stored section and an LZX-compressed one that spans several frames and resets.
 */
@RunWith(AndroidJUnit4.class)
public class ChmDeviceTest {

    private static final int PAGE_WIDTH = 1080;

    private static final int PAGE_HEIGHT = 1800;

    private ActivityScenario<FoliateHostActivity> host;

    private File dir;

    private int engineBefore;

    private boolean hypensBefore;

    private boolean logBefore;

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        logBefore = AppsConfig.IS_LOG;
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        host = ActivityScenario.launch(FoliateHostActivity.class);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        engineBefore = AppState.get().epubEngine;
        hypensBefore = BookCSS.get().isAutoHypens;
        BookCSS.get().isAutoHypens = false;

        dir = new File(context.getCacheDir(), "chm-test");
        delete(dir);
        dir.mkdirs();
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        BookCSS.get().isAutoHypens = hypensBefore;
        AppsConfig.IS_LOG = logBefore;
        if (dir != null) {
            delete(dir);
        }
        if (host != null) {
            host.close();
        }
    }

    // ---------------------------------------------------------------- the conversion

    @Test
    public void theEpubHoldsThePagesInOrderWithTheContentsAndThePicture() throws Exception {
        final File chm = sample();
        final File epub = new File(dir, "sample.epub");
        final ChmToEpub.Result result = ChmToEpub.convert(chm, epub);

        assertEquals(ChmSample.TITLE, result.title);
        assertEquals("every page, the orphan last: " + result.pages, 5, result.pages);
        assertEquals("the picture and the stylesheet", 2, result.resources);

        // The contents of the .hhc, nesting and all
        assertEquals(4, result.toc.size());
        assertEquals("Introduction", result.toc.get(0).title);
        assertEquals("Chapter One", result.toc.get(1).title);
        assertEquals("Section 1.1", result.toc.get(1).children.get(0).title);
        assertTrue("the section lands on its anchor: " + result.toc.get(1).children.get(0).href,
                result.toc.get(1).children.get(0).href.endsWith("#sec11"));
        assertEquals("Chapter Two", result.toc.get(2).title);
        // A heading that opens no page opens its first child's
        assertEquals("Appendices", result.toc.get(3).title);
        assertEquals(result.toc.get(3).children.get(0).href, result.toc.get(3).href);

        final ZipFile zip = new ZipFile(epub);
        try {
            final List<String> spine = spine(read(zip, "OEBPS/content.opf"));
            assertEquals(5, spine.size());
            final String[] order = {ChmSample.WELCOME, ChmSample.CHAPTER_ONE, ChmSample.CHAPTER_TWO,
                    ChmSample.APPENDIX, ChmSample.ORPHAN};
            final DocumentBuilderFactory xml = DocumentBuilderFactory.newInstance();
            xml.setNamespaceAware(true);
            for (int i = 0; i < spine.size(); i++) {
                final String page = read(zip, "OEBPS/" + spine.get(i));
                assertTrue("page " + i + " is " + order[i] + ": " + page, page.contains(order[i]));
                // foliate parses a section as XML: one stray entity and the page is blank
                xml.newDocumentBuilder().parse(zip.getInputStream(zip.getEntry("OEBPS/" + spine.get(i))));
                assertFalse("no script survives", page.contains("<script"));
                assertFalse("no named entity survives", page.contains("&nbsp;"));
            }
            xml.newDocumentBuilder().parse(zip.getInputStream(zip.getEntry("OEBPS/toc.ncx")));
            xml.newDocumentBuilder().parse(zip.getInputStream(zip.getEntry("OEBPS/nav.xhtml")));

            final String chapterOne = read(zip, "OEBPS/" + spine.get(1));
            assertTrue("the section's anchor is an id", chapterOne.contains("id=\"sec11\""));
            assertTrue("the ms-its: link goes to chapter two", chapterOne.contains("href=\"" + spine.get(2) + "\""));
            assertTrue(chapterOne.contains(ChmSample.SECTION));

            final String title = read(zip, "OEBPS/" + spine.get(0));
            final String picture = attribute(title, "<img", "src");
            assertNotNull("the title page keeps its picture: " + title, picture);
            final ZipEntry png = zip.getEntry("OEBPS/" + picture);
            assertNotNull("and the picture is in the book: " + picture, png);
            assertTrue("the same bytes as in the CHM",
                    java.util.Arrays.equals(ChmSample.png(), bytes(zip.getInputStream(png))));
        } finally {
            zip.close();
        }
    }

    /**
     * Help files pin their text in points, pixels and keywords. Those become rem with the body
     * text at 1rem, so that the text follows the reader's font size and a heading keeps its
     * proportion to it.
     */
    @Test
    public void absoluteFontSizesBecomeTheReadersSize() throws Exception {
        final File epub = new File(dir, "sizes.epub");
        final ChmToEpub.Result result = ChmToEpub.convert(sample(), epub);
        assertEquals("the body text's 9pt is the base", 12f, result.basePx, 0.01f);

        final ZipFile zip = new ZipFile(epub);
        try {
            String css = null;
            for (java.util.Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                final ZipEntry entry = e.nextElement();
                if (entry.getName().endsWith(".css")) {
                    css = read(zip, entry.getName());
                }
            }
            assertNotNull(css);
            assertTrue("the body text is the reader's size: " + css,
                    css.contains("font-size: 1rem; line-height: 1.167;"));
            assertTrue("the heading twice that, in its shorthand: " + css,
                    css.contains("font: bold 2rem/1.333 Arial"));
            assertTrue("the body's 11px: " + css, css.contains("font-size: 0.917rem"));
            assertTrue("a keyword too: " + css, css.contains("font-size: 0.833rem"));
            assertFalse("no absolute size is left: " + css, css.matches("(?s).*font-size:\\s*[\\d.]+(px|pt).*"));

            final String title = read(zip, "OEBPS/" + spine(read(zip, "OEBPS/content.opf")).get(0));
            assertTrue("<font size> becomes a size in rem: " + title, title.contains("font-size:1.5rem"));
            assertFalse(title.contains("size=\"4\""));
        } finally {
            zip.close();
        }
    }

    /** With foliate the text follows the reader's font size: twice the size, more pages. */
    @Test
    public void foliateTextFollowsTheFontSize() throws Exception {
        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
        final int sizeBefore = BookCSS.get().fontSizeSpFoliate;
        try {
            final File chm = sample();
            BookCSS.get().fontSizeSpFoliate = 16;
            final int small = pagesAt(chm);
            BookCSS.get().fontSizeSpFoliate = 32;
            final int large = pagesAt(chm);
            assertTrue("twice the font size, many more pages: " + small + " -> " + large,
                    large > small * 1.6);
        } finally {
            BookCSS.get().fontSizeSpFoliate = sizeBefore;
        }
    }

    private int pagesAt(final File chm) {
        final CodecDocument document = open(chm);
        try {
            return document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
        } finally {
            document.recycle();
        }
    }

    // ---------------------------------------------------------------- opening it

    /** foliate-js, the only engine of the JS flavours and the other one where MuPDF is. */
    @Test
    public void itOpensInFoliateWithItsContentsTextAndPicture() throws Exception {
        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
        opensWithItsContentsTextAndPicture();
    }

    @Test
    public void itOpensInMuPdfWithItsContentsTextAndPicture() throws Exception {
        Assume.assumeTrue("this build has no MuPDF", Engines.IS_MUPDF);
        AppState.get().epubEngine = AppState.EPUB_ENGINE_MUPDF;
        opensWithItsContentsTextAndPicture();
    }

    private void opensWithItsContentsTextAndPicture() throws Exception {
        final File chm = sample();
        final CodecDocument document = open(chm);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            assertTrue("it paginated, got " + pages, pages >= 3);

            final List<OutlineLink> outline = document.getOutline();
            assertNotNull(outline);
            final List<String> titles = new ArrayList<String>();
            for (OutlineLink link : outline) {
                titles.add(link.getTitle() == null ? "" : link.getTitle().trim());
            }
            assertTrue("the contents came along: " + titles, titles.contains("Introduction"));
            assertTrue("the contents came along: " + titles, titles.contains("Chapter Two"));
            assertTrue("and their nesting: " + titles, titles.contains("Section 1.1"));

            boolean second = false;
            for (int i = 0; i < pages && !second; i++) {
                final String html = document.getPage(i).getPageHTML();
                second = html != null && html.contains(ChmSample.CHAPTER_TWO);
            }
            assertTrue("chapter two's text is on a page", second);

            // The title page holds a big red square: MuPDF draws it under the text, foliate gives
            // a picture that size a page of its own, so it is looked for on the first pages
            int red = 0;
            for (int pg = 0; pg < Math.min(3, pages) && red <= 200; pg++) {
                final Bitmap bitmap = render(document, pg);
                red = 0;
                for (int x = 0; x < bitmap.getWidth(); x += 4) {
                    for (int y = 0; y < bitmap.getHeight(); y += 4) {
                        final int pixel = bitmap.getPixel(x, y);
                        if (Color.red(pixel) > 200 && Color.green(pixel) < 80 && Color.blue(pixel) < 80) {
                            red++;
                        }
                    }
                }
            }
            assertTrue("the picture was drawn, red pixels: " + red, red > 200);
        } finally {
            document.recycle();
        }
    }

    /** The converted book is kept: the second open reads it back instead of converting again. */
    @Test
    public void aSecondOpenReadsTheCachedEpub() throws Exception {
        // Debug logging is what makes the MOBI family convert on every open
        AppsConfig.IS_LOG = false;
        final File chm = sample();

        CodecDocument document = open(chm);
        final File cached;
        try {
            assertTrue(document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp) >= 1);
            cached = epubOf(document);
        } finally {
            document.recycle();
        }
        assertTrue("the book is cached: " + cached, cached != null && cached.isFile());
        final long converted = cached.lastModified();
        Thread.sleep(1100);

        document = open(chm);
        try {
            assertTrue(document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp) >= 1);
            assertEquals("it opened the same file", cached.getPath(), epubOf(document).getPath());
        } finally {
            document.recycle();
        }
        assertTrue("and it is still there", cached.isFile());
        assertEquals("it was not converted again", converted, cached.lastModified());
    }

    // ---------------------------------------------------------------- helpers

    private File sample() throws Exception {
        final File chm = new File(dir, "sample.chm");
        final FileOutputStream out = new FileOutputStream(chm);
        try {
            out.write(ChmSample.build());
        } finally {
            out.close();
        }
        return chm;
    }

    private CodecDocument open(final File file) {
        final CodecContext context = BookType.getCodecContextByPath(file.getPath());
        assertNotNull("there is a context for " + file.getName(), context);
        assertEquals("the extension is read by its own context", ChmContext.class, context.getClass());

        final CodecDocument document = context.openDocument(file.getPath(), "");
        assertNotNull("the book was opened: " + file.getName(), document);
        if (AppState.get().getEpubEngine() == AppState.EPUB_ENGINE_FOLIATE) {
            assertTrue("foliate laid it out: " + document, document instanceof FoliateDocument);
        }
        return document;
    }

    /** The EPUB the engine was handed. */
    private static File epubOf(final CodecDocument document) {
        if (document instanceof FoliateDocument) {
            final String path = ((FoliateDocument) document).getPath();
            assertTrue("foliate was handed an archive: " + path, path.toLowerCase(Locale.US).endsWith(".epub"));
            return new File(path);
        }
        // MuPDF's document does not say what it opened: the cache folder holds just this book
        File newest = null;
        final File[] files = CacheZipUtils.CACHE_BOOK_DIR.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            if (files[i].getName().endsWith(".epub")
                    && (newest == null || files[i].lastModified() > newest.lastModified())) {
                newest = files[i];
            }
        }
        return newest;
    }

    private Bitmap render(final CodecDocument document, final int index) {
        final CodecPage page = document.getPage(index);
        assertNotNull(page);
        final BitmapRef ref = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
        assertNotNull("the page was rendered", ref);
        return ref.getBitmap();
    }

    private static List<String> spine(final String opf) {
        final List<String> ids = new ArrayList<String>();
        int at = 0;
        while ((at = opf.indexOf("<itemref idref=\"", at)) >= 0) {
            at += 16;
            ids.add(opf.substring(at, opf.indexOf('"', at)));
        }
        final List<String> hrefs = new ArrayList<String>();
        for (String id : ids) {
            final String item = opf.substring(opf.indexOf("<item id=\"" + id + "\""));
            hrefs.add(attribute(item, "<item", "href"));
        }
        return hrefs;
    }

    private static String attribute(final String text, final String tag, final String name) {
        final int t = text.indexOf(tag);
        if (t < 0) {
            return null;
        }
        final int a = text.indexOf(name + "=\"", t);
        if (a < 0) {
            return null;
        }
        final int start = a + name.length() + 2;
        return text.substring(start, text.indexOf('"', start));
    }

    private static String read(final ZipFile zip, final String name) throws Exception {
        final ZipEntry entry = zip.getEntry(name);
        assertNotNull("the book has " + name, entry);
        return new String(bytes(zip.getInputStream(entry)), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(final InputStream in) throws Exception {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static void delete(final File file) {
        final File[] children = file.listFiles();
        for (int i = 0; children != null && i < children.length; i++) {
            delete(children[i]);
        }
        file.delete();
    }
}
