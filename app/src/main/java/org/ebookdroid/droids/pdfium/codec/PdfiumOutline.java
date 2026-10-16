package org.ebookdroid.droids.pdfium.codec;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.ebookdroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

import io.legere.pdfiumandroid.PdfDocument;
import io.legere.pdfiumandroid.api.Bookmark;

/**
 * A PDF's table of contents from PDFium, flattened into the list of
 * {@link OutlineLink}s Librera's contents panel reads.
 *
 * Every entry gets a <code>#page</code> link, 1-based, as the panel's
 * {@link com.foobnix.pdf.info.model.OutlineLinkWrapper} expects. An entry whose destination PDFium
 * cannot resolve points at page 1 rather than being dropped, so the chapter is still listed - and
 * never at <code>#0</code>, which is the value the panel takes as "resolve this through MuPDF".
 */
class PdfiumOutline {

    /** A malformed document could nest bookmarks without end. */
    private static final int MAX_LEVEL = 16;

    static List<OutlineLink> get(final PdfDocument document, final long docHandle) {
        final List<OutlineLink> result = new ArrayList<OutlineLink>();
        try {
            append(result, document.getTableOfContents(), 0, docHandle);
        } catch (Throwable e) {
            LOG.e(e);
        }
        LOG.d("PDFIUM! outline", result.size());
        return result;
    }

    private static void append(final List<OutlineLink> result, final List<Bookmark> bookmarks, final int level,
                               final long docHandle) {
        if (bookmarks == null || level > MAX_LEVEL) {
            return;
        }
        for (Bookmark bookmark : bookmarks) {
            if (bookmark == null) {
                continue;
            }
            final String title = bookmark.getTitle();
            if (TxtUtils.isNotEmpty(title)) {
                final int page = (int) Math.max(0, bookmark.getPageIdx()) + 1;
                result.add(new OutlineLink(title, "#" + page, level, docHandle, ""));
            }
            append(result, bookmark.getChildren(), level + 1, docHandle);
        }
    }
}
