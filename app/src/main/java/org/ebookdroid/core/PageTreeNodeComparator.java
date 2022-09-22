package org.ebookdroid.core;

import java.util.Comparator;

public class PageTreeNodeComparator implements Comparator<PageTreeNode> {
    private final ViewState viewState;

    public PageTreeNodeComparator(final ViewState viewState) {
        this.viewState = viewState;
    }

    @Override
    public int compare(final PageTreeNode node1, final PageTreeNode node2) {
        return Boolean.compare(viewState.isNodeVisible(node2, viewState.getBounds(node2.page)),
                viewState.isNodeVisible(node1, viewState.getBounds(node1.page)));
    }
}
