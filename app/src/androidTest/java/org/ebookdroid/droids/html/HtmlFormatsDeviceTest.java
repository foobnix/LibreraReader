package org.ebookdroid.droids.html;

import static org.junit.Assert.assertEquals;
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
import com.foobnix.ext.HtmlToEpub;
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
import org.ebookdroid.droids.DocContext;
import org.ebookdroid.droids.DocxContext;
import org.ebookdroid.droids.HtmlContext;
import org.ebookdroid.droids.MdContext;
import org.ebookdroid.droids.MhtContext;
import org.ebookdroid.droids.OdtContext;
import org.ebookdroid.droids.RtfContext;
import org.ebookdroid.droids.TxtContext;
import org.ebookdroid.droids.foliate.FoliateHostActivity;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The formats whose context prepares a loose {@code .html} rather than an {@code .epub}, on a
 * device: {@code .docx}, {@code .odt}, {@code .doc}, {@code .rtf}, {@code .mht}, {@code .txt} and
 * {@code .html} itself.
 *
 * MuPDF reads such a file directly, foliate-js does not, so in a build without MuPDF they are
 * wrapped in a one-chapter EPUB first - see {@link HtmlToEpub} and
 * {@link org.ebookdroid.droids.mupdf.codec.PdfContext#openReflowableHtml(String, String)}. What
 * has to hold either way is that the book opens, paginates and draws its text, and that a picture
 * the page names is still there once it has been through the wrapper.
 *
 * The three formats that cannot be written out as text - {@code .doc}, {@code .docx},
 * {@code .odt} - are read from {@code src/androidTest/assets/html-formats}, where they were put by
 * <code>textutil -convert doc|docx|odt -output sample.&lt;ext&gt; sample.txt</code> on a Mac, so
 * they are files a word processor really wrote rather than a hand-made approximation.
 */
@RunWith(AndroidJUnit4.class)
public class HtmlFormatsDeviceTest {

    /** A real screen's worth of page, as the foliate test uses; a small one is a layout question. */
    private static final int PAGE_WIDTH = 1080;

    private static final int PAGE_HEIGHT = 1800;

    private static final String ASSETS = "html-formats/";

    /** The sentence every one of these books is made of, so a page always has text on it. */
    private static final String SENTENCE = "The sample speaks of beginnings and of carefully"
            + " numbered pages, written out at length so that the engine has something to"
            + " paginate and something uncommonly long to measure.";

    /** The engine needs an activity on screen to attach its WebView to; see FoliateHostActivity. */
    private ActivityScenario<FoliateHostActivity> host;

    private File dir;

    private int engineBefore;

    private boolean preTextBefore;

    private boolean hypensBefore;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        host = ActivityScenario.launch(FoliateHostActivity.class);
        // The engine reads the activity from Application.registerActivityLifecycleCallbacks, so
        // the launch has to have been processed before a book is opened.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        engineBefore = AppState.get().epubEngine;
        preTextBefore = AppState.get().isPreText;
        hypensBefore = BookCSS.get().isAutoHypens;
        // Where MuPDF is in the build this says "use the other engine"; where it is not there is
        // only one and the setting is ignored (see Engines) - either way the book is foliate's
        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
        BookCSS.get().isAutoHypens = false;

        dir = new File(context.getCacheDir(), "html-formats-test");
        dir.mkdirs();
        delete(dir);
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        AppState.get().isPreText = preTextBefore;
        BookCSS.get().isAutoHypens = hypensBefore;
        if (dir != null) {
            delete(dir);
            dir.delete();
        }
        if (host != null) {
            host.close();
        }
    }

    // ---------------------------------------------------------------- the formats

    @Test
    public void htmlOpensAndRenders() throws Exception {
        final File file = new File(dir, "sample.html");
        write(file, "<html><head><title>Sample</title></head><body>" + paragraphs()
                + "</body></html>");
        opensAndRenders(file, HtmlContext.class);
    }

    @Test
    public void txtOpensAndRenders() throws Exception {
        // The reflowed .txt goes round by fb2 and comes out an .epub; it is the preformatted one
        // that is handed on as a page, so that is the path this format is checked on
        AppState.get().isPreText = true;
        final File file = new File(dir, "sample.txt");
        write(file, text());
        opensAndRenders(file, TxtContext.class);
    }

    @Test
    public void mhtOpensAndRenders() throws Exception {
        final File file = new File(dir, "sample.mht");
        write(file, "MIME-Version: 1.0\r\n"
                + "Content-Type: text/html; charset=\"utf-8\"\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + "<html><head><title>Sample</title></head><body>" + paragraphs()
                + "</body></html>\r\n");
        opensAndRenders(file, MhtContext.class);
    }

    @Test
    public void rtfOpensAndRenders() throws Exception {
        final StringBuilder rtf = new StringBuilder();
        rtf.append("{\\rtf1\\ansi\\ansicpg1252\\deff0{\\fonttbl{\\f0\\froman Times;}}");
        rtf.append("\\f0\\fs24 ");
        for (int i = 1; i <= 8; i++) {
            rtf.append("Paragraph ").append(i).append(" of the sample. ").append(SENTENCE);
            rtf.append("\\par\n");
        }
        rtf.append("}");
        final File file = new File(dir, "sample.rtf");
        write(file, rtf.toString());
        opensAndRenders(file, RtfContext.class);
    }

    @Test
    public void docxOpensAndRenders() throws Exception {
        opensAndRenders(asset("sample.docx"), DocxContext.class);
    }

    @Test
    public void odtOpensAndRenders() throws Exception {
        opensAndRenders(asset("sample.odt"), OdtContext.class);
    }

    @Test
    public void docOpensAndRenders() throws Exception {
        opensAndRenders(asset("sample.doc"), DocContext.class);
    }

    /**
     * A .md is in this set too.
     *
     * Only a {@code SUMMARY.md}, the table of contents of a GitBook, is turned into an .epub by
     * {@link MdContext}; a markdown file on its own becomes one page like the rest of these.
     */
    @Test
    public void mdOpensAndRenders() throws Exception {
        final StringBuilder md = new StringBuilder();
        md.append("# The sample\n\n");
        for (int i = 1; i <= 8; i++) {
            md.append("Paragraph ").append(i).append(" of the sample. ").append(SENTENCE);
            md.append("\n\n");
        }
        final File file = new File(dir, "sample.md");
        write(file, md.toString());
        opensAndRenders(file, MdContext.class);
    }

    /**
     * The archive is still there on the second open.
     *
     * A format that caches its page - .docx does - is reopened from the cache folder, and
     * {@link CacheZipUtils#removeFiles(File[], File)} empties that folder of everything whose name
     * does not start with the page's. The archive is named after the page for exactly this reason,
     * so this is where that would be noticed.
     */
    @Test
    public void aSecondOpenKeepsTheArchive() throws Exception {
        final File file = asset("sample.docx");
        CodecDocument document = open(file, DocxContext.class);
        try {
            assertTrue(document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp) >= 1);
        } finally {
            document.recycle();
        }

        document = open(file, DocxContext.class);
        try {
            assertTrue("it opened again from the cache",
                    document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp) >= 1);
        } finally {
            document.recycle();
        }

        if (!Engines.IS_MUPDF) {
            int archives = 0;
            final File[] cached = CacheZipUtils.CACHE_BOOK_DIR.listFiles();
            for (int i = 0; cached != null && i < cached.length; i++) {
                if (cached[i].getName().endsWith(HtmlToEpub.EXT)) {
                    archives++;
                }
            }
            assertEquals("the archive survived the cache prune", 1, archives);
        }
    }

    // ---------------------------------------------------------------- the pictures

    /**
     * A page's picture survives the wrapper.
     *
     * The generated HTML names its images by a path relative to the cache folder it was written
     * into, so they have to be carried into the archive for the page to still draw them. The book
     * here is one solid red square and no text, which is a thing a rendered page can be checked
     * for exactly.
     */
    @Test
    public void aPicturesNextToThePageIsDrawn() throws Exception {
        final File file = new File(dir, "picture.html");
        write(new File(dir, "red.png"), png());
        write(file, "<html><head><title>Picture</title></head><body>"
                + "<p><img src=\"red.png\" width=\"600\" height=\"600\"/></p></body></html>");

        final CodecDocument document = open(file, HtmlContext.class);
        try {
            document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            final Bitmap bitmap = render(document);
            int red = 0;
            for (int x = 0; x < bitmap.getWidth(); x += 4) {
                for (int y = 0; y < bitmap.getHeight(); y += 4) {
                    final int pixel = bitmap.getPixel(x, y);
                    if (Color.red(pixel) > 200 && Color.green(pixel) < 80 && Color.blue(pixel) < 80) {
                        red++;
                    }
                }
            }
            assertTrue("the picture was drawn, red pixels: " + red, red > 200);
        } finally {
            document.recycle();
        }
    }

    // ---------------------------------------------------------------- the wrapper itself

    @Test
    public void theWrapperHoldsThePageAndWhatItNames() throws Exception {
        final File html = new File(dir, "wrapped.html");
        write(new File(dir, "red.png"), png());
        write(new File(dir, "book.css"), "body{color:#000}");
        write(new File(dir, "unused.png"), png());
        write(html, "<html><head><title>Wrapped</title>"
                + "<link rel=\"stylesheet\" href=\"book.css\"/></head><body>"
                + "<p><img src=\"red.png\"/></p>"
                + "<p><img src=\"missing.png\"/></p>"
                + "<p><img src=\"https://librera.mobi/away.png\"/></p>"
                + "<p><a href=\"#top\">up</a></p>"
                + paragraphs() + "</body></html>");

        final String wrapped = HtmlToEpub.wrap(html.getPath());
        assertNotNull("the page was wrapped", wrapped);
        assertEquals(html.getPath() + HtmlToEpub.EXT, wrapped);

        final Set<String> names = entries(new File(wrapped));
        assertTrue("the mimetype is there: " + names, names.contains("mimetype"));
        assertTrue("and the container: " + names, names.contains("META-INF/container.xml"));
        assertTrue("and the package: " + names, names.contains("OEBPS/content.opf"));
        assertTrue("the page keeps its own name: " + names,
                names.contains("OEBPS/" + html.getName()));
        assertTrue("the picture came along: " + names, names.contains("OEBPS/red.png"));
        assertTrue("so did the stylesheet: " + names, names.contains("OEBPS/book.css"));
        // Only what the page names, and only what it names and has: the cache folder it was
        // written into holds every other book's leftovers too
        assertTrue("a file the page does not name stays out: " + names,
                !names.contains("OEBPS/unused.png"));
        assertTrue("and a reference with no file is skipped: " + names,
                !names.contains("OEBPS/missing.png"));
        assertEquals("nothing else was put in: " + names, 6, names.size());

        // The archive is cached beside the page rather than built again on the next open
        final long built = new File(wrapped).lastModified();
        Thread.sleep(1100);
        assertEquals(wrapped, HtmlToEpub.wrap(html.getPath()));
        assertEquals("the second wrap read the archive back", built,
                new File(wrapped).lastModified());

        // A page written again is wrapped again
        write(html, "<html><head><title>Wrapped</title></head><body>" + paragraphs()
                + "</body></html>");
        assertEquals(wrapped, HtmlToEpub.wrap(html.getPath()));
        assertTrue("a newer page is wrapped again",
                new File(wrapped).lastModified() > built);
        assertTrue("and the picture it no longer names is gone",
                !entries(new File(wrapped)).contains("OEBPS/red.png"));
    }

    /** Nothing may be pulled into the archive from outside the folder the page is in. */
    @Test
    public void theWrapperStaysInsideItsFolder() throws Exception {
        final File html = new File(dir, "escape.html");
        write(new File(dir, "red.png"), png());
        write(html, "<html><body><p><img src=\"../red.png\"/></p>"
                + "<p><img src=\"/red.png\"/></p>"
                + "<p><img src=\"file:///sdcard/red.png\"/></p>"
                + paragraphs() + "</body></html>");

        final String wrapped = HtmlToEpub.wrap(html.getPath());
        assertNotNull(wrapped);
        final Set<String> names = entries(new File(wrapped));
        assertEquals("only the page itself: " + names, 4, names.size());
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Opens the book through its own context and checks that it paginates and draws its text.
     *
     * @param expected the context the extension is supposed to be read by, so a format that
     *                 quietly stopped being offered fails here rather than passing on a different
     *                 engine's work
     */
    private void opensAndRenders(final File file, final Class<?> expected) {
        final CodecDocument document = open(file, expected);
        try {
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            assertTrue("it paginated, got " + pages, pages >= 1);
            assertTrue("and not into an absurd number, got " + pages, pages < 1000);

            final Bitmap bitmap = render(document);
            int dark = 0;
            int light = 0;
            for (int x = 0; x < bitmap.getWidth(); x += 2) {
                for (int y = 0; y < bitmap.getHeight(); y += 2) {
                    final int pixel = bitmap.getPixel(x, y);
                    assertEquals("the page is opaque", 255, Color.alpha(pixel));
                    if (Color.red(pixel) < 100) {
                        dark++;
                    } else if (Color.red(pixel) > 200) {
                        light++;
                    }
                }
            }
            assertTrue("the text was drawn, dark pixels: " + dark, dark > 20);
            assertTrue("the page is light, light pixels: " + light, light > dark);
        } finally {
            document.recycle();
        }
    }

    private CodecDocument open(final File file, final Class<?> expected) {
        final CodecContext context = BookType.getCodecContextByPath(file.getPath());
        assertNotNull("there is a context for " + file.getName(), context);
        assertEquals("the extension is read by its own context", expected, context.getClass());

        final CodecDocument document = context.openDocument(file.getPath(), "");
        assertNotNull("the book was opened: " + file.getName(), document);
        if (!Engines.IS_MUPDF) {
            // The only engine in this build, reached through the wrapper
            assertTrue("foliate laid it out: " + document, document instanceof FoliateDocument);
            final String path = ((FoliateDocument) document).getPath();
            assertTrue("and it was handed an archive: " + path,
                    path.toLowerCase(Locale.US).endsWith(".epub"));
        }
        return document;
    }

    private Bitmap render(final CodecDocument document) {
        final CodecPage page = document.getPage(0);
        assertNotNull(page);
        final BitmapRef ref = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
        assertNotNull("the page was rendered", ref);
        final Bitmap bitmap = ref.getBitmap();
        assertEquals(PAGE_WIDTH, bitmap.getWidth());
        assertEquals(PAGE_HEIGHT, bitmap.getHeight());
        return bitmap;
    }

    /** One of the word-processor files, copied out of the test's own assets. */
    private File asset(final String name) throws Exception {
        final File file = new File(dir, name);
        final InputStream in = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open(ASSETS + name);
        try {
            final OutputStream out = new FileOutputStream(file);
            try {
                final byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        return file;
    }

    private static String paragraphs() {
        final StringBuilder html = new StringBuilder();
        for (int i = 1; i <= 8; i++) {
            html.append("<p>Paragraph ").append(i).append(" of the sample. ").append(SENTENCE)
                    .append("</p>");
        }
        return html.toString();
    }

    private static String text() {
        final StringBuilder plain = new StringBuilder();
        for (int i = 1; i <= 8; i++) {
            plain.append("Paragraph ").append(i).append(" of the sample. ").append(SENTENCE)
                    .append("\n\n");
        }
        return plain.toString();
    }

    private static Set<String> entries(final File zip) throws Exception {
        final Set<String> names = new HashSet<String>();
        final ZipInputStream in = new ZipInputStream(new java.io.FileInputStream(zip));
        try {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        } finally {
            in.close();
        }
        return names;
    }

    /** A solid red square, written out as a PNG by hand so the test carries no binary of its own. */
    private static byte[] png() throws Exception {
        final int side = 60;
        final java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
        for (int y = 0; y < side; y++) {
            raw.write(0);
            for (int x = 0; x < side; x++) {
                raw.write(255);
                raw.write(0);
                raw.write(0);
            }
        }
        final java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        png.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        final java.io.ByteArrayOutputStream ihdr = new java.io.ByteArrayOutputStream();
        writeInt(ihdr, side);
        writeInt(ihdr, side);
        ihdr.write(8);  // 8 bits a channel
        ihdr.write(2);  // truecolour
        ihdr.write(0);
        ihdr.write(0);
        ihdr.write(0);
        chunk(png, "IHDR", ihdr.toByteArray());
        chunk(png, "IDAT", deflate(raw.toByteArray()));
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static byte[] deflate(final byte[] data) throws Exception {
        final java.util.zip.Deflater deflater = new java.util.zip.Deflater();
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final java.util.zip.DeflaterOutputStream zip =
                new java.util.zip.DeflaterOutputStream(out, deflater);
        zip.write(data);
        zip.finish();
        zip.close();
        return out.toByteArray();
    }

    private static void chunk(final java.io.ByteArrayOutputStream out, final String type,
                              final byte[] data) throws Exception {
        writeInt(out, data.length);
        final byte[] typed = type.getBytes(StandardCharsets.US_ASCII);
        out.write(typed);
        out.write(data);
        final java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(typed);
        crc.update(data);
        writeInt(out, (int) crc.getValue());
    }

    private static void writeInt(final java.io.ByteArrayOutputStream out, final int value) {
        out.write(value >>> 24);
        out.write(value >>> 16);
        out.write(value >>> 8);
        out.write(value);
    }

    private static void write(final File file, final String content) throws Exception {
        write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void write(final File file, final byte[] content) throws Exception {
        final OutputStream out = new FileOutputStream(file);
        try {
            out.write(content);
        } finally {
            out.close();
        }
    }

    private static void delete(final File dir) {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File file : files) {
            file.delete();
        }
    }
}
