package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.Bitmaps;
import org.ebookdroid.ui.viewer.IViewController.InvalidateSizeReason;

import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class EventReset extends AbstractEvent {

    private final Queue<EventReset> eventQueue;

    protected PageTreeLevel level;
    protected InvalidateSizeReason reason;
    protected boolean clearPages;

    EventReset(final Queue<EventReset> eventQueue) {
        this.eventQueue = eventQueue;
    }

    void init(final AbstractViewController ctrl, final InvalidateSizeReason reason, final boolean clearPages) {
        this.viewState = new ViewState(ctrl);
        this.ctrl = ctrl;
        this.level = PageTreeLevel.getLevel(viewState.zoom);
        this.reason = reason;
        this.clearPages = clearPages;
    }

    void release() {
        this.ctrl = null;
        this.viewState = null;
        this.level = null;
        this.reason = null;
        this.bitmapsToRecycle.clear();
        this.nodesToDecode.clear();
        eventQueue.offer(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractEvent#process()
     */
    @Override
    public ViewState process() {
        try {
            if (clearPages) {
                final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
                for (final Page page : ctrl.model.getPages()) {
                    page.nodes.recycleAll(bitmapsToRecycle, true);
                }
                BitmapManager.release(bitmapsToRecycle);
            }
            if (reason != null) {
                ctrl.invalidatePageSizes(reason, null);
                ctrl.invalidateScroll();
                viewState = new ViewState(ctrl);
            }
            return super.process();
        } finally {
            release();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTree)
     */
    @Override
    public boolean process(final PageTree nodes) {
        return process(nodes, level);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTreeNode)
     */
    @Override
    public boolean process(final PageTreeNode node) {

        final RectF pageBounds = viewState.getBounds(node.page);

        if (!viewState.isNodeKeptInMemory(node, pageBounds)) {
            node.recycle(bitmapsToRecycle);
            return false;
        }

        if (!node.holder.hasBitmaps()) {
            node.decodePageTreeNode(nodesToDecode, viewState);
        }

        return true;
    }

}
