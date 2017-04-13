package org.ebookdroid.ui.viewer.stubs;

import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IView;
import org.ebookdroid.ui.viewer.IViewController;
import org.emdev.ui.progress.IProgressIndicator;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

public class ViewContollerStub implements IViewController {

    public static final ViewContollerStub STUB = new ViewContollerStub();

    @Override
    public void zoomChanged(final float oldZoom, final float newZoom, final boolean committed) {
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public ViewState goToPage(final int page) {
        return null;
    }

    @Override
    public ViewState goToPage(final int page, final float offsetX, final float offsetY) {
        return null;
    }

    @Override
    public void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
    }

    @Override
    public int getFirstVisiblePage() {
        return 0;
    }

    @Override
    public int calculateCurrentPage(final ViewState viewState, final int firstVisible, final int lastVisible) {
        return 0;
    }

    @Override
    public int getLastVisiblePage() {
        return 0;
    }

    @Override
    public void verticalConfigScroll(final int i) {
    }
    @Override
    public void clearSelectedText() {
    }

    @Override
    public void redrawView() {
    }

    @Override
    public void redrawView(final ViewState viewState) {
    }

    @Override
    public void setAlign(final PageAlign byResValue) {
    }

    @Override
    public IActivityController getBase() {
        return ActivityControllerStub.STUB;
    }

    @Override
    public IView getView() {
        return ViewStub.STUB;
    }

    @Override
    public void updateAnimationType() {
    }

    @Override
    public void updateMemorySettings() {
    }

    @Override
    public boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout,
            final Rect newLayout) {
        return false;
    }

    @Override
    public Rect getScrollLimits() {
        return new Rect(0, 0, 0, 0);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent ev) {
        return false;
    }

    @Override
    public void onScrollChanged(final int dX, final int dY) {
    }


    @Override
    public void show() {
    }

    @Override
    public final void init(final IProgressIndicator task) {
    }

    @Override
    public void toggleRenderingEffects() {
    }

    @Override
    public void drawView(final EventDraw eventDraw) {
    }

    @Override
    public boolean isPageVisible(final Page page, final ViewState viewState) {
        return false;
    }

    @Override
    public void pageUpdated(final ViewState viewState, final Page page) {
    }

    @Override
    public void invalidateScroll() {
    }

    @Override
    public final void onDestroy() {
    }

    @Override
    public RectF calcPageBounds(final PageAlign pageAlign, final float pageAspectRatio, final int width,
            final int height) {
        return new RectF();
    }

    @Override
    public ViewState goToLink(final int pageDocIndex, final RectF targetRect, boolean history) {
        return null;
    }

    @Override
    public ViewState goToPage(int page, boolean animate) {
        // TODO Auto-generated method stub
        return null;
    }

}
