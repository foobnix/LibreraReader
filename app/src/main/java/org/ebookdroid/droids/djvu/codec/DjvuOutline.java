package org.ebookdroid.droids.djvu.codec;

import java.util.ArrayList;
import java.util.List;

import org.ebookdroid.core.codec.OutlineLink;

public class DjvuOutline {

    private long docHandle;

    public List<OutlineLink> getOutline(final long dochandle) {
        final List<OutlineLink> ls = new ArrayList<OutlineLink>();
        docHandle = dochandle;
        final long expr = open(docHandle);
        ttOutline(ls, expr, 0);
        ls.add(new OutlineLink("", "", -1, dochandle, ""));
        return ls;
    }

    private void ttOutline(final List<OutlineLink> ls, long expr, int level) {
        while (expConsp(expr)) {
            final String title = getTitle(expr);
            final String link = getLink(expr, docHandle);
            if (title != null) {
                ls.add(new OutlineLink(title, link, level, docHandle, ""));
            }
            final long child = getChild(expr);
            ttOutline(ls, child, level + 1);

            expr = getNext(expr);
        }

    }

    private static native long open(long dochandle);

    private static native boolean expConsp(long expr);

    private static native String getTitle(long expr);

    private static native String getLink(long expr, long dochandle);

    private static native long getNext(long expr);

    private static native long getChild(long expr);

}
