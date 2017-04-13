package org.ebookdroid.core;


import android.graphics.RectF;

import java.util.Comparator;

import org.emdev.utils.CompareUtils;

public class PageTreeNodeComparator implements Comparator<PageTreeNode> {

    final ViewState viewState;

    public PageTreeNodeComparator(final ViewState viewState) {
        this.viewState = viewState;
    }

    @Override
    public int compare(final PageTreeNode node1, final PageTreeNode node2) {
        final int cp = viewState.pages.currentIndex;
        final int viewIndex1 = node1.page.index.viewIndex;
        final int viewIndex2 = node2.page.index.viewIndex;

        final boolean v1 = viewState.isNodeVisible(node1, viewState.getBounds(node1.page));
        final boolean v2 = viewState.isNodeVisible(node2, viewState.getBounds(node2.page));

        final RectF s1 = node1.pageSliceBounds;
        final RectF s2 = node2.pageSliceBounds;

        int res = 0;

        if (viewIndex1 == cp && viewIndex2 == cp) {
            res = CompareUtils.compare(s1.top, s2.top);
            if (res == 0) {
                res = CompareUtils.compare(s1.left, s2.left);
            }
        } else if (v1 && !v2) {
            res = -1;
        } else if (!v1 && v2) {
            res = 1;
        } else {
            final float d1 = viewIndex1 + s1.centerY() - (cp + 0.5f);
            final float d2 = viewIndex2 + s2.centerY() - (cp + 0.5f);
            final int dist1 = Math.abs((int) (d1 * node1.level.zoom));
            final int dist2 = Math.abs((int) (d2 * node2.level.zoom));
            res = CompareUtils.compare(dist1, dist2);
            if (res == 0) {
                res = -CompareUtils.compare(viewIndex1, viewIndex2);
            }
        }

        return res;
    }
}
