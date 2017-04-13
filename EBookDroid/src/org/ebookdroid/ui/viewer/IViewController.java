package org.ebookdroid.ui.viewer;

import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.core.EventDraw;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.events.ZoomListener;
import org.emdev.ui.progress.IProgressIndicator;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;

public interface IViewController extends ZoomListener {

    public boolean isInitialized();

    void init(IProgressIndicator bookLoadTask);

    void show();

    /* Page related methods */
    ViewState goToPage(int page);

    ViewState goToPage(int page, boolean animate);

    ViewState goToPage(int page, float offsetX, float offsetY);

    ViewState goToLink(int pageDocIndex, RectF targetRect, boolean addToHistory);

    RectF calcPageBounds(PageAlign pageAlign, float pageAspectRatio, int width, int height);

    void invalidatePageSizes(InvalidateSizeReason reason, Page changedPage);

    int getFirstVisiblePage();

    int calculateCurrentPage(ViewState viewState, int firstVisible, int lastVisible);

    int getLastVisiblePage();

    void verticalConfigScroll(int i);

    void redrawView();

    void redrawView(ViewState viewState);

    void setAlign(PageAlign byResValue);

    /* Infrastructure methods */

    IActivityController getBase();

    IView getView();

    void updateAnimationType();

    void updateMemorySettings();

    public static enum InvalidateSizeReason {
        INIT, LAYOUT, PAGE_ALIGN, PAGE_LOADED;
    }

    boolean onLayoutChanged(boolean layoutChanged, boolean layoutLocked, Rect oldLaout, Rect newLayout);

    Rect getScrollLimits();

    boolean isPageVisible(Page page, ViewState viewState);
    
    boolean onTouchEvent(MotionEvent ev);

    void onScrollChanged(int dX, final int dY);

    void toggleRenderingEffects();

    void drawView(EventDraw eventDraw);

    void pageUpdated(ViewState viewState, Page page);

    void invalidateScroll();

    void onDestroy();
    void clearSelectedText();
}
