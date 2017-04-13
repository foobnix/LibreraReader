package org.ebookdroid.common.bitmaps;

import org.ebookdroid.core.PagePaint;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region.Op;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.emdev.utils.LengthUtils;
import org.emdev.utils.MathUtils;

public class Bitmaps {


    private static final Config DEF_BITMAP_TYPE = Bitmap.Config.RGB_565;

    private static boolean useDefaultBitmapType = true;

    private static final ThreadLocal<RawBitmap> threadSlices = new ThreadLocal<RawBitmap>();

    public final Bitmap.Config config;
    public final int partSize;
    public Rect bounds;
    public int columns;
    public int rows;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private BitmapRef[] bitmaps;

    public Bitmaps(final String nodeId, final BitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        final Bitmap origBitmap = orig.getBitmap();

        this.partSize = BitmapManager.partSize;
        this.bounds = bitmapBounds;
        this.columns = (int) Math.ceil(bounds.width() / (float) partSize);
        this.rows = (int) Math.ceil(bounds.height() / (float) partSize);
        this.config = useDefaultBitmapType ? DEF_BITMAP_TYPE : origBitmap.getConfig();
        this.bitmaps = new BitmapRef[columns * rows];

        final boolean hasAlpha = origBitmap.hasAlpha();
        RawBitmap slice = threadSlices.get();
        if (slice == null || slice.pixels.length < partSize * partSize || slice.hasAlpha != hasAlpha) {
            slice = new RawBitmap(partSize, partSize, hasAlpha);
            threadSlices.set(slice);
        }

        int top = 0;
        for (int row = 0; row < rows; row++, top += partSize) {
            int left = 0;
            for (int col = 0; col < columns; col++, left += partSize) {
                final String name = nodeId + ":[" + row + ", " + col + "]";
                final BitmapRef b = BitmapManager.getBitmap(name, partSize, partSize, config);
                final Bitmap bmp = b.getBitmap();

                if (row == rows - 1 || col == columns - 1) {
                    final int right = Math.min(left + partSize, bounds.width());
                    final int bottom = Math.min(top + partSize, bounds.height());
                    bmp.eraseColor(invert ? PagePaint.NIGHT.fillPaint.getColor() : PagePaint.DAY.fillPaint.getColor());
                    slice.retrieve(origBitmap, left, top, right - left, bottom - top);
                } else {
                    slice.retrieve(origBitmap, left, top, partSize, partSize);
                }
                if (invert) {
                    slice.invert();
                }
                slice.toBitmap(bmp);

                final int index = row * columns + col;
                bitmaps[index] = b;
            }
        }
    }

    public boolean reuse(final String nodeId, final BitmapRef orig, final Rect bitmapBounds, final boolean invert) {
        lock.writeLock().lock();
        try {
            final Bitmap origBitmap = orig.getBitmap();
            final Config cfg = useDefaultBitmapType ? DEF_BITMAP_TYPE : origBitmap.getConfig();
            if (cfg != this.config) {
                return false;
            }
            if (BitmapManager.partSize != this.partSize) {
                return false;
            }

            final BitmapRef[] oldBitmaps = this.bitmaps;
            final int oldBitmapsLength = LengthUtils.length(oldBitmaps);

            this.bounds = bitmapBounds;
            this.columns = (int) Math.ceil(bitmapBounds.width() / (float) partSize);
            this.rows = (int) Math.ceil(bitmapBounds.height() / (float) partSize);
            this.bitmaps = new BitmapRef[columns * rows];

            final int newsize = this.columns * this.rows;

            int i = 0;
            for (; i < newsize; i++) {
                if (i < oldBitmapsLength) {
                    this.bitmaps[i] = oldBitmaps[i];
                    if (this.bitmaps[i] != null && this.bitmaps[i].isRecycled()) {
                        BitmapManager.release(this.bitmaps[i]);
                        this.bitmaps[i] = null;
                    }
                }
                if (this.bitmaps[i] == null) {
                    this.bitmaps[i] = BitmapManager.getBitmap(nodeId + ":reuse:" + i, partSize, partSize, config);
                }
                this.bitmaps[i].getBitmap().eraseColor(Color.CYAN);
            }
            for (; i < oldBitmapsLength; i++) {
                BitmapManager.release(oldBitmaps[i]);
            }

            final boolean hasAlpha = origBitmap.hasAlpha();
            RawBitmap slice = threadSlices.get();
            if (slice == null || slice.pixels.length < partSize * partSize || slice.hasAlpha != hasAlpha) {
                slice = new RawBitmap(partSize, partSize, hasAlpha);
                threadSlices.set(slice);
            }

            int top = 0;
            for (int row = 0; row < rows; row++, top += partSize) {
                int left = 0;
                for (int col = 0; col < columns; col++, left += partSize) {
                    final int index = row * columns + col;
                    final BitmapRef b = bitmaps[index];
                    final Bitmap bmp = b.getBitmap();

                    if (row == rows - 1 || col == columns - 1) {
                        final int right = Math.min(left + partSize, bounds.width());
                        final int bottom = Math.min(top + partSize, bounds.height());
                        bmp.eraseColor(invert ? PagePaint.NIGHT.fillPaint.getColor() : PagePaint.DAY.fillPaint
                                .getColor());
                        slice.retrieve(origBitmap, left, top, right - left, bottom - top);
                    } else {
                        slice.retrieve(origBitmap, left, top, partSize, partSize);
                    }
                    if (invert) {
                        slice.invert();
                    }
                    slice.toBitmap(bmp);
                }
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean hasBitmaps() {
        lock.readLock().lock();
        try {
            if (bitmaps == null) {
                return false;
            }
            for (int i = 0; i < bitmaps.length; i++) {
                if (bitmaps[i] == null) {
                    return false;
                }
                if (bitmaps[i].isRecycled()) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    BitmapRef[] clear() {
        lock.writeLock().lock();
        try {
            final BitmapRef[] refs = this.bitmaps;
            this.bitmaps = null;
            return refs;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        lock.writeLock().lock();
        try {
            if (bitmaps != null) {
                for (final BitmapRef ref : bitmaps) {
                    BitmapManager.release(ref);
                }
                bitmaps = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean draw(final Canvas canvas, final PagePaint paint, final PointF vb, final RectF tr, final RectF cr) {
        lock.readLock().lock();
        try {
            if (this.bitmaps == null) {
                return false;
            }

            final Rect orig = canvas.getClipBounds();
            canvas.clipRect(cr.left - vb.x, cr.top - vb.y, cr.right - vb.x, cr.bottom - vb.y, Op.INTERSECT);

            final float offsetX = tr.left - vb.x;
            final float offsetY = tr.top - vb.y;

            final float scaleX = tr.width() / bounds.width();
            final float scaleY = tr.height() / bounds.height();

            final float sizeX = partSize * scaleX;
            final float sizeY = partSize * scaleY;

            final Rect src = new Rect();
            final RectF rect = new RectF(offsetX, offsetY, offsetX + sizeX, offsetY + sizeY);
            final RectF r = new RectF();

            boolean res = true;
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    final int index = row * columns + col;
                    final BitmapRef ref = this.bitmaps[index];
                    if (ref != null) {
                        r.set(rect);
                        if (ref.bitmap != null) {
                            try {
                                src.set(0, 0, ref.bitmap.getWidth(), ref.bitmap.getHeight());
                                canvas.drawBitmap(ref.bitmap, src, MathUtils.round(r), paint.bitmapPaint);
                            } catch (final Throwable th) {
                            }
                        }
                    } else {
                        res = false;
                    }
                    rect.left += sizeX;
                    rect.right += sizeX;
                }
                rect.left = offsetX;
                rect.right = offsetX + sizeX;

                rect.top += sizeY;
                rect.bottom += sizeY;
            }
            canvas.clipRect(orig, Op.REPLACE);
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }
}
