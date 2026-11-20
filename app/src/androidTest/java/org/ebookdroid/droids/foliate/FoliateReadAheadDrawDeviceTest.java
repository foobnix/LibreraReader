package org.ebookdroid.droids.foliate;

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

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.foliate.codec.FoliateDocument;
import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Every word of a page is in the page drawn while TTS reads ahead.
 * <p>
 * TTS reads the text of the pages ahead through the one WebView the pages are drawn from, so after
 * it the book is somewhere else and the next page drawn loads its section again - a new document,
 * whose fonts load again. Headings and text are in two font files (Literata Bold and Regular, as
 * a user's settings have them), so a page captured while one of them was still loading came out
 * with the heading and none of the text, and the reader kept that bitmap.
 */
@RunWith(AndroidJUnit4.class)
public class FoliateReadAheadDrawDeviceTest {

    private static final int PAGE_WIDTH = 1080;

    private static final int PAGE_HEIGHT = 1800;

    private static final String[] STYLES = {"Regular", "Bold", "Italic", "BoldItalic"};

    private ActivityScenario<FoliateHostActivity> host;

    private File dir;

    private int engineBefore;

    private String[] fontsBefore;

    @Before
    public void setUp() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        host = ActivityScenario.launch(FoliateHostActivity.class);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        dir = new File(context.getCacheDir(), "read-ahead-draw-test");
        dir.mkdirs();

        // The bundled Literata, wherever this profile would have unpacked it
        final File[] fonts = new File[STYLES.length];
        for (int i = 0; i < STYLES.length; i++) {
            fonts[i] = new File(dir, "Literata-" + STYLES[i] + ".ttf");
            final InputStream in = context.getAssets().open("fonts/Literata-" + STYLES[i] + ".ttf");
            final OutputStream out = new FileOutputStream(fonts[i]);
            try {
                final byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        }
        final BookCSS css = BookCSS.get();
        fontsBefore = new String[]{css.normalFont, css.boldFont, css.italicFont, css.boldItalicFont,
                css.headersFont, css.capitalFont};
        css.normalFont = fonts[0].getPath();
        css.boldFont = fonts[1].getPath();
        css.italicFont = fonts[2].getPath();
        css.boldItalicFont = fonts[3].getPath();
        css.headersFont = fonts[1].getPath();
        css.capitalFont = fonts[0].getPath();

        engineBefore = AppState.get().epubEngine;
        AppState.get().epubEngine = AppState.EPUB_ENGINE_FOLIATE;
    }

    @After
    public void tearDown() {
        AppState.get().epubEngine = engineBefore;
        if (fontsBefore != null) {
            final BookCSS css = BookCSS.get();
            css.normalFont = fontsBefore[0];
            css.boldFont = fontsBefore[1];
            css.italicFont = fontsBefore[2];
            css.boldItalicFont = fontsBefore[3];
            css.headersFont = fontsBefore[4];
            css.capitalFont = fontsBefore[5];
        }
        final File[] files = dir == null ? null : dir.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        if (dir != null) {
            dir.delete();
        }
        if (host != null) {
            host.close();
        }
    }

    @Test
    public void everyWordIsDrawnAfterTtsReadsAhead() throws Exception {
        final File book = new File(dir, "read-ahead.epub");
        final OutputStream out = new FileOutputStream(book);
        try {
            out.write(epub());
        } finally {
            out.close();
        }
        final CodecContext context = BookType.getCodecContextByPath(book.getPath());
        assertNotNull(context);
        final CodecDocument document = context.openDocument(book.getPath(), "");
        assertNotNull("the book was opened", document);
        try {
            assertTrue("foliate laid it out: " + document, document instanceof FoliateDocument);
            final FoliateDocument foliate = (FoliateDocument) document;
            final int pages = document.getPageCount(PAGE_WIDTH, PAGE_HEIGHT, BookCSS.get().fontSizeSp);
            assertTrue("pages: " + pages, pages >= 6);

            int drawn = 0;
            final StringBuilder failures = new StringBuilder();
            for (int round = 0; round < 3; round++) {
                for (int page = 0; page < pages; page++) {
                    // What TTS does on every page: the pages ahead are read, which moves the book
                    readAheadAndWait(foliate, page, pages);
                    final String missing = missingWords(document, page);
                    drawn++;
                    if (missing != null) {
                        failures.append("\nround ").append(round).append(", ").append(missing);
                    }
                }
            }
            LOG.d("FOLIATE-TEST read-ahead draws", drawn, "failures", failures);
            assertEquals("pages drawn without some of their words, of " + drawn + ":" + failures,
                    "", failures.toString());
        } finally {
            document.recycle();
        }
    }

    /** Reads ahead from the page as TTS does, and waits until that has moved the book on. */
    private static void readAheadAndWait(final FoliateDocument document, final int page, final int pages)
            throws InterruptedException {
        final int last = Math.min(pages - 1, page + 3);
        document.readAhead(page + 1);
        final long until = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < until && page + 1 < pages && !readAheadHas(document, last)) {
            Thread.sleep(20);
        }
    }

    private static boolean readAheadHas(final FoliateDocument document, final int page) {
        try {
            final java.lang.reflect.Method method =
                    FoliateDocument.class.getDeclaredMethod("readAheadText", int.class);
            method.setAccessible(true);
            return method.invoke(document, page) != null;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Draws a page and says which of its words have no ink where they are; null when all of them
     * have some.
     */
    private static String missingWords(final CodecDocument document, final int page) {
        final CodecPage codecPage = document.getPageInner(page);
        assertNotNull(codecPage);
        try {
            final BitmapRef ref = codecPage.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
            assertNotNull("page " + page + " was drawn", ref);
            final Bitmap bitmap = ref.getBitmap();
            final TextWord[][] lines = codecPage.getText();
            int words = 0;
            int missing = 0;
            String first = null;
            for (int i = 0; lines != null && i < lines.length; i++) {
                for (TextWord word : lines[i]) {
                    if (word == null || word.w == null || word.w.trim().length() < 2) {
                        continue;
                    }
                    words++;
                    if (!hasInk(bitmap, word)) {
                        missing++;
                        if (first == null) {
                            first = word.w;
                        }
                    }
                }
            }
            return missing == 0 ? null
                    : "page " + page + ": " + missing + " of " + words + " words not drawn, first '" + first + "'";
        } finally {
            codecPage.recycle();
        }
    }

    private static boolean hasInk(final Bitmap bitmap, final RectF word) {
        final int left = Math.max(0, (int) (word.left * bitmap.getWidth()));
        final int right = Math.min(bitmap.getWidth(), (int) Math.ceil(word.right * bitmap.getWidth()));
        final int top = Math.max(0, (int) (word.top * bitmap.getHeight()));
        final int bottom = Math.min(bitmap.getHeight(), (int) Math.ceil(word.bottom * bitmap.getHeight()));
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right; x++) {
                if (Color.red(bitmap.getPixel(x, y)) < 110) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Short chapters, each a heading and some text: most pages are a section's first or last. */
    private static byte[] epub() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final ZipOutputStream zip = new ZipOutputStream(bytes);
        final byte[] mimetype = "application/epub+zip".getBytes(StandardCharsets.US_ASCII);
        final ZipEntry entry = new ZipEntry("mimetype");
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(mimetype.length);
        entry.setCompressedSize(mimetype.length);
        final CRC32 crc = new CRC32();
        crc.update(mimetype);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(mimetype);
        zip.closeEntry();

        write(zip, "META-INF/container.xml",
                "<?xml version=\"1.0\"?>"
                        + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                        + "<rootfiles><rootfile full-path=\"OEBPS/content.opf\""
                        + " media-type=\"application/oebps-package+xml\"/></rootfiles></container>");
        final StringBuilder manifest = new StringBuilder();
        final StringBuilder spine = new StringBuilder();
        final int chapters = 8;
        for (int i = 0; i < chapters; i++) {
            final String name = "chapter" + (i + 1) + ".xhtml";
            manifest.append("<item id=\"c").append(i + 1).append("\" href=\"").append(name)
                    .append("\" media-type=\"application/xhtml+xml\"/>");
            spine.append("<itemref idref=\"c").append(i + 1).append("\"/>");
            final StringBuilder body = new StringBuilder("<h1>Глава ").append(i + 1).append("</h1>");
            for (int p = 0; p < 6; p++) {
                body.append("<p>Шесть раз они ужинали на открытом воздухе, болтая о всякой всячине ")
                        .append("у небольшого костерка, абзац ").append(p + 1).append(" главы ")
                        .append(i + 1).append(". Отсветы играли высоко на боку серебристой ракеты, ")
                        .append("доставившей их сюда через космос.</p>");
            }
            write(zip, "OEBPS/" + name,
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                            + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"ru\"><head><title>Глава "
                            + (i + 1) + "</title></head><body>" + body + "</body></html>");
        }
        write(zip, "OEBPS/content.opf",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"id\">"
                        + "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
                        + "<dc:title>Read Ahead</dc:title><dc:language>ru</dc:language>"
                        + "<dc:identifier id=\"id\">librera-read-ahead-test</dc:identifier></metadata>"
                        + "<manifest>" + manifest + "</manifest><spine>" + spine + "</spine></package>");
        zip.close();
        return bytes.toByteArray();
    }

    private static void write(final ZipOutputStream zip, final String name, final String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
