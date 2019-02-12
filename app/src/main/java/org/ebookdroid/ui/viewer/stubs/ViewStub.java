package org.ebookdroid.ui.viewer.stubs;

import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.widget.Scroller;

public class ViewStub implements IView {

    public static final ViewStub STUB = new ViewStub();

    @Override
    public View getView() {
        return null;
    }

    @Override
    public IActivityController getBase() {
        return ActivityControllerStub.STUB;
    }

    @Override
    public Scroller getScroller() {
        return null;
    }

    @Override
    public void invalidateScroll() {
    }

    @Override
    public void invalidateScroll(final float newZoom, final float oldZoom) {
    }

    @Override
    public void startPageScroll(final int dx, final int dy) {
    }

    @Override
    public void startFling(final float vX, final float vY, final Rect limits) {
    }

    @Override
    public void continueScroll() {
    }

    @Override
    public void forceFinishScroll() {
    }

    @Override
    public void scrollBy(final int x, final int y) {
    }

    @Override
    public void scrollTo(final int x, final int y) {
    }

    @Override
    public RectF getViewRect() {
        return new RectF(0, 0, 0, 0);
    }

    @Override
    public void changeLayoutLock(final boolean lock) {
    }

    @Override
    public boolean isLayoutLocked() {
        return false;
    }

    @Override
    public void waitForInitialization() {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public float getScrollScaleRatio() {
        return 1.0f;
    }

    @Override
    public void stopScroller() {
    }

    @Override
    public void redrawView() {
    }

    @Override
    public void redrawView(final ViewState viewState) {
    }

    @Override
    public int getScrollX() {
        return 0;
    }

    @Override
    public int getScrollY() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public PointF getBase(final RectF viewRect) {
        return new PointF(0, 0);
    }

    @Override
    public void checkFullScreenMode() {
    }

}
