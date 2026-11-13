package org.ebookdroid.core.codec;

import android.graphics.RectF;

import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.List;
import java.util.concurrent.Callable;

public class PageLink {

    public String url;

    public RectF sourceRect;

    public int targetPage = -1;
    public RectF targetRect;
    public int number = 0;

    public PageLink() {
    }

    /**
     * Internal links whose destination the document does not have - a GoTo to a name it never
     * defines, as books laid out in InDesign carry for their whole contents - are given the page
     * the line they cover ends with: "Chapter 12. Multitasking ..... 601" goes to page 601.
     * The printed number is taken for the page's own, as such books number them.
     *
     * @param words     the page's words, read only if there is such a link
     * @param pageCount pages in the document, asked only if there is such a link
     */
    public static void resolveByPrintedPage(final List<PageLink> links, final Callable<TextWord[][]> words,
                                            final Callable<Integer> pageCount) {
        TextWord[][] lines = null;
        int count = -1;
        for (PageLink link : links) {
            if (link.url != null || link.targetPage >= 0 || link.sourceRect == null) {
                continue;
            }
            try {
                if (lines == null) {
                    lines = words.call();
                    count = pageCount.call();
                }
            } catch (Exception e) {
                return;
            }
            if (lines == null) {
                return;
            }
            TextWord last = null;
            for (TextWord[] line : lines) {
                for (TextWord word : line) {
                    if (word != null && word.w != null && RectF.intersects(link.sourceRect, word)
                            && word.centerY() >= link.sourceRect.top && word.centerY() <= link.sourceRect.bottom
                            && (last == null || word.right > last.right)) {
                        last = word;
                    }
                }
            }
            final String number = last == null ? "" : last.w.replaceAll("[^0-9]", "");
            if (number.isEmpty() || number.length() > 6) {
                continue;
            }
            final int page = Integer.parseInt(number);
            if (page >= 1 && page <= count) {
                link.targetPage = page - 1;
            }
        }
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
