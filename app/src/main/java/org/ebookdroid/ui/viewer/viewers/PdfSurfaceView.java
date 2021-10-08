package org.ebookdroid.ui.viewer.viewers;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.foobnix.pdf.search.activity.msg.MessagePageXY;

import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.EventPool;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;
import org.emdev.utils.MathUtils;
import org.greenrobot.eventbus.EventBus;

public final class PdfSurfaceView extends android.view.View implements IView{


    protected final IActivityController base;

    protected final Scroller scroller;

    protected PageAlign align;

    public PdfSurfaceView(final IActivityController baseActivity) {
        super(baseActivity.getContext());
        this.base = baseActivity;
        this.scroller = new Scroller(getContext());
    }


    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getView()
     */
    @Override
    public final View getView() {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getBase()
     */
    @Override
    public final IActivityController getBase() {
        return base;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getScroller()
     */
    @Override
    public final Scroller getScroller() {
        return scroller;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#invalidateScroll()
     */
    @Override
    public final void invalidateScroll() {
        stopScroller();

        final float scrollScaleRatio = getScrollScaleRatio();
        scrollTo((int) (getScrollX() * scrollScaleRatio), (int) (getScrollY() * scrollScaleRatio));
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#invalidateScroll(float, float)
     */
    @Override
    public final void invalidateScroll(final float newZoom, final float oldZoom) {
        stopScroller();

        final float ratio = newZoom / oldZoom;
        final float halfWidth = getWidth() / 2.0f;
        final float halfHeight = getHeight() / 2.0f;

        final int x = (int) ((getScrollX() + halfWidth) * ratio - halfWidth);
        final int y = (int) ((getScrollY() + halfHeight) * ratio - halfHeight);


        scrollTo(x, y);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#startPageScroll(int, int)
     */
    @Override
    public void startPageScroll(final int dx, final int dy) {
        scroller.startScroll(getScrollX(), getScrollY(), dx, dy);
        redrawView();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#startFling(float, float,
     * android.graphics.Rect)
     */
    @Override
    public void startFling(final float vX, final float vY, final Rect limits) {
        scroller.fling(getScrollX(), getScrollY(), -(int) vX, -(int) vY, limits.left, limits.right, limits.top, limits.bottom);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#continueScroll()
     */
    @Override
    public void continueScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#forceFinishScroll()
     */
    @Override
    public void forceFinishScroll() {
        if (!scroller.isFinished()) { // is flinging
            scroller.forceFinished(true); // to stop flinging on touch
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see android.view.View#onScrollChanged(int, int, int, int)
     */
    @Override
    protected final void onScrollChanged(final int curX, final int curY, final int oldX, final int oldY) {
        super.onScrollChanged(curX, curY, oldX, oldY);
        base.getDocumentController().onScrollChanged(curX - oldX, curY - oldY);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.view.View#onTouchEvent(android.view.MotionEvent)
     */
    @Override
    public boolean onTouchEvent(final MotionEvent ev) {

        if (base.getDocumentController().onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }


    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#scrollTo(int, int)
     */
    @Override
    public final void scrollTo(final int x, final int y) {
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                final IViewController dc = base.getDocumentController();
                final Rect l = dc.getScrollLimits();

                final int xx = MathUtils.adjust(x, l.left, l.right);
                final int yy = MathUtils.adjust(y, l.top, l.bottom);
                PdfSurfaceView.super.scrollTo(xx, yy);
                EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
            }
        };
        base.getActivity().runOnUiThread(r);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getViewRect()
     */
    @Override
    public final RectF getViewRect() {
        return new RectF(getScrollX(), getScrollY(), getScrollX() + getWidth(), getScrollY() + getHeight());
    }




    /**
     * {@inheritDoc}
     *
     * @see android.view.View#onLayout(boolean, int, int, int, int)
     */
    @Override
    protected final void onLayout(final boolean layoutChanged, final int left, final int top, final int right, final int bottom) {
        super.onLayout(layoutChanged, left, top, right, bottom);
        base.getDocumentController().onLayoutChanged(layoutChanged);
    }


    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#onDestroy()
     */
    @Override
    public void onDestroy() {

    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getScrollScaleRatio()
     */
    @Override
    public float getScrollScaleRatio() {
        final Page page = base.getDocumentModel().getCurrentPageObject();
        final float zoom = base.getZoomModel().getZoom();

        if (page == null || page.getBounds(zoom) == null) {
            return 0;
        }
        return getWidth() * zoom / page.getBounds(zoom).width();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#stopScroller()
     */
    @Override
    public void stopScroller() {
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#redrawView()
     */
    @Override
    public final void redrawView() {
        EventBus.getDefault().post(new MessagePageXY(MessagePageXY.TYPE_HIDE));
        redrawView(new ViewState(base.getDocumentController()));
    }


    ViewState viewState;

    @Override
    public final void redrawView(final ViewState viewState) {
        this.viewState = viewState;
        if (viewState != null) {
            if (base.getDecodeService() != null) {
                base.getDecodeService().updateViewState(viewState);
            }
            postInvalidate();
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(viewState!=null) {

            Matrix matrix = canvas.getMatrix();
            matrix.reset();
            canvas.setMatrix(matrix);

            int save = canvas.save();
            EventPool.newEventDraw(viewState, canvas, null).process();
            canvas.restoreToCount(save);
        }


        super.onDraw(canvas);

    }



    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IView#getBase(android.graphics.RectF)
     */
    @Override
    public PointF getBase(final RectF viewRect) {
        return new PointF(viewRect.left, viewRect.top);
    }
}
