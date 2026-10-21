package org.ebookdroid.droids.comic.codec;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.model.AnnotationType;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.AbstractCodecPage;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.ArrayList;
import java.util.List;

/**
 * One image of a comic archive, drawn as a page. See {@link ComicContext}.
 */
public class ComicPage extends AbstractCodecPage {

    private static final int WHITE = Color.rgb(255, 255, 255);

    private final ComicDocument document;
    private final int pageNumber;

    /** The archive this page came from, for the logs; AbstractCodecPage keeps its own copy. */
    private final String fname;

    private volatile boolean recycled;

    ComicPage(final ComicDocument document, final String path, final int pageNumber) {
        super(path);
        this.fname = path;
        this.document = document;
        this.pageNumber = pageNumber;
    }

    private CodecPageInfo info() {
        final CodecPageInfo info = document.getPageInfo(pageNumber);
        return info == null ? new CodecPageInfo() : info;
    }

    @Override
    public int getWidth() {
        return info().width;
    }

    @Override
    public int getHeight() {
        return info().height;
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public BitmapRef renderBitmap(final int width, final int height, final RectF pageSliceBounds, final boolean cache) {
        return render(width, height, pageSliceBounds, true);
    }

    @Override
    public BitmapRef renderBitmapSimple(final int width, final int height, final RectF pageSliceBounds) {
        return render(width, height, pageSliceBounds, false);
    }

    @Override
    public Bitmap renderThumbnail(final int width) {
        return renderThumbnail(width, getWidth(), getHeight());
    }

    @Override
    public Bitmap renderThumbnail(final int width, int originW, int originH) {
        if (originW <= 0 || originH <= 0) {
            originW = getWidth();
            originH = getHeight();
        }
        if (originW <= 0 || originH <= 0) {
            return null;
        }
        final float k = (float) originH / originW;
        final BitmapRef bitmap = renderBitmap(width, (int) (width * k), new RectF(0, 0, 1f, 1f), false);
        return bitmap == null ? null : bitmap.getBitmap();
    }

    /**
     * Draws a slice of the image at the given size.
     *
     * The reader asks for a {@code width x height} bitmap holding the part of the page
     * {@code pageSliceBounds} names, both as fractions of the whole page - the same request the
     * other two engines answer by scaling the page up by 1/slice and shifting the slice into
     * view. Here the page is an image, so the work is: decode it no larger than the whole page
     * would be at this zoom, then copy the slice's rectangle onto the bitmap.
     *
     * @param withColors whether the reader's colour settings apply - the night-mode recolouring,
     *                   the contrast and brightness and the mirroring. The thumbnails and the
     *                   library's covers ask without them.
     */
    private BitmapRef render(final int width, final int height, final RectF pageSliceBounds, final boolean withColors) {
        if (width <= 0 || height <= 0 || recycled) {
            return null;
        }

        final CodecPageInfo info = info();
        if (info.width <= 0 || info.height <= 0) {
            return null;
        }

        final float sliceW = pageSliceBounds.width() > 0 ? pageSliceBounds.width() : 1f;
        final float sliceH = pageSliceBounds.height() > 0 ? pageSliceBounds.height() : 1f;

        // The size the whole page has at this zoom, which is what the slice is a fraction of
        final int pageW = Math.max(1, Math.round(width / sliceW));
        final int pageH = Math.max(1, Math.round(height / sliceH));

        final Bitmap decoded = decode(info, pageW, pageH);
        if (decoded == null) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(width, height, AppsConfig.CURRENT_BITMAP_ARGB);

            // Where the slice sits inside the image as it was decoded
            final Rect src = new Rect(
                    Math.round(pageSliceBounds.left * decoded.getWidth()),
                    Math.round(pageSliceBounds.top * decoded.getHeight()),
                    Math.round(pageSliceBounds.right * decoded.getWidth()),
                    Math.round(pageSliceBounds.bottom * decoded.getHeight()));
            // A slice thinner than one pixel of the decoded image still has to draw something
            if (src.right <= src.left) {
                src.right = Math.min(decoded.getWidth(), src.left + 1);
            }
            if (src.bottom <= src.top) {
                src.bottom = Math.min(decoded.getHeight(), src.top + 1);
            }

            final Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);

            final Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(background(withColors));
            canvas.drawBitmap(decoded, src, new Rect(0, 0, width, height), paint);
        } catch (Throwable e) {
            LOG.e(e);
            if (bitmap != null) {
                bitmap.recycle();
            }
            return null;
        } finally {
            decoded.recycle();
        }

        return postProcess(bitmap, width, height, withColors);
    }

    /**
     * Decodes the image no larger than it needs to be: BitmapFactory's inSampleSize halves it as
     * many times as it can while still covering the size asked for, so reading a page of a comic
     * at a page-fitting zoom costs a fraction of the full-resolution decode - and a 4000px scan
     * never has to be held in memory whole.
     */
    private Bitmap decode(final CodecPageInfo info, final int pageW, final int pageH) {
        final byte[] bytes = document.read(pageNumber);
        if (bytes == null) {
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = AppsConfig.CURRENT_BITMAP_ARGB;
        options.inSampleSize = sampleSize(info.width, info.height, pageW, pageH);
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Throwable e) {
            // A page too big to hold even sampled down: try again, smaller
            LOG.e(e, fname, pageNumber);
            options.inSampleSize = options.inSampleSize * 2;
            try {
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            } catch (Throwable e2) {
                LOG.e(e2);
                return null;
            }
        }
    }

    static int sampleSize(final int imageW, final int imageH, final int targetW, final int targetH) {
        int sample = 1;
        while (targetW > 0 && targetH > 0
                && imageW / (sample * 2) >= targetW
                && imageH / (sample * 2) >= targetH) {
            sample *= 2;
        }
        return sample;
    }

    /** The colour the page is drawn on, so a slice narrower than the image is not transparent. */
    private static int background(final boolean withColors) {
        if (withColors && MagicHelper.isNeedMagic() && !AppState.get().isCustomizeBgAndColors) {
            int color = MagicHelper.getBgColor();
            if (!AppState.get().isDayNotInvert) {
                color = ~color;
            }
            return opaque(color);
        }
        return WHITE;
    }

    /**
     * The reader's colour settings, applied the same way the other two engines apply them, so a
     * comic read at night looks like every other book does.
     */
    private BitmapRef postProcess(final Bitmap bitmap, final int width, final int height, final boolean withColors) {
        final boolean magic = withColors && MagicHelper.isNeedMagic() && AppState.get().isCustomizeBgAndColors;
        final boolean contrast = withColors && MagicHelper.isNeedBC;
        if (magic || contrast) {
            try {
                final int[] pixels = new int[width * height];
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
                if (magic) {
                    MagicHelper.udpateColorsMagic(pixels);
                }
                if (contrast) {
                    MagicHelper.applyQuickContrastAndBrightness(pixels, width, height);
                }
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            } catch (Throwable e) {
                LOG.e(e);
            }
        }

        final BitmapRef ref = new BitmapRef(bitmap, 0L);
        if (withColors && AppState.get().isMirrorImage) {
            try {
                final Matrix m = new Matrix();
                m.preScale(-1, 1);
                final Bitmap mirrored = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
                bitmap.recycle();
                ref.setBitmap(mirrored);
            } catch (Throwable e) {
                LOG.e(e);
            }
        }
        return ref;
    }

    private static int opaque(final int color) {
        return Color.argb(255, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ---------------------------------------------------------------- text and annotations

    @Override
    public TextWord[][] getTextImpl() {
        // A comic is scanned artwork: there is no text layer, which is what MuPDF reports for a
        // .cbz too. Selection, search and text-to-speech are off for these books either way.
        return new TextWord[0][0];
    }

    @Override
    public int getCharCount() {
        return 0;
    }

    @Override
    public String getPageHTML() {
        return "";
    }

    @Override
    public String getPageHTMLWithImages() {
        return "";
    }

    @Override
    public List<Annotation> getAnnotationsImpl() {
        return new ArrayList<Annotation>();
    }

    @Override
    public void addAnnotation(final float[] color, final PointF[][] points, final float width, final float alpha) {
        // Nothing to write them into: a .cbz is a zip of images and carries no annotation layer
    }

    @Override
    public void addMarkupAnnotation(final PointF[] quadPoints, final AnnotationType type, final float[] color) {
    }

    @Override
    public long getPageHandle() {
        return 0;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void recycle() {
        recycled = true;
    }

    @Override
    public boolean isRecycled() {
        return recycled || document.isRecycled();
    }
}
