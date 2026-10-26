package com.foobnix.pdf.info;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.LibreraBuildConfig;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.libmobi.LibMobi;
import com.foobnix.model.AppProfile;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.RawBitmap;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Each part of the native code that is not MuPDF is a library of its own, the same in the flavours
 * built with MuPDF and the JS ones without it ({@link Engines#NATIVE_LIBRARIES},
 * Builder/jni/Librera-libs.mk). This runs the same way in both, against each library in turn, and
 * checks that MuPDF is there in the first and nowhere in the second.
 *
 * The books are not in the repository: they are pushed beside the app, into
 * {@code Android/data/<application id>/files/native-test/} - sample.djvu, sample.mobi,
 * sample.azw3, sample.doc. A test whose book is missing is skipped. Timings are logged under
 * {@value #TAG}.
 */
@RunWith(AndroidJUnit4.class)
public class NativeLibrariesDeviceTest {

    private static final String TAG = "NativeLibrariesTest";
    private static final int PAGE_WIDTH = 1080;
    private static final int PAGE_HEIGHT = 1800;

    private File dir;
    private File out;

    @Before
    public void setUp() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppsConfig.IS_LOG = true;
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        dir = new File(context.getExternalFilesDir(null), "native-test");
        out = new File(context.getCacheDir(), "native-test-out");
        out.mkdirs();
    }

    @Test
    public void muPdfIsThereOnlyWhereTheBuildSays() {
        // AppsConfig loaded the libraries; this is what it found
        Log.i(TAG, "flavour " + LibreraBuildConfig.FLAVOR + " IS_MUPDF " + Engines.IS_MUPDF
                + " MuPDF " + AppsConfig.MUPDF_FZ_VERSION);
        if (Engines.IS_MUPDF) {
            assertFalse("MuPDF answers", AppsConfig.MUPDF_FZ_VERSION.isEmpty());
        } else {
            assertTrue(AppsConfig.MUPDF_FZ_VERSION.isEmpty());
            boolean loaded;
            try {
                System.loadLibrary(Engines.MUPDF_LIBRARY);
                loaded = true;
            } catch (UnsatisfiedLinkError e) {
                loaded = false;
            }
            assertFalse("libMuPDF.so is not in this build", loaded);
        }
    }

    @Test
    public void djvuOpensAndRendersWithLibDjvu() {
        final File book = book("sample.djvu");
        final CodecContext context = BookType.getCodecContextByPath(book.getPath());
        assertTrue("DjVu is read by DjvuContext: " + context, context instanceof DjvuContext);

        long t = System.nanoTime();
        final CodecDocument document = context.openDocument(book.getPath(), "");
        final long openMs = ms(t);
        assertNotNull(document);
        try {
            final int pages = document.getPageCount();
            assertTrue(pages > 0);
            t = System.nanoTime();
            final CodecPage page = document.getPage(0);
            final BitmapRef ref = page.renderBitmap(PAGE_WIDTH, PAGE_HEIGHT, new RectF(0, 0, 1, 1), false);
            assertNotNull(ref);
            assertTrue("the page shows something", hasContent(ref.getBitmap()));
            page.recycle();
            Log.i(TAG, String.format(Locale.US, "djvu %d pages, open %d ms, page 1 %d ms", pages, openMs, ms(t)));
        } finally {
            document.recycle();
        }
    }

    @Test
    public void mobiConvertsWithLibMobi() throws Exception {
        for (final String name : new String[]{"sample.mobi", "sample.azw3"}) {
            final File book = book(name);
            final File epub = new File(out, name + ".epub");
            epub.delete();
            final long t = System.nanoTime();
            final int result = LibMobi.convertToEpub(book.getPath(), epub.getPath());
            Log.i(TAG, String.format(Locale.US, "%s -> epub: %d, %d bytes, %d ms",
                    name, result, epub.length(), ms(t)));
            assertEquals("libmobi converted " + name, 0, result);
            assertTrue("an epub came out of " + name, epub.isFile() && epub.length() > 0);
            assertTrue("and it is a zip", startsWith(epub, "PK"));
        }
    }

    @Test
    public void docConvertsWithLibAntiword() throws Exception {
        final File book = book("sample.doc");
        final File html = new File(out, "sample.doc.html");
        html.delete();
        final long t = System.nanoTime();
        final int result = LibMobi.convertDocToHtml(book.getPath(), html.getPath());
        Log.i(TAG, String.format(Locale.US, "doc -> html: %d, %d bytes, %d ms", result, html.length(), ms(t)));
        assertTrue("an html came out", html.isFile() && html.length() > 0);
        final String text = read(html);
        assertTrue("with the document in it: " + text.substring(0, Math.min(200, text.length())),
                text.toLowerCase(Locale.US).contains("<html") || text.length() > 100);
    }

    @Test
    public void theBitmapScalerRunsInLibHqx() {
        final int w = 16;
        final int h = 16;
        final RawBitmap raw = new RawBitmap(w, h, false);
        final int[] pixels = raw.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (i % 3 == 0) ? 0xFF000000 : 0xFFFFFFFF;
        }
        // each of them is a native call into libhqx.so, whatever book was open last
        final BitmapRef[] scaled = {raw.scaleHq2x(), raw.scaleHq3x(), raw.scaleHq4x()};
        for (int k = 0; k < scaled.length; k++) {
            assertNotNull(scaled[k]);
            assertEquals(w * (k + 2), scaled[k].getBitmap().getWidth());
            assertTrue("hq" + (k + 2) + "x drew the picture", hasContent(scaled[k].getBitmap()));
        }
    }

    private File book(final String name) {
        final File file = new File(dir, name);
        assumeTrue("push " + name + " into " + dir, file.isFile());
        return file;
    }

    /** Not one flat colour: a page that failed to decode comes out blank. */
    private static boolean hasContent(final Bitmap bitmap) {
        final int first = bitmap.getPixel(0, 0);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (bitmap.getPixel(bitmap.getWidth() * x / 10, bitmap.getHeight() * y / 10) != first) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean startsWith(final File file, final String magic) throws Exception {
        final byte[] head = new byte[magic.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            return in.read(head) == head.length && magic.equals(new String(head, StandardCharsets.US_ASCII));
        }
    }

    private static String read(final File file) throws Exception {
        final byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int n = 0;
            int r;
            while (n < bytes.length && (r = in.read(bytes, n, bytes.length - n)) > 0) {
                n += r;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static long ms(final long since) {
        return (System.nanoTime() - since) / 1000000;
    }
}
