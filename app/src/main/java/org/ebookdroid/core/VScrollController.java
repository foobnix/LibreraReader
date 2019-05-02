package org.ebookdroid.core;

import android.graphics.Rect;
import android.graphics.RectF;

import com.foobnix.model.AppBook;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.types.DocumentViewMode;
import org.ebookdroid.common.settings.types.PageAlign;
import org.ebookdroid.ui.viewer.IActivityController;

public class VScrollController extends AbstractScrollController {

    public VScrollController(final IActivityController base) {
        super(base, DocumentViewMode.VERTICALL_SCROLL);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#calculateCurrentPage(org.ebookdroid.core.ViewState)
     */
    @Override
    public final int calculateCurrentPage(final ViewState viewState, final int firstVisible, final int lastVisible) {
        int result = 0;
        long bestDistance = Long.MAX_VALUE;

        final int viewY = Math.round(viewState.viewRect.centerY());

        final Iterable<Page> pages = firstVisible != -1 ? viewState.model.getPages(firstVisible, lastVisible + 1) : viewState.model.getPages(0);

        for (final Page page : pages) {
            final RectF bounds = viewState.getBounds(page);
            final int pageY = Math.round(bounds.centerY());
            final long dist = Math.abs(pageY - viewY);
            if (dist < bestDistance) {
                bestDistance = dist;
                result = page.index.viewIndex;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#getScrollLimits()
     */
    @Override
    public final Rect getScrollLimits() {
        final int width = getWidth();
        final int height = getHeight();

        Page lpo = model.getLastPageObject();
        if (lpo == null) {
            lpo = model.getCurrentPageObject();
        }

        final float zoom = getBase().getZoomModel().getZoom();

        final int bottom = lpo != null ? (int) lpo.getBounds(zoom).bottom - height + 120 : 0;

        int right = (int) (width * zoom) - 120;
        int left = -1 * width + 120;

        return new Rect(left, -120, right, bottom);
    }

    @Override
    public final int getBottomScrollLimit() {
        final int height = getHeight();

        Page lpo = model.getLastPageObject();
        if (lpo == null) {
            lpo = model.getCurrentPageObject();
        }

        final float zoom = getBase().getZoomModel().getZoom();

        final RectF bounds = lpo.getBounds(zoom);
        final int bottom = lpo != null ? (int) (bounds.top - (bounds.bottom - bounds.top)) : 0;

        return bottom;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#invalidatePageSizes(org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason,
     * org.ebookdroid.core.Page)
     */
    @Override
    public synchronized final void invalidatePageSizes(final InvalidateSizeReason reason, final Page changedPage) {
        if (!isInitialized) {
            return;
        }

        if (reason == InvalidateSizeReason.PAGE_ALIGN) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();
        final AppBook bookSettings = SettingsManager.getBookSettings();
        final PageAlign pageAlign = DocumentViewMode.getPageAlign(bookSettings);

        if (changedPage == null) {
            float heightAccum = 0;
            for (final Page page : model.getPages()) {
                final RectF pageBounds = calcPageBounds(pageAlign, page.getAspectRatio(), width, height);
                pageBounds.offset(0, heightAccum);
                page.setBounds(pageBounds);
                heightAccum += pageBounds.height() + 3;
            }
        } else {
            float heightAccum = changedPage.getBounds(1.0f).top;
            for (final Page page : model.getPages(changedPage.index.viewIndex)) {
                final RectF pageBounds = calcPageBounds(pageAlign, page.getAspectRatio(), width, height);
                pageBounds.offset(0, heightAccum);
                page.setBounds(pageBounds);
                heightAccum += pageBounds.height() + 3;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.ui.viewer.IViewController#calcPageBounds(org.ebookdroid.core.Page,
     * int, int)
     */
    @Override
    public RectF calcPageBounds(final PageAlign pageAlign, final float pageAspectRatio, final int width, final int height) {
        return new RectF(0, 0, width, width / pageAspectRatio);
    }
}
