package com.foobnix.pdf.info.model;

import com.foobnix.ext.Fb2Extractor;
import com.foobnix.pdf.info.PageUrl;

import android.graphics.RectF;

public class OutlineLinkWrapper implements CharSequence {

    private final String title;
    public final int level;

    public String targetUrl;
    public int targetPage = -1;
    public RectF targetRect;
    public long docHandle;

    public String linkUri;

    public OutlineLinkWrapper(final String title, final String link, final int level, long docHandle, String linkUri) {
        this.title = title;
        this.level = level;
        this.docHandle = docHandle;

        this.linkUri = linkUri;

        if (link != null) {
            if (link.startsWith("#")) {
                try {
                    targetPage = Integer.parseInt(link.substring(1).replace(" ", ""));
                    targetPage = PageUrl.realToFake(targetPage);

                } catch (final Exception e) {
                    e.printStackTrace();
                    targetPage = -1;
                }
            } else if (link.startsWith("http:")) {
                targetUrl = link;
            }
        }
    }

    public static int getPageNumber(String link) {
        if (link != null) {
            if (link.startsWith("#")) {
                try {
                    int page = Integer.parseInt(link.substring(1).replace(" ", ""));
                    return PageUrl.realToFake(page);

                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;

    }

    public String getTitleRaw() {
        return title;

    }

    public String getTitleAsString() {
        String t = title;
        if (title.contains("$")) {
            t = t.substring(0, title.indexOf("$"));
        }
        return t.replace(Fb2Extractor.FOOTER_AFTRER_BOODY, "");
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.CharSequence#charAt(int)
     */
    @Override
    public char charAt(final int index) {
        return title.charAt(index);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.CharSequence#length()
     */
    @Override
    public int length() {
        return title.length();
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    @Override
    public CharSequence subSequence(final int start, final int end) {
        return title.subSequence(start, end);
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return title;
    }
}
