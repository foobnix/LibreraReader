package com.foobnix.ext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.BaseExtractor;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.PageUrl;
import com.foobnix.pdf.info.R;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.droids.comic.codec.ComicContext;
import org.ebookdroid.droids.tiff.codec.TiffContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * libtiff.so on a device, against the TIFF files in assets/tiff: each was written by libtiff's
 * tiffcp in one of the layouts and compressions scans come in, beside a PNG of what libtiff - read
 * through Pillow on the computer that made them - draws of it (make-fixtures.py). What has to hold
 * is that the image comes out the same here, whole and shrunk, and that a comic archive of TIFF
 * pages opens in the comic engine and gives its cover - which Android's own decoder cannot read -
 * and a .tif book opens in the TIFF engine with every image a page.
 */
@RunWith(AndroidJUnit4.class)
public class NativeTiffDeviceTest {

    private static final String[] LOSSLESS = {
            "bw-g3-1d", "bw-g3-2d-fill", "bw-g4", "bw-g4-lsb2msb", "bw-g4-miniswhite", "cmyk",
            "gray16-lzw-predictor", "palette", "rgb-bigtiff", "rgb-lzw-predictor", "rgb-planar-lzw",
            "rgb-tiled-packbits", "rgb-zip-predictor"};

    /** The image the JPEG files were made from, as it was before JPEG lost anything of it. */
    private static final String JPEG_SOURCE = "rgb-lzw-predictor";
    private static final String[] JPEG = {"rgb-jpeg", "rgb-tiled-jpeg"};

    @Test
    public void everyLosslessFileComesOutAsLibtiffDrawsIt() throws Exception {
        for (String name : LOSSLESS) {
            final Bitmap actual = decode(name, 1);
            final Bitmap expected = expected(name);
            assertEquals(name + " width", expected.getWidth(), actual.getWidth());
            assertEquals(name + " height", expected.getHeight(), actual.getHeight());
            assertEquals(name + " pixels that differ", 0, differing(expected, actual, 0));
        }
    }

    /**
     * JPEG decoders do not agree to the level - libjpeg here and libjpeg-turbo on the computer
     * that drew the PNGs scale the colour up differently - and the image's blue, a sawtooth, is
     * as hard on JPEG as it gets. So what is checked is how near each comes to the image the file
     * was made from: as near as libtiff on the computer did, and no colour off one way.
     */
    @Test
    public void jpegFilesComeOutAsNearTheOriginalAsLibtiffDrawsThem() throws Exception {
        final Bitmap source = expected(JPEG_SOURCE);
        for (String name : JPEG) {
            final Bitmap actual = decode(name, 1);
            final Bitmap reference = expected(name);
            assertEquals(name, source.getWidth(), actual.getWidth());
            assertEquals(name, source.getHeight(), actual.getHeight());
            final double[] ours = error(source, actual);
            final double[] theirs = error(source, reference);
            for (int c = 0; c < 3; c++) {
                assertTrue(name + " channel " + c + ": " + ours[c] + " off, libtiff " + theirs[c],
                        ours[c] <= theirs[c] * 1.15 + 1);
                assertEquals(name + " channel " + c + " leans one way", 0, ours[c + 3], 2);
            }
        }
    }

    @Test
    public void unassociatedAlphaComesOutAsItIs() throws Exception {
        final Bitmap actual = decode("rgba-lzw", 1);
        final Bitmap expected = expected("rgba-lzw");
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                final int e = expected.getPixel(x, y);
                final int a = actual.getPixel(x, y);
                assertEquals("alpha at " + x + "," + y, Color.alpha(e), Color.alpha(a), 1);
                // The bitmap holds its colours premultiplied: the fainter, the coarser they come back
                if (Color.alpha(e) >= 128) {
                    assertEquals("red at " + x + "," + y, Color.red(e), Color.red(a), 3);
                    assertEquals("green at " + x + "," + y, Color.green(e), Color.green(a), 3);
                    assertEquals("blue at " + x + "," + y, Color.blue(e), Color.blue(a), 3);
                }
            }
        }
    }

    /**
     * Shrunk, every pixel is the average of the square it stands for - the last ones of a row
     * and a column of what is left over - so a fax page's lines grey out rather than vanish.
     */
    @Test
    public void aShrunkImageIsTheAverageOfTheWholeOne() throws Exception {
        for (String name : new String[]{"bw-g4", "rgb-lzw-predictor", "rgb-tiled-packbits", "rgb-planar-lzw"}) {
            final Bitmap whole = expected(name);
            for (int s : new int[]{2, 3, 7}) {
                final Bitmap shrunk = decode(name, s);
                assertEquals(name + "/" + s, (whole.getWidth() + s - 1) / s, shrunk.getWidth());
                assertEquals(name + "/" + s, (whole.getHeight() + s - 1) / s, shrunk.getHeight());
                for (int oy = 0; oy < shrunk.getHeight(); oy++) {
                    for (int ox = 0; ox < shrunk.getWidth(); ox++) {
                        final int e = average(whole, ox * s, oy * s, s);
                        final int a = shrunk.getPixel(ox, oy);
                        assertEquals(name + "/" + s + " red at " + ox + "," + oy, Color.red(e), Color.red(a), 1);
                        assertEquals(name + "/" + s + " green at " + ox + "," + oy, Color.green(e), Color.green(a), 1);
                        assertEquals(name + "/" + s + " blue at " + ox + "," + oy, Color.blue(e), Color.blue(a), 1);
                    }
                }
            }
        }
    }

    @Test
    public void whatIsNotATiffIsNotRead() throws Exception {
        final byte[] png = asset("rgb-lzw-predictor.png");
        assertFalse(NativeTiff.isTiff(png));
        assertNull(NativeTiff.size(png));
        assertNull(NativeTiff.decode(png, 1));

        // A TIFF cut short: its header says where its directory is, and the directory is gone
        final byte[] tiff = asset("rgb-lzw-predictor.tif");
        final byte[] cut = new byte[64];
        System.arraycopy(tiff, 0, cut, 0, cut.length);
        assertTrue(NativeTiff.isTiff(cut));
        assertNull(NativeTiff.decode(cut, 1));
    }

    /**
     * A .cbz of TIFF pages, with a PNG among them, opens in the comic engine with every page - a
     * TIFF page was not even counted as one before - and its cover, the first page, is drawn.
     */
    @Test
    public void aComicOfTiffPagesOpens() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Without the profile, openDocument swallows a NullPointerException and answers null
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        final File file = new File(context.getCacheDir(), "native-tiff-test.cbz");
        final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file));
        try {
            put(zip, "page1.tif", asset("bw-g4.tif"));
            put(zip, "page2.png", asset("rgb-lzw-predictor.png"));
            put(zip, "page3.tiff", asset("rgb-jpeg.tif"));
        } finally {
            zip.close();
        }

        final CodecDocument document = new ComicContext().openDocument(file.getPath(), "");
        try {
            assertNotNull("the comic opens", document);
            assertEquals(3, document.getPageCount());

            final CodecPageInfo fax = document.getPageInfo(0);
            assertEquals(2700, fax.width);
            assertEquals(24, fax.height);
            final CodecPageInfo jpeg = document.getPageInfo(2);
            assertEquals(97, jpeg.width);
            assertEquals(61, jpeg.height);

            for (int page = 0; page < 3; page++) {
                final BitmapRef ref = document.getPage(page).renderBitmapSimple(97, 61, new RectF(0, 0, 1, 1));
                assertNotNull("page " + page + " is drawn", ref);
                assertTrue("page " + page + " is not blank", colours(ref.getBitmap()) > 2);
            }
        } finally {
            if (document != null) {
                document.recycle();
            }
            file.delete();
        }

        final Bitmap cover = BaseExtractor.arrayToBitmap(asset("rgb-jpeg.tif"), 40);
        assertNotNull("a TIFF cover is drawn", cover);
        assertTrue("shrunk to about the width asked for: " + cover.getWidth(), cover.getWidth() <= 97 / 2 + 1);
    }

    /**
     * A .tif book of three pages - a fax page, an LZW one and a JPEG one, with a thumbnail of
     * the first stored between them - opens in the TIFF engine with the three pages and not the
     * thumbnail, each its own size and drawn as libtiff draws it. Every build reads .tif books
     * this way, the ones with MuPDF too.
     */
    @Test
    public void aTiffBookOpensEveryImageButItsThumbnailsAsAPage() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        final CodecContext chosen = BookType.getCodecContextByPath("/sdcard/Books/scan.tif");
        assertTrue("the engine for a .tif: " + chosen, chosen instanceof TiffContext);
        assertTrue(BookType.isSupportedExtByPath("/sdcard/Books/scan.TIFF"));

        final File file = new File(context.getCacheDir(), "native-tiff-test-book.tif");
        final FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(asset("book.tif"));
        } finally {
            out.close();
        }

        final CodecDocument document = new TiffContext().openDocument(file.getPath(), "");
        try {
            assertNotNull("the book opens", document);
            assertEquals("pages, the thumbnail not among them", 3, document.getPageCount());
            assertEquals(2700, document.getPageInfo(0).width);
            assertEquals(24, document.getPageInfo(0).height);
            assertEquals(97, document.getPageInfo(1).width);
            assertEquals(61, document.getPageInfo(2).height);

            // Drawn at its own size, a lossless page is what libtiff draws of it
            final Bitmap lzw = document.getPage(1).renderBitmapSimple(97, 61, new RectF(0, 0, 1, 1)).getBitmap();
            assertEquals(0, differing(expected("rgb-lzw-predictor"), lzw, 1));
            for (int page : new int[]{0, 2}) {
                final BitmapRef ref = document.getPage(page).renderBitmapSimple(97, 61, new RectF(0, 0, 1, 1));
                assertNotNull("page " + page + " is drawn", ref);
                assertTrue("page " + page + " is not blank", colours(ref.getBitmap()) > 2);
            }
            // Asked out of order, the pages still come out right
            assertEquals(2700, document.getPage(0).getWidth());
        } finally {
            if (document != null) {
                document.recycle();
            }
            file.delete();
        }

        assertNull("what is not a TIFF does not open", new TiffContext().openDocument(
                new File(context.getCacheDir(), "no-such-book.tif").getPath(), ""));
    }

    /**
     * A TIFF libtiff opens but does not draw - 12 bits a sample, which its RGBA interface does not
     * take - has a cover that says it is not supported, under its file name, and not #error.
     */
    @Test
    public void aTiffThatCannotBeDrawnHasANotSupportedCoverWithItsName() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        final File file = new File(context.getCacheDir(), "scan-12bit.tif");
        final FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(asset("gray12.tif"));
        } finally {
            out.close();
        }
        try {
            final NativeTiff.Book book = NativeTiff.Book.open(file.getPath());
            assertNotNull("libtiff reads the file", book);
            try {
                assertNull("but does not draw it", book.decode(0, 1));
            } finally {
                book.close();
            }

            final Bitmap cover = ImageExtractor.getInstance(context).proccessCoverPage(
                    new PageUrl(file.getPath(), ImageExtractor.COVER_PAGE, 200, 0, false, false, 0, false));
            final Bitmap expected = BaseExtractor.getBookCoverWithTitle(context.getString(R.string.not_supported),
                    file.getName(), true);
            assertNotNull(cover);
            assertTrue("the cover says it is not supported, with the name", cover.sameAs(expected));
        } finally {
            file.delete();
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Bitmap decode(final String name, final int sample) throws Exception {
        final byte[] data = asset(name + ".tif");
        assertTrue(name + " is a TIFF", NativeTiff.isTiff(data));
        final Bitmap bitmap = NativeTiff.decode(data, sample);
        assertNotNull(name + " is decoded", bitmap);
        return bitmap;
    }

    private static Bitmap expected(final String name) throws Exception {
        final byte[] png = asset(name + ".png");
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPremultiplied = false;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(png, 0, png.length, options);
        assertNotNull(name + ".png", bitmap);
        return bitmap;
    }

    private static int differing(final Bitmap expected, final Bitmap actual, final int tolerance) {
        int count = 0;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                final int e = expected.getPixel(x, y);
                final int a = actual.getPixel(x, y);
                if (Math.abs(Color.red(e) - Color.red(a)) > tolerance
                        || Math.abs(Color.green(e) - Color.green(a)) > tolerance
                        || Math.abs(Color.blue(e) - Color.blue(a)) > tolerance
                        || Color.alpha(e) != Color.alpha(a)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The mean distance of each of red, green and blue from the source, then the mean difference. */
    private static double[] error(final Bitmap source, final Bitmap image) {
        final double[] out = new double[6];
        final int n = source.getWidth() * source.getHeight();
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                final int e = source.getPixel(x, y);
                final int a = image.getPixel(x, y);
                final int[] d = {Color.red(a) - Color.red(e), Color.green(a) - Color.green(e),
                        Color.blue(a) - Color.blue(e)};
                for (int c = 0; c < 3; c++) {
                    out[c] += Math.abs(d[c]) / (double) n;
                    out[c + 3] += d[c] / (double) n;
                }
            }
        }
        return out;
    }

    private static int average(final Bitmap b, final int x0, final int y0, final int s) {
        int r = 0;
        int g = 0;
        int bl = 0;
        int n = 0;
        for (int y = y0; y < Math.min(b.getHeight(), y0 + s); y++) {
            for (int x = x0; x < Math.min(b.getWidth(), x0 + s); x++) {
                final int p = b.getPixel(x, y);
                r += Color.red(p);
                g += Color.green(p);
                bl += Color.blue(p);
                n++;
            }
        }
        return Color.rgb(r / n, g / n, bl / n);
    }

    private static int colours(final Bitmap b) {
        final java.util.Set<Integer> set = new java.util.HashSet<Integer>();
        for (int y = 0; y < b.getHeight(); y += 3) {
            for (int x = 0; x < b.getWidth(); x += 3) {
                set.add(b.getPixel(x, y));
            }
        }
        return set.size();
    }

    private static void put(final ZipOutputStream zip, final String name, final byte[] data) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static byte[] asset(final String name) throws Exception {
        final InputStream in = InstrumentationRegistry.getInstrumentation().getContext().getAssets()
                                                      .open("tiff/" + name);
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[16 * 1024];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
