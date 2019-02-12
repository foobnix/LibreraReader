package org.ebookdroid.common.settings.types;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.HScrollController;
import org.ebookdroid.core.VScrollController;
import org.ebookdroid.ui.viewer.IActivityController;
import org.ebookdroid.ui.viewer.IViewController;

public enum DocumentViewMode {

    VERTICALL_SCROLL(PageAlign.WIDTH, VScrollController.class),

    HORIZONTAL_SCROLL(PageAlign.HEIGHT, HScrollController.class);

    private final PageAlign pageAlign;


    private DocumentViewMode(final PageAlign pageAlign, final Class<? extends IViewController> clazz) {
        this.pageAlign = pageAlign;
    }

    public IViewController create(final IActivityController base) {
        //return new HScrollController(base);
        //TODO switch there
        return new VScrollController(base);
    }

    public static PageAlign getPageAlign(final BookSettings bs) {
        return PageAlign.AUTO;
    }

    
}
