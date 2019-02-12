package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.BitmapManager;
import org.ebookdroid.common.bitmaps.Bitmaps;

import android.graphics.Rect;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class EventChildLoaded extends AbstractEvent {

    private final Queue<EventChildLoaded> eventQueue;

    public Page page;
    public PageTree nodes;
    public PageTreeNode child;

    public Rect bitmapBounds;

    public EventChildLoaded(final Queue<EventChildLoaded> eventQueue) {
        this.eventQueue = eventQueue;
    }

    final void init(final AbstractViewController ctrl, final PageTreeNode child, final Rect bitmapBounds) {
        this.viewState = new ViewState(ctrl);
        this.ctrl = ctrl;
        this.page = child.page;
        this.nodes = page.nodes;
        this.child = child;
        this.bitmapBounds = bitmapBounds;
    }

    final void release() {
        this.ctrl = null;
        this.viewState = null;
        this.child = null;
        this.nodes = null;
        this.page = null;
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
    public final ViewState process() {
        try {
            if (ctrl == null || viewState.book == null) {
                return null;
            }

            final RectF bounds = viewState.getBounds(page);
            final PageTreeNode parent = child.parent;
            if (parent != null) {
                recycleParent(parent, bounds);
            }
            recycleChildren();

            ctrl.pageUpdated(viewState, page);
            ctrl.redrawView(viewState);

            return viewState;
        } finally {
            release();
        }
    }

    protected void recycleParent(final PageTreeNode parent, final RectF bounds) {
        final boolean hiddenByChildren = nodes.isHiddenByChildren(parent, viewState, bounds);

        // if (LCTX.isDebugEnabled()) {
        // LCTX.d("Node " + parent.fullId + " is: " + (hiddenByChildren ? "" : "not") + " hidden by children");
        // }

        if (!viewState.isNodeVisible(parent, bounds) || hiddenByChildren) {
            final List<Bitmaps> bitmapsToRecycle = new ArrayList<Bitmaps>();
            final boolean res = nodes.recycleParents(child, bitmapsToRecycle);
            BitmapManager.release(bitmapsToRecycle);

            if (res) {
            }
        }
    }

    protected void recycleChildren() {
        nodes.recycleChildren(child, bitmapsToRecycle);
        BitmapManager.release(bitmapsToRecycle);
    }

    @Override
    public boolean process(final PageTree nodes) {
        return false;
    }

    @Override
    public boolean process(final PageTreeNode node) {
        return false;
    }
}
