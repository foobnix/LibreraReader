package org.ebookdroid.core;

import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.ui.viewer.IView;

import android.graphics.PointF;
import android.graphics.RectF;

public class EventGotoPage implements IEvent {

    protected final boolean centerPage;

    protected AbstractViewController ctrl;
    protected ViewState viewState;
    protected DocumentModel model;
    protected int toPageIndex;
    protected final float offsetX;
    protected final float offsetY;
    private boolean animte = false;

    public EventGotoPage(final AbstractViewController ctrl, final int toPage, boolean animate) {
        this.viewState = new ViewState(ctrl);
        this.ctrl = ctrl;
        this.model = viewState.model;
        this.centerPage = true;
        this.toPageIndex = toPage;
        this.offsetX = 0;
        this.offsetY = 0;
        this.animte = animate;
    }

    public EventGotoPage(final AbstractViewController ctrl, final int viewIndex, final float offsetX,
            final float offsetY) {
        this.viewState = new ViewState(ctrl);
        this.ctrl = ctrl;
        this.model = viewState.model;
        this.centerPage = false;
        this.toPageIndex = viewIndex;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public ViewState process() {
        if (model == null) {
            return null;
        }

        final int pageCount = model.getPageCount();
        if (toPageIndex < 0 && toPageIndex >= pageCount) {
            return viewState;
        }

        final Page page = model.getPageObject(toPageIndex);
        if (page == null) {
            return viewState;
        }



        final IView view = ctrl.getView();

        final int scrollX = view.getScrollX();
        final int scrollY = view.getScrollY();

        final PointF p = calculateScroll(page, scrollX, scrollY);
        final int left = Math.round(p.x);
        final int top = Math.round(p.y);

        if (isScrollRequired(left, top, scrollX, scrollY)) {
            if(animte){
                view.startPageScroll(0, top - scrollY);
            }else{
                view.scrollTo(left, top);
            }
            return new ViewState(ctrl);
        }

        return EventPool.newEventScrollTo(ctrl, toPageIndex).process();
    }

    protected PointF calculateScroll(final Page page, final int scrollX, final int scrollY) {
        final RectF viewRect = ctrl.getView().getViewRect();
        final RectF bounds = page.getBounds(viewState.zoom);
        final float width = bounds.width();
        final float height = bounds.height();

        if (centerPage) {
            switch (ctrl.mode) {
                case HORIZONTAL_SCROLL:
                    return new PointF(bounds.left - (viewRect.width() - width) / 2, scrollY);
                case VERTICALL_SCROLL:
                    return new PointF(scrollX, bounds.top - (viewRect.height() - height) / 2);
            }
        }
        return new PointF(bounds.left + offsetX * width, bounds.top + offsetY * height);

    }

    protected boolean isScrollRequired(final int left, final int top, final int scrollX, final int scrollY) {
        switch (ctrl.mode) {
            case HORIZONTAL_SCROLL:
                return left != scrollX;
            case VERTICALL_SCROLL:
                return top != scrollY;
        }
        return true;
    }

    @Override
    public boolean process(final Page page) {
        return false;
    }

    @Override
    public boolean process(final PageTree nodes) {
        return false;
    }

    @Override
    public boolean process(final PageTree nodes, final PageTreeLevel level) {
        return false;
    }

    @Override
    public boolean process(final PageTreeNode node) {
        return false;
    }
}
