package org.ebookdroid.core;

import android.graphics.RectF;

import java.util.Queue;

public class EventZoomIn extends AbstractEventZoom<EventZoomIn> {

    EventZoomIn(final Queue<EventZoomIn> eventQueue) {
        super(eventQueue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.IEvent#process(org.ebookdroid.core.ViewState, org.ebookdroid.core.PageTreeNode)
     */
    @Override
    public final boolean process(final PageTreeNode node) {

        final RectF pageBounds = viewState.getBounds(node.page);

        if (!viewState.isNodeKeptInMemory(node, pageBounds)) {
            node.recycle(bitmapsToRecycle);
            return false;
        }

        if (isReDecodingRequired(viewState, node, committed)) {
            node.stopDecodingThisNode("Zoom changed");
            node.decodePageTreeNode(nodesToDecode, viewState);
        } else if (!node.holder.hasBitmaps()) {
            node.decodePageTreeNode(nodesToDecode, viewState);
        }
        return true;
    }

    protected final boolean isReDecodingRequired(final ViewState viewState, final PageTreeNode node, final boolean committed) {
        if (committed) {
            return viewState.zoom != node.bitmapZoom;
        }
        return (viewState.app.reloadDuringZoom && viewState.zoom > 1.2 * node.bitmapZoom);
    }
}
