package org.ebookdroid.core;

import org.ebookdroid.common.bitmaps.Bitmaps;

import android.graphics.RectF;

import java.util.List;

public class PageTree {

    // private static final LogContext LCTX = Page.LCTX;

    static RectF[] splitMasks = {
            // Left Top
            new RectF(0, 0, 0.5f, 0.5f),
            // Right top
            new RectF(0.5f, 0, 1.0f, 0.5f),
            // Left Bottom
            new RectF(0, 0.5f, 0.5f, 1.0f),
            // Right Bottom
            new RectF(0.5f, 0.5f, 1.0f, 1.0f), };

    final Page owner;

    final PageTreeNode root;

    private PageTreeNode[] treeNodes;

    private volatile int maxNodeId;

    public PageTree(final Page owner) {
        this.owner = owner;
        this.root = new PageTreeNode(owner);
        this.maxNodeId = 1;
    }

    private synchronized PageTreeNode[] getNodes() {
        if (this.treeNodes == null) {
            this.treeNodes = new PageTreeNode[PageTreeLevel.NODES];
            this.treeNodes[0] = root;
        }
        return this.treeNodes;
    }

    public boolean process(final IEvent event, final PageTreeLevel level, final boolean createNodes) {
        boolean res = false;
        if (createNodes || level.start < maxNodeId) {
            final PageTreeNode[] nodes = getNodes();
            for (int nodeIndex = level.start; nodeIndex < level.end; nodeIndex++) {
                if (nodes[nodeIndex] == null) {
                    createChildren(getParent(nodeIndex, true));
                }
                res |= event.process(nodes[nodeIndex]);
            }
        }
        return res;
    }

    public boolean paintChildren(final EventDraw event, final PageTreeNode node, final RectF nodeRect) {
        boolean res = true;
        int childId = PageTree.getFirstChildId(node.id);
        if (childId < maxNodeId) {
            final PageTreeNode[] nodes = getNodes();
            for (final int end = Math.min(nodes.length, childId + PageTree.splitMasks.length); childId < end; childId++) {
                final PageTreeNode child = nodes[childId];
                if (child != null) {
                    res &= event.paintChild(node, child, nodeRect);
                }
            }
        }
        return res;
    }

    public boolean createChildren(final PageTreeNode parent) {
        final PageTreeNode[] nodes = getNodes();
        int childId = getFirstChildId(parent.id);
        for (int i = 0; i < splitMasks.length; i++, childId++) {
            if (nodes[childId] == null) {
                nodes[childId] = new PageTreeNode(owner, parent, childId, splitMasks[i]);
            }
        }
        maxNodeId = Math.max(maxNodeId, childId);
        return true;
    }

    public PageTreeNode getParent(final int nodeIndex, final boolean create) {
        if (nodeIndex == 0) {
            return null;
        }
        if (nodeIndex >= maxNodeId && !create) {
            return null;
        }
        final PageTreeNode[] nodes = getNodes();
        final int parentIndex = (nodeIndex - 1) / 4;
        if (nodes[parentIndex] == null && create) {
            createChildren(getParent(parentIndex, true));
        }
        return nodes[parentIndex];
    }

    public boolean recycleAll(final List<Bitmaps> bitmapsToRecycle, final boolean includeRoot) {
        boolean res = false;
        if (includeRoot) {
            res |= root.recycle(bitmapsToRecycle);
        }
        if (maxNodeId > 1) {
            final PageTreeNode[] nodes = getNodes();
            for (int index = 1; index < maxNodeId; index++) {
                if (nodes[index] != null) {
                    res |= nodes[index].recycle(bitmapsToRecycle);
                    nodes[index] = null;
                }
            }
        }
        maxNodeId = 1;
        return res;
    }

    public boolean recycleParents(final PageTreeNode child, final List<Bitmaps> bitmapsToRecycle) {
        if (child.id == 0) {
            return false;
        }
        boolean res = false;
        int childId = child.id;
        for (PageTreeNode p = getParent(childId, false); p != null; p = getParent(childId, false)) {
            res |= p.recycle(bitmapsToRecycle);
            childId = p.id;
            if (child.id == 0) {
                break;
            }
        }
        return res;
    }

    public boolean recycleChildren(final PageTreeNode node, final List<Bitmaps> bitmapsToRecycle) {
        boolean res = false;
        int childId = getFirstChildId(node.id);
        if (childId >= maxNodeId) {
            return res;
        }
        final PageTreeNode[] nodes = getNodes();
        for (final int end = Math.min(nodes.length, childId + splitMasks.length); childId < end; childId++) {
            if (nodes[childId] != null) {
                res |= nodes[childId].recycle(bitmapsToRecycle);
                nodes[childId] = null;
            }
        }
        if (childId >= maxNodeId) {
            if (childId >= nodes.length) {
                maxNodeId = nodes.length - 1;
            }
            while (maxNodeId > 0 && nodes[maxNodeId] == null) {
                maxNodeId--;
            }
            maxNodeId++;
        }
        return res;
    }

    public void recycleNodes(final PageTreeLevel level, final List<Bitmaps> bitmapsToRecycle) {
        if (level.start >= maxNodeId) {
            return;
        }
        final PageTreeNode[] nodes = getNodes();
        for (int i = level.start; i < maxNodeId; i++) {
            if (nodes[i] != null) {
                nodes[i].recycle(bitmapsToRecycle);
                nodes[i] = null;
            }
        }
        maxNodeId = level.start;
        while (maxNodeId > 0 && nodes[maxNodeId] == null) {
            maxNodeId--;
        }
        maxNodeId++;
    }

    public boolean isHiddenByChildren(final PageTreeNode parent, final ViewState viewState, final RectF pageBounds) {
        int childId = getFirstChildId(parent.id);
        if (childId >= maxNodeId) {
            return false;
        }
        final PageTreeNode[] nodes = getNodes();
        for (final int end = Math.min(nodes.length, childId + splitMasks.length); childId < end; childId++) {
            final PageTreeNode child = nodes[childId];
            if (child == null) {
                return false;
            }
            if (viewState.isNodeKeptInMemory(child, pageBounds) && !child.holder.hasBitmaps()) {
                return false;
            }
        }
        return true;
    }

    static int getFirstChildId(final long parentId) {
        return (int) (parentId * splitMasks.length + 1);
    }
}
