package org.ebookdroid.core;

public interface IEvent {

    ViewState process();

    boolean process(Page page);

    boolean process(PageTree nodes);

    boolean process(PageTree nodes, PageTreeLevel level);

    boolean process(PageTreeNode node);
}
