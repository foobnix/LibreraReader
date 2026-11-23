package org.ebookdroid.core;

import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppBook;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.ui.viewer.IActivityController;

public abstract class AbstractScrollController extends AbstractViewController {


    protected AbstractScrollController(final IActivityController base, final DocumentViewMode mode) {
        super(base, mode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#goToPage(int)
     */
    @Override
    public final ViewState goToPage(final int toPage) {
        return new EventGotoPage(this, toPage, false).process();
    }

    @Override
    public ViewState goToPageAndCenter(int page) {
        return new EventGotoPage(this, page, false, true).process();

    }

    @Override
    public final ViewState goToPage(final int toPage, boolean animate) {
        return new EventGotoPage(this, toPage, animate).process();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#goToPage(int, float, float)
     */
    @Override
    public final ViewState goToPage(final int toPage, final float offsetX, final float offsetY) {
        return new EventGotoPage(this, toPage, offsetX, offsetY).process();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#drawView(org.ebookdroid.core.EventDraw)
     */
    @Override
    public final void drawView(final EventDraw eventDraw) {
        final ViewState viewState = eventDraw.viewState;
        if (viewState.model == null) {
            return;
        }

        for (final Page page : viewState.pages.getVisiblePages()) {
            if (page != null) {
                eventDraw.process(page);
            }
        }

        getView().continueScroll();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractViewController#onLayoutChanged(boolean, boolean, android.graphics.Rect,
     *      android.graphics.Rect)
     */
    @Override
    public final boolean onLayoutChanged(final boolean layoutChanged) {
        LOG.d("onLayoutChanged");
        final AppBook bs = SettingsManager.getBookSettings();
        final int page = model != null ? model.getCurrentViewPageIndex() : -1;
        final float offsetX = bs != null ? bs.x : 0;
        final float offsetY = bs != null ? bs.y : 0;

        if (super.onLayoutChanged(layoutChanged)) {
            if (isShown && layoutChanged && page != -1) {
                goToPage(page, offsetX, offsetY);
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#onScrollChanged(int, int)
     */
    @Override
    public final void onScrollChanged(final int dX, final int dY) {
        if (inZoom.get()) {
            // Fingers that pinch also move the page. Draw it where it is now: the last zoom
            // step's view state still holds the old scroll, and a two-finger pan stuck to it
            // until the next step. Only the visible pages, no decoding: that waits for the
            // zoom to be committed.
            getView().redrawView(visibleState());
            return;
        }

        EventPool.newEventScroll(this, mode == DocumentViewMode.VERTICALL_SCROLL ? dY : dX).process();
    }

    /** The view as it is now, with the pages it shows. */
    private ViewState visibleState() {
        final ViewState initial = new ViewState(this);
        if (model == null) {
            return initial;
        }
        int first = -1;
        int last = -1;
        for (final Page page : model.getPages()) {
            if (isPageVisible(page, initial)) {
                if (first == -1) {
                    first = page.index.viewIndex;
                }
                last = page.index.viewIndex;
            } else if (first != -1) {
                break;
            }
        }
        return new ViewState(initial, first, last);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.AbstractViewController#isPageVisible(org.ebookdroid.core.Page,
     *      org.ebookdroid.core.ViewState)
     */
    @Override
    public final boolean isPageVisible(final Page page, final ViewState viewState) {
        return RectF.intersects(viewState.viewRect, viewState.getBounds(page));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#pageUpdated(org.ebookdroid.core.ViewState,
     *      org.ebookdroid.core.Page)
     */
    @Override
	public void pageUpdated(final ViewState viewState, final Page page) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.ui.viewer.IViewController#updateAnimationType()
     */
    @Override
    public void updateAnimationType() {
    }
}
