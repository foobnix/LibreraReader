package org.ebookdroid.droids.comic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.droids.comic.codec.ComicContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The comic engine on a device, against a .cbz the test writes itself.
 *
 * It is the engine the flavours built without MuPDF read .cbz and .cbr with: the archive's images
 * are decoded straight to bitmaps instead of being opened as a MuPDF document. What has to hold is
 * what the rest of Librera relies on - the number of pages, their order, their sizes, and pixels
 * in the right places when a page is drawn whole and when it is drawn one tile at a time.
 *
 * The archive holds two solid-colour pages, named so that a plain string sort would get their
 * order wrong, plus the junk a Mac and a comic tool leave behind.
 */
@RunWith(AndroidJUnit4.class)
public class ComicEngineDeviceTest {

    /** Page one: named "page9", so it must sort before "page10". */
    private static final int P1_WIDTH = 60;
    private static final int P1_HEIGHT = 90;
    private static final int P1_COLOR = Color.rgb(0, 0, 255);

    private static final int P2_WIDTH = 120;
    private static final int P2_HEIGHT = 80;
    private static final int P2_COLOR = Color.rgb(255, 0, 0);

    /** The colour drawn into the bottom right quarter of page two, for the tiling check. */
    private static final int P2_CORNER = Color.rgb(0, 255, 0);

    private File file;
    private ComicContext context;
    private CodecDocument document;

    @Before
    public void setUp() throws Exception {
        final Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(ctx);
        CacheZipUtils.createAllCacheDirs();
        // Without the profile, openDocument swallows a NullPointerException and answers null
        AppProfile.init(ctx);
        TempHolder.get().loadingCancelled.set(false);

        file = new File(ctx.getCacheDir(), "comic-engine-test.cbz");
        writeArchive(file);

        context = new ComicContext();
        document = context.openDocument(file.getPath(), "");
        assertNotNull("the engine could not open the archive", document);
    }

    @After
    public void tearDown() {
        if (document != null && !document.isRecycled()) {
            document.recycle();
        }
        if (file != null) {
            file.delete();
        }
    }

    /**
     * Two solid pages, and three entries that are not pages: the metadata file a comic tool
     * writes, the folder a Mac adds and the resource fork it puts beside every image - that last
     * one is a copy of the image under the same name, so counting it would double the book.
     */
    private static void writeArchive(final File target) throws Exception {
        final OutputStream out = new FileOutputStream(target);
        final ZipOutputStream zip = new ZipOutputStream(out);
        try {
            // Deliberately added in the wrong order, so the engine has to sort them
            writeEntry(zip, "pages/page10.png", page2());
            writeEntry(zip, "pages/page9.png", solid(P1_WIDTH, P1_HEIGHT, P1_COLOR));

            writeEntry(zip, "ComicInfo.xml", null);
            writeEntry(zip, "__MACOSX/pages/._page9.png", solid(8, 8, Color.BLACK));
            writeEntry(zip, "pages/._page10.png", solid(8, 8, Color.BLACK));
        } finally {
            zip.close();
        }
    }

    /** Page two: red, with its bottom right quarter green. */
    private static Bitmap page2() {
        final Bitmap bitmap = solid(P2_WIDTH, P2_HEIGHT, P2_COLOR);
        final Canvas canvas = new Canvas(bitmap);
        final android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(P2_CORNER);
        canvas.drawRect(P2_WIDTH / 2f, P2_HEIGHT / 2f, P2_WIDTH, P2_HEIGHT, paint);
        return bitmap;
    }

    private static Bitmap solid(final int width, final int height, final int color) {
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap;
    }

    private static void writeEntry(final ZipOutputStream zip, final String name, final Bitmap image)
            throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        if (image == null) {
            zip.write("<ComicInfo/>".getBytes("UTF-8"));
        } else {
            image.compress(Bitmap.CompressFormat.PNG, 100, zip);
            image.recycle();
        }
        zip.closeEntry();
    }

    // ---------------------------------------------------------------- the book

    @Test
    public void readsOnlyTheImages() {
        // Five entries, two of them pages: the metadata and the two resource forks are not
        assertEquals(2, document.getPageCount());
    }

    @Test
    public void pageCountDoesNotReflow() {
        // A comic is a fixed number of images whatever the screen and the font size are
        assertEquals(document.getPageCount(), document.getPageCount(480, 800, 14));
    }

    @Test
    public void sortsPagesAsAReaderExpects() {
        // page9 before page10, which a plain string sort puts the other way round
        final CodecPageInfo first = document.getPageInfo(0);
        final CodecPageInfo second = document.getPageInfo(1);
        assertEquals(P1_WIDTH, first.width);
        assertEquals(P1_HEIGHT, first.height);
        assertEquals(P2_WIDTH, second.width);
        assertEquals(P2_HEIGHT, second.height);
    }

    @Test
    public void pagesHaveNoOneSize() {
        // The pages of a comic are each whatever size their image is
        assertNull(document.getUnifiedPageInfo());
    }

    @Test
    public void readsTheImageSizeAsThePageSize() {
        final CodecPage page = document.getPage(0);
        assertEquals(P1_WIDTH, page.getWidth());
        assertEquals(P1_HEIGHT, page.getHeight());
    }

    @Test
    public void hasNoTextOrAnnotationLayer() {
        final CodecPage page = document.getPage(0);
        assertEquals(0, page.getCharCount());
        assertEquals(0, page.getText().length);
        assertTrue(page.getAnnotations().isEmpty());
        assertTrue(page.getPageLinks().isEmpty());
    }

    // ---------------------------------------------------------------- the pixels

    @Test
    public void drawsTheWholePage() {
        final BitmapRef ref =
                document.getPage(0).renderBitmapSimple(P1_WIDTH, P1_HEIGHT, new RectF(0, 0, 1, 1));
        assertNotNull(ref);
        final Bitmap bitmap = ref.getBitmap();
        assertEquals(P1_WIDTH, bitmap.getWidth());
        assertEquals(P1_HEIGHT, bitmap.getHeight());
        assertColor(P1_COLOR, bitmap.getPixel(P1_WIDTH / 2, P1_HEIGHT / 2));
        assertColor(P1_COLOR, bitmap.getPixel(1, 1));
    }

    /** The reader draws a zoomed page in tiles, so a slice has to hold that part of the image. */
    @Test
    public void drawsOneTileOfAPage() {
        final CodecPage page = document.getPage(1);

        // The top left quarter is red, the bottom right quarter green
        final BitmapRef topLeft = page.renderBitmapSimple(80, 80, new RectF(0, 0, 0.5f, 0.5f));
        assertNotNull(topLeft);
        assertColor(P2_COLOR, topLeft.getBitmap().getPixel(40, 40));

        final BitmapRef bottomRight = page.renderBitmapSimple(80, 80, new RectF(0.5f, 0.5f, 1f, 1f));
        assertNotNull(bottomRight);
        assertColor(P2_CORNER, bottomRight.getBitmap().getPixel(40, 40));
    }

    /** Zoomed in: the engine decodes the image large enough to fill the tile it was asked for. */
    @Test
    public void drawsATileLargerThanTheImage() {
        final BitmapRef ref =
                document.getPage(1).renderBitmapSimple(200, 200, new RectF(0.5f, 0.5f, 1f, 1f));
        assertNotNull(ref);
        assertEquals(200, ref.getBitmap().getWidth());
        assertColor(P2_CORNER, ref.getBitmap().getPixel(100, 100));
    }

    @Test
    public void drawsAThumbnail() {
        final Bitmap thumb = document.getPage(0).renderThumbnail(30);
        assertNotNull(thumb);
        assertEquals(30, thumb.getWidth());
        // The thumbnail keeps the page's shape
        assertEquals(30 * P1_HEIGHT / P1_WIDTH, thumb.getHeight());
        assertColor(P1_COLOR, thumb.getPixel(15, thumb.getHeight() / 2));
    }

    @Test
    public void readsAPageAfterAnother() {
        // The archive hands out one entry stream at a time; reading pages in turn has to work
        for (int i = 0; i < document.getPageCount(); i++) {
            final BitmapRef ref =
                    document.getPage(i).renderBitmapSimple(40, 40, new RectF(0, 0, 1, 1));
            assertNotNull("page " + i + " did not draw", ref);
        }
    }

    @Test
    public void searchIsNotSupported() {
        try {
            document.searchText(0, "anything");
            org.junit.Assert.fail("a comic has no text to search");
        } catch (CodecDocument.DocSearchNotSupported expected) {
            // what the reader catches to leave the search out for this book
        }
    }

    /** A page drawn at its own size has no resampling in it, so the colours have to be exact. */
    private static void assertColor(final int expected, final int actual) {
        assertEquals("red", Color.red(expected), Color.red(actual));
        assertEquals("green", Color.green(expected), Color.green(actual));
        assertEquals("blue", Color.blue(expected), Color.blue(actual));
    }
}
