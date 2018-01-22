package org.ebookdroid.core;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.common.settings.AppSettings;
import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.ui.viewer.IViewController;
import org.emdev.utils.MatrixUtils;

import com.foobnix.pdf.info.wrapper.AppState;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

public class PageTreeNode implements DecodeService.DecodeCallback {

    final Page page;
    final PageTreeNode parent;
    final int id;
    final PageTreeLevel level;
    final String shortId;
    final String fullId;

    final AtomicBoolean decodingNow = new AtomicBoolean();
    final BitmapHolder holder = new BitmapHolder();

    final RectF pageSliceBounds;

    float bitmapZoom = 1;
    RectF croppedBounds = null;

    PageTreeNode(final Page page) {
        this.page = page;
        this.parent = null;
        this.id = 0;
        this.level = PageTreeLevel.ROOT;
        this.shortId = page.index.viewIndex + ":0";
        this.fullId = page.index + ":0";
        this.pageSliceBounds = page.type.getInitialRect();
        this.croppedBounds = null;
    }

    PageTreeNode(final Page page, final PageTreeNode parent, final int id, final RectF localPageSliceBounds) {
        this.page = page;
        this.parent = parent;
        this.id = id;
        this.level = parent.level.next;
        this.shortId = page.index.viewIndex + ":" + id;
        this.fullId = page.index + ":" + id;
        this.pageSliceBounds = evaluatePageSliceBounds(localPageSliceBounds, parent);
        this.croppedBounds = evaluateCroppedPageSliceBounds(localPageSliceBounds, parent);
    }

    @Override
    protected void finalize() throws Throwable {
        holder.recycle(null);
    }

    public boolean recycle(final List<Bitmaps> bitmapsToRecycle) {
        stopDecodingThisNode("node recycling");
        return holder.recycle(bitmapsToRecycle);
    }

    protected void decodePageTreeNode(final List<PageTreeNode> nodesToDecode, final ViewState viewState) {
        if (this.decodingNow.compareAndSet(false, true)) {
            bitmapZoom = viewState.zoom;
            nodesToDecode.add(this);
        }
    }

    void stopDecodingThisNode(final String reason) {
        if (this.decodingNow.compareAndSet(true, false)) {
            final DecodingProgressModel dpm = page.base.getDecodingProgressModel();
            if (dpm != null) {
                dpm.decrease();
            }
            if (reason != null) {
                final DecodeService ds = page.base.getDecodeService();
                if (ds != null) {
                    ds.stopDecoding(this, reason);
                }
            }
        }
    }

    @Override
    public void decodeComplete(final CodecPage codecPage, final BitmapRef bitmap, final Rect bitmapBounds,
            final RectF croppedPageBounds) {

        try {
            if (bitmap == null || bitmapBounds == null) {
                page.base.getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        stopDecodingThisNode(null);
                    }
                });
                return;
            }

            final Bitmaps bitmaps = holder.reuse(fullId, bitmap, bitmapBounds);

            final Runnable r = new Runnable() {

                @Override
                public void run() {
                    // long t0 = System.currentTimeMillis();
                    holder.setBitmap(bitmaps);
                    stopDecodingThisNode(null);

                    final IViewController dc = page.base.getDocumentController();
                    if (dc instanceof AbstractViewController) {
                        EventPool.newEventChildLoaded((AbstractViewController) dc, PageTreeNode.this, bitmapBounds)
                                .process();
                    }

                    // System.out.println("decodeComplete(): " + (System.currentTimeMillis() - t0) + " ms");
                }
            };

            page.base.getActivity().runOnUiThread(r);
        } catch (final OutOfMemoryError ex) {
            ex.printStackTrace();
            BitmapManager.clear("PageTreeNode OutOfMemoryError: ");
            page.base.getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    stopDecodingThisNode(null);
                }
            });
        } finally {
            BitmapManager.release(bitmap);
        }
    }

    public RectF getTargetRect(final RectF pageBounds) {
        return Page.getTargetRect(page.type, pageBounds, pageSliceBounds);
    }

    public static RectF evaluatePageSliceBounds(final RectF localPageSliceBounds, final PageTreeNode parent) {
        final Matrix tmpMatrix = MatrixUtils.get();

        tmpMatrix.postScale(parent.pageSliceBounds.width(), parent.pageSliceBounds.height());
        tmpMatrix.postTranslate(parent.pageSliceBounds.left, parent.pageSliceBounds.top);
        final RectF sliceBounds = new RectF();
        tmpMatrix.mapRect(sliceBounds, localPageSliceBounds);
        return sliceBounds;
    }

    public static RectF evaluateCroppedPageSliceBounds(final RectF localPageSliceBounds, final PageTreeNode parent) {
        if (parent == null) {
            return null;
        }
        if (parent.croppedBounds == null) {
            parent.croppedBounds = evaluateCroppedPageSliceBounds(parent.pageSliceBounds, parent.parent);
            if (parent.croppedBounds == null) {
                return null;
            }
        }

        final Matrix tmpMatrix = MatrixUtils.get();

        tmpMatrix.postScale(parent.croppedBounds.width(), parent.croppedBounds.height());
        tmpMatrix.postTranslate(parent.croppedBounds.left, parent.croppedBounds.top);
        final RectF sliceBounds = new RectF();
        tmpMatrix.mapRect(sliceBounds, localPageSliceBounds);

        return sliceBounds;
    }

    @Override
    public int hashCode() {
        return (page == null) ? 0 : page.index.viewIndex;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PageTreeNode) {
            final PageTreeNode that = (PageTreeNode) obj;
            if (this.page == null) {
                return that.page == null;
            }
            return this.page.index.viewIndex == that.page.index.viewIndex
                    && this.pageSliceBounds.equals(that.pageSliceBounds);
        }

        return false;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder("PageTreeNode");
        buf.append("[");

        buf.append("id").append("=").append(page.index.viewIndex).append(":").append(id);
        buf.append(", ");
        buf.append("rect").append("=").append(this.pageSliceBounds);
        buf.append(", ");
        buf.append("hasBitmap").append("=").append(holder.hasBitmaps());

        buf.append("]");
        return buf.toString();
    }

    class BitmapHolder {

        final AtomicReference<Bitmaps> ref = new AtomicReference<Bitmaps>();

        public boolean drawBitmap(final Canvas canvas, final PagePaint paint, final PointF viewBase,
                final RectF targetRect, final RectF clipRect) {
            final Bitmaps bitmaps = ref.get();
            return bitmaps != null ? bitmaps.draw(canvas, paint, viewBase, targetRect, clipRect) : false;
        }

        public Bitmaps reuse(final String nodeId, final BitmapRef bitmap, final Rect bitmapBounds) {
            final BookSettings bs = SettingsManager.getBookSettings();
            final AppSettings app = AppSettings.getInstance();
            // final boolean invert = bs != null ? bs.nightMode : app.nightMode;
            final boolean invert = !AppState.get().isDayNotInvert;
            if (app.textureReuseEnabled) {
                final Bitmaps bitmaps = ref.get();
                if (bitmaps != null) {
                    if (bitmaps.reuse(nodeId, bitmap, bitmapBounds, invert)) {
                        return bitmaps;
                    }
                }
            }
            return new Bitmaps(nodeId, bitmap, bitmapBounds, invert);
        }

        public boolean hasBitmaps() {
            final Bitmaps bitmaps = ref.get();
            return bitmaps != null ? bitmaps.hasBitmaps() : false;
        }

        public boolean recycle(final List<Bitmaps> bitmapsToRecycle) {
            final Bitmaps bitmaps = ref.getAndSet(null);
            if (bitmaps != null) {
                if (bitmapsToRecycle != null) {
                    bitmapsToRecycle.add(bitmaps);
                } else {
                    BitmapManager.release(Arrays.asList(bitmaps));
                }
                return true;
            }
            return false;
        }

        public void setBitmap(final Bitmaps bitmaps) {
            if (bitmaps == null) {
                return;
            }
            final Bitmaps oldBitmaps = ref.getAndSet(bitmaps);
            if (oldBitmaps != null && oldBitmaps != bitmaps) {
                BitmapManager.release(Arrays.asList(oldBitmaps));
            }
        }
    }
}
