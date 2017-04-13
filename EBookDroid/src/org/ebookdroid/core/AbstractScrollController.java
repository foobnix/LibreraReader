package org.ebookdroid.core;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.ui.viewer.IActivityController;

import android.graphics.Rect;
import android.graphics.RectF;

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
    public final boolean onLayoutChanged(final boolean layoutChanged, final boolean layoutLocked, final Rect oldLaout,
            final Rect newLayout) {
        final BookSettings bs = SettingsManager.getBookSettings();
        final int page = model != null ? model.getCurrentViewPageIndex() : -1;
        final float offsetX = bs != null ? bs.offsetX : 0;
        final float offsetY = bs != null ? bs.offsetY : 0;

        if (super.onLayoutChanged(layoutChanged, layoutLocked, oldLaout, newLayout)) {
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
            return;
        }

        EventPool.newEventScroll(this, mode == DocumentViewMode.VERTICALL_SCROLL ? dY : dX).process();
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
