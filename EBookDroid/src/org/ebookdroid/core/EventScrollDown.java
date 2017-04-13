package org.ebookdroid.core;

import java.util.Queue;

import org.emdev.utils.LengthUtils;

public class EventScrollDown extends AbstractEventScroll<EventScrollDown> {

    public EventScrollDown(final Queue<EventScrollDown> eventQueue) {
        super(eventQueue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.ebookdroid.core.AbstractEvent#calculatePageVisibility(org.ebookdroid.core.ViewState)
     */
    @Override
    protected ViewState calculatePageVisibility(final ViewState initial) {
        final Page[] pages = ctrl.model.getPages();
        if (LengthUtils.isEmpty(pages)) {
            return initial;
        }

        int firstVisiblePage = initial.pages.firstVisible;
        int lastVisiblePage = initial.pages.lastVisible;

        if (LengthUtils.isNotEmpty(pages) && firstVisiblePage != -1) {
            for (int i = firstVisiblePage; i < pages.length; i++) {
                if (!ctrl.isPageVisible(pages[i], initial)) {
                    continue;
                } else {
                    firstVisiblePage = i;
                    break;
                }
            }
            lastVisiblePage = firstVisiblePage;
            while (lastVisiblePage < pages.length - 1) {
                final int index = lastVisiblePage + 1;
                if (!ctrl.isPageVisible(pages[index], initial)) {
                    break;
                }
                lastVisiblePage = index;
            }
            return new ViewState(initial, firstVisiblePage, lastVisiblePage);
        }

        return super.calculatePageVisibility(initial);
    }

}
