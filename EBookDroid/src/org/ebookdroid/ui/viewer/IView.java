package org.ebookdroid.ui.viewer;

import org.ebookdroid.core.ViewState;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.Scroller;

public interface IView {

    View getView();

    IActivityController getBase();

    Scroller getScroller();

    void invalidateScroll();

    void invalidateScroll(final float newZoom, final float oldZoom);

    void startPageScroll(final int dx, final int dy);

    void startFling(final float vX, final float vY, final Rect limits);

    void continueScroll();

    void forceFinishScroll();

    void scrollBy(int x, int y);

    void scrollTo(final int x, final int y);

    RectF getViewRect();

    void changeLayoutLock(final boolean lock);

    boolean isLayoutLocked();

    void waitForInitialization();

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

    void checkFullScreenMode();
}
