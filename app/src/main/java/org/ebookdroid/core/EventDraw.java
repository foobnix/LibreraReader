package org.ebookdroid.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.text.TextPaint;

import androidx.core.graphics.ColorUtils;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import org.ebookdroid.LibreraApp;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.ui.viewer.IActivityController;
import org.emdev.utils.LengthUtils;

import java.util.Queue;

public class EventDraw implements IEvent {

    static Paint rect = new Paint();

    static {
        rect.setColor(Color.DKGRAY);
        rect.setStrokeWidth(Dips.DP_1);
        rect.setStyle(Style.STROKE);

    }

    final RectF fixedPageBounds = new RectF();
    private final Queue<EventDraw> eventQueue;
    public ViewState viewState;
    public PageTreeLevel level;
    public Canvas canvas;
    RectF pageBounds;
    Paint paintWrods = new Paint();
    private IActivityController base;

    EventDraw(final Queue<EventDraw> eventQueue) {
        this.eventQueue = eventQueue;
        paintWrods.setAlpha(60);
        paintWrods.setStrokeWidth(Dips.dpToPx(1));
        paintWrods.setTextSize(30);
    }

    void init(final ViewState viewState, final Canvas canvas, IActivityController base) {
        this.viewState = viewState;
        this.base = base;
        this.level = PageTreeLevel.getLevel(viewState.zoom);
        this.canvas = canvas;

    }

    void init(final EventDraw event, final Canvas canvas, IActivityController base) {
        this.base = base;
        this.viewState = event.viewState;
        this.level = event.level;
        this.canvas = canvas;
    }

    void release() {
        this.canvas = null;
        this.level = null;
        this.pageBounds = null;
        this.viewState = null;
        eventQueue.offer(this);
    }

    @Override
    public ViewState process() {
        try {

            if (AppState.get().isOLED && !AppState.get().isDayNotInvert /* && MagicHelper.getBgColor() == Color.BLACK */) {
                viewState.paint.backgroundFillPaint.setColor(Color.BLACK);
            } else {
                viewState.paint.backgroundFillPaint.setColor(MagicHelper.ligtherColor(MagicHelper.getBgColor()));
            }
            if (canvas != null) {
                canvas.drawRect(canvas.getClipBounds(), viewState.paint.backgroundFillPaint);
            }

            viewState.ctrl.drawView(this);
            return viewState;
        } finally {
            release();
        }
    }

    @Override
    public boolean process(final Page page) {
        pageBounds = viewState.getBounds(page);

        drawPageBackground(page);

        final boolean res = process(page.nodes);

        if (MagicHelper.isNeedBookBackgroundImage()) {


            Bitmap bgBitmap = MagicHelper.getBackgroundImage();
            Matrix m = new Matrix();
            float width = fixedPageBounds.width();
            float height = fixedPageBounds.height();
            m.setScale(width / bgBitmap.getWidth(), height / bgBitmap.getHeight());
            m.postTranslate(fixedPageBounds.left, fixedPageBounds.top);

            Paint p = new Paint();
            p.setFilterBitmap(false);
            p.setAntiAlias(false);
            p.setDither(false);

            p.setAlpha(255 - MagicHelper.getTransparencyInt());
            canvas.drawBitmap(bgBitmap, m, p);
        }
        if (AppState.get().isOLED && !AppState.get().isDayNotInvert/* && !TempHolder.get().isTextFormat */) {
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.top - Dips.DP_1, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom + Dips.DP_1, rect);
        }

        if (AppState.get().isShowLastPageRed && AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN && page.isLastPage) {
            rect.setColor(ColorUtils.setAlphaComponent(Color.RED, 150));
            rect.setStyle(Style.FILL);
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.bottom - Dips.DP_25, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom + Dips.DP_1, rect);
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.bottom - fixedPageBounds.height() / 4 - Dips.DP_5, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom - fixedPageBounds.height() / 4, rect);

        } else if (AppState.get().isShowLineDividing && AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN) {
            rect.setColor(ColorUtils.setAlphaComponent(Color.GRAY, 200));
            rect.setStyle(Style.FILL);
            canvas.drawRect(fixedPageBounds.left - Dips.DP_1, fixedPageBounds.bottom - Dips.DP_2, fixedPageBounds.right + Dips.DP_1, fixedPageBounds.bottom + Dips.DP_1, rect);

        }


        // TODO Draw there
        // drawLine(page);
        if (!(BookCSS.get().isTextFormat() || AppSP.get().readingMode == AppState.READING_MODE_MUSICIAN)) {
            drawPageLinks(page);
        }
        // drawSomething(page);
        // drawHighlights(page);
        drawSelectedText(page);

        return res;
    }

    @Override
    public boolean process(final PageTree nodes) {
        return process(nodes, level);
    }

    @Override
    public boolean process(final PageTree nodes, final PageTreeLevel level) {
        return nodes.process(this, level, false);
    }

    @Override
    public boolean process(final PageTreeNode node) {
        final RectF nodeRect = node.getTargetRect(pageBounds);

        if (!viewState.isNodeVisible(nodeRect)) {
            return false;
        }

        try {
            if (node.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, nodeRect, nodeRect)) {
                return true;
            }

            if (node.parent != null) {
                final RectF parentRect = node.parent.getTargetRect(pageBounds);
                if (node.parent.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, parentRect, nodeRect)) {
                    return true;
                }
            }

            return node.page.nodes.paintChildren(this, node, nodeRect);

        } finally {
        }
    }

    public boolean paintChild(final PageTreeNode node, final PageTreeNode child, final RectF nodeRect) {
        final RectF childRect = child.getTargetRect(pageBounds);
        return child.holder.drawBitmap(canvas, viewState.paint, viewState.viewBase, childRect, nodeRect);
    }

    protected void drawPageBackground(final Page page) {
        if (canvas == null) {
            LOG.d("canvas is null");
            return;
        }

        fixedPageBounds.set(pageBounds);
        fixedPageBounds.offset(-viewState.viewBase.x, -viewState.viewBase.y);

        viewState.paint.fillPaint.setColor(MagicHelper.getBgColor());
        canvas.drawRect(fixedPageBounds, viewState.paint.fillPaint);

        final TextPaint textPaint = viewState.paint.textPaint;
        // textPaint.setTextSize(20 * viewState.z);
        textPaint.setTextSize(Dips.spToPx(16));
        textPaint.setColor(MagicHelper.getTextColor());

        final String text = LibreraApp.context.getString(R.string.text_page) + " " + (page.index.viewIndex + 1);
        canvas.drawText(text, fixedPageBounds.centerX(), fixedPageBounds.centerY(), textPaint);

    }


    private void drawPageLinks(final Page page) {

        if (LengthUtils.isEmpty(page.links)) {
            return;
        }

        paintWrods.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
        paintWrods.setAlpha(60);

        for (final PageLink link : page.links) {
            final RectF rect = page.getLinkSourceRect(pageBounds, link);
            if (rect != null) {
                rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
                // canvas.drawRect(rect, paintWrods);
                canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paintWrods);
            }
        }
    }

    private void drawSomething(final Page page) {
        final RectF link = new RectF(0.1f, 0.1f, 0.3f, 0.3f);
        final RectF rect = page.getPageRegion(pageBounds, new RectF(link));
        rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
        final Paint p = new Paint();
        p.setColor(Color.MAGENTA);
        p.setAlpha(40);
        canvas.drawRect(rect, p);
    }

    private void drawSelectedText(final Page page) {
        final Paint p = new Paint();
        p.setColor(AppState.get().isDayNotInvert ? Color.BLUE : Color.YELLOW);
        p.setAlpha(60);

        if (page.selectionAnnotion != null) {
            final RectF rect = page.getPageRegion(pageBounds, new RectF(page.selectionAnnotion));
            rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
            canvas.drawRect(rect, p);
        }

        if (page.selectedText.isEmpty()) {
            return;
        }
        for (RectF selected : page.selectedText) {
            final RectF rect = page.getPageRegion(pageBounds, new RectF(selected));
            rect.offset(-viewState.viewBase.x, -viewState.viewBase.y);
            canvas.drawRect(rect, p);
        }

    }

}
