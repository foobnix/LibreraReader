package org.ebookdroid.droids.pdfium.codec;

import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.ebookdroid.core.codec.PageLink;

import java.util.ArrayList;
import java.util.List;

import io.legere.pdfiumandroid.PdfPage;
import io.legere.pdfiumandroid.PdfPageLink;
import io.legere.pdfiumandroid.PdfTextPage;
import io.legere.pdfiumandroid.api.Link;

/**
 * A PDFium page's links as Librera's {@link PageLink}: the rectangles scaled to the page's 0..1,
 * an internal link carrying its 0-based target page and an external one its URL.
 */
class PdfiumLinks {

    /** The longest URL read out of a web link found in the page's text. */
    private static final int URL_LIMIT = 1024;

    /**
     * @param page     an open PDFium page
     * @param textPage the page's text layer, for the URLs written in the text itself; may be null
     */
    static List<PageLink> get(final PdfPage page, final PdfTextPage textPage, final PdfiumTransform transform) {
        final List<PageLink> links = new ArrayList<PageLink>();
        annotationLinks(page, transform, links);
        webLinks(textPage, transform, links);
        return links;
    }

    /** The links the page carries as annotations: /Link, both GoTo and URI. */
    private static void annotationLinks(final PdfPage page, final PdfiumTransform transform,
                                        final List<PageLink> links) {
        List<Link> found;
        try {
            found = page.getPageLinks();
        } catch (Throwable e) {
            LOG.e(e);
            return;
        }
        if (found == null) {
            return;
        }
        for (Link link : found) {
            if (link == null || link.getBounds() == null) {
                continue;
            }
            final PageLink result = new PageLink();
            result.sourceRect = normalized(link.getBounds(), transform);
            if (TxtUtils.isNotEmpty(link.getUri())) {
                result.url = link.getUri();
            } else if (link.getDestPageIdx() != null) {
                result.targetPage = link.getDestPageIdx().intValue();
            } else {
                continue;
            }
            links.add(result);
        }
    }

    /** The URLs written in the page's text, which are not annotations. */
    private static void webLinks(final PdfTextPage textPage, final PdfiumTransform transform,
                                 final List<PageLink> links) {
        if (textPage == null) {
            return;
        }
        PdfPageLink pageLink = null;
        try {
            pageLink = textPage.loadWebLink();
            if (pageLink == null) {
                return;
            }
            final int count = pageLink.countWebLinks();
            for (int i = 0; i < count; i++) {
                final String url = url(pageLink, i);
                if (TxtUtils.isEmpty(url)) {
                    continue;
                }
                final int rects = pageLink.countRects(i);
                for (int r = 0; r < rects; r++) {
                    final RectF bounds = pageLink.getRect(i, r);
                    if (bounds == null) {
                        continue;
                    }
                    final PageLink result = new PageLink();
                    result.url = url;
                    result.sourceRect = normalized(bounds, transform);
                    links.add(result);
                }
            }
        } catch (Throwable e) {
            LOG.e(e);
        } finally {
            if (pageLink != null) {
                try {
                    pageLink.close();
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        }
    }

    private static String url(final PdfPageLink pageLink, final int index) {
        final String url = pageLink.getURL(index, URL_LIMIT);
        if (url == null) {
            return null;
        }
        // The buffer comes back padded with NULs up to the length that was asked for
        final int end = url.indexOf(0);
        return end >= 0 ? url.substring(0, end) : url;
    }

    private static RectF normalized(final RectF pdf, final PdfiumTransform transform) {
        final RectF rect = transform.toPageSpace(pdf);
        transform.normalize(rect);
        return rect;
    }
}
