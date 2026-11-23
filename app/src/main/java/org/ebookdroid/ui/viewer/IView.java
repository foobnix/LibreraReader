package org.ebookdroid.ui.viewer;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.Scroller;

import org.ebookdroid.core.ViewState;

public interface IView {

    View getView();

    IActivityController getBase();

    Scroller getScroller();

    void invalidateScroll();

    void invalidateScroll(final float newZoom, final float oldZoom);

    /**
     * The point on the screen a zoom keeps in place, the middle of the fingers that pinch;
     * NaN for the middle of the view.
     */
    void setZoomFocus(float x, float y);

    void startPageScroll(final int dx, final int dy);

    void startFling(final float vX, final float vY, final Rect limits);

    void continueScroll();

    void forceFinishScroll();

    void scrollBy(int x, int y);

    void scrollTo(final int x, final int y);

    RectF getViewRect();

    void onDestroy();

    float getScrollScaleRatio();

    void stopScroller();

    void redrawView();

    void redrawView(final ViewState viewState);

    int getScrollX();

    int getScrollY();

    int getWidth();

    int getHeight();

    PointF getBase(RectF viewRect);

}
