package org.ebookdroid.core.codec;

import android.graphics.RectF;

public class PageLink {

    public String url;

    public RectF sourceRect;

    public int targetPage = -1;
    public RectF targetRect;
    public int number = 0;

    public PageLink() {
    }

    public PageLink(final String l, final int[] source) {
        url = l;
        sourceRect = new RectF(source[0], source[1], source[2], source[3]);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder(this.getClass().getSimpleName());
        buf.append("[");
        buf.append("source").append("=").append(sourceRect);
        if (url != null) {
            buf.append(", ");
            buf.append("url").append("=").append(url);
        } 
        if (targetPage != -1) {
            buf.append(", ");
            buf.append("target").append("=").append(targetPage);
            if (targetRect != null) {
                buf.append(" ").append(targetRect);
            }
        }
        buf.append("]");
        return buf.toString();
    }
}
