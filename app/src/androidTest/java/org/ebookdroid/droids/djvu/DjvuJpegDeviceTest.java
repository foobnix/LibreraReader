package org.ebookdroid.droids.djvu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * A DjVu page whose picture is a JPEG - a BGjp chunk - is drawn by libdjvu.so through
 * libjpeg-turbo, the libjpeg it shares with libtiff.so. Few DjVu books carry JPEG, none of the
 * test library's does, so the test writes one of its own: a one-page DjVu of a JPEG made here,
 * which has to come out as Android's own decoder draws that JPEG.
 */
@RunWith(AndroidJUnit4.class)
public class DjvuJpegDeviceTest {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;

    @Test
    public void aJpegPageIsDrawnAsTheJpegIs() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CacheZipUtils.init(context);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.init(context);
        TempHolder.get().loadingCancelled.set(false);

        final byte[] jpeg = jpeg();
        final File file = new File(context.getCacheDir(), "djvu-jpeg-test.djvu");
        final FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(djvu(jpeg));
        } finally {
            out.close();
        }

        final CodecContext codec = BookType.getCodecContextByPath(file.getPath());
        assertTrue("DjVu is read by DjvuContext: " + codec, codec instanceof DjvuContext);
        final CodecDocument document = codec.openDocument(file.getPath(), "");
        try {
            assertNotNull("the DjVu opens", document);
            assertEquals(1, document.getPageCount());
            final BitmapRef ref = document.getPage(0).renderBitmapSimple(WIDTH, HEIGHT, new RectF(0, 0, 1, 1));
            assertNotNull("the page is drawn", ref);
            final Bitmap drawn = ref.getBitmap();
            final Bitmap expected = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);

            double off = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    final int e = expected.getPixel(x, y);
                    final int a = drawn.getPixel(x, y);
                    off += Math.abs(Color.red(e) - Color.red(a)) + Math.abs(Color.green(e) - Color.green(a))
                            + Math.abs(Color.blue(e) - Color.blue(a));
                }
            }
            off /= WIDTH * HEIGHT * 3;
            assertTrue("the page is the JPEG, " + off + " off a level on average", off < 4);
            // and not a blank one that happens to be close to a pale picture
            assertTrue(Color.red(drawn.getPixel(WIDTH / 4, HEIGHT / 2)) > 200);
            assertTrue(Color.blue(drawn.getPixel(WIDTH / 4, HEIGHT / 2)) < 80);
        } finally {
            if (document != null) {
                document.recycle();
            }
            file.delete();
        }
    }

    /** A picture with a gradient and a red disc, as a JPEG. */
    private static byte[] jpeg() {
        final Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                bitmap.setPixel(x, y, Color.rgb(x * 255 / WIDTH, y * 255 / HEIGHT, 160));
            }
        }
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(240, 20, 30));
        new Canvas(bitmap).drawCircle(WIDTH / 4f, HEIGHT / 2f, HEIGHT / 4f, paint);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
        return out.toByteArray();
    }

    /**
     * A single-page DjVu: an INFO chunk saying the page's size, resolution and gamma, and a BGjp
     * chunk holding the JPEG as the page's background, at the page's own size.
     */
    private static byte[] djvu(final byte[] jpeg) throws Exception {
        final ByteArrayOutputStream form = new ByteArrayOutputStream();
        form.write("DJVU".getBytes("US-ASCII"));
        chunk(form, "INFO", new byte[]{
                (byte) (WIDTH >> 8), (byte) WIDTH, (byte) (HEIGHT >> 8), (byte) HEIGHT,
                24, 0, // minor, major version
                100, 0, // dpi, little-endian
                22, // gamma 2.2
                1}); // rotation: none
        chunk(form, "BGjp", jpeg);

        final ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.write("AT&T".getBytes("US-ASCII"));
        chunk(file, "FORM", form.toByteArray());
        return file.toByteArray();
    }

    private static void chunk(final ByteArrayOutputStream out, final String id, final byte[] data) throws Exception {
        out.write(id.getBytes("US-ASCII"));
        out.write(new byte[]{(byte) (data.length >> 24), (byte) (data.length >> 16), (byte) (data.length >> 8),
                (byte) data.length});
        out.write(data);
        if ((data.length & 1) == 1) {
            out.write(0);
        }
    }
}
