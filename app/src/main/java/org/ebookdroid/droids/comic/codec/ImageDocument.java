package org.ebookdroid.droids.comic.codec;

import android.graphics.Bitmap;
import android.graphics.RectF;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A book whose pages are images and nothing else - a comic archive ({@link ComicDocument}), a TIFF
 * of many pages ({@link org.ebookdroid.droids.tiff.codec.TiffDocument}). Its pages are drawn by
 * {@link ComicPage}, which only asks each page's size and its image decoded small enough, so the
 * reader's colours, tiling and zoom are the same for both.
 *
 * There is no text, no outline, no metadata and no annotation layer, which is true of such a book
 * read by MuPDF too.
 */
public abstract class ImageDocument extends AbstractCodecDocument {

    /**
     * There is no native document behind this one, and Librera only uses the handle to tell
     * documents apart, so a counter stands in for it. It must not be 0: that means "recycled".
     */
    private static final AtomicLong HANDLES = new AtomicLong(1);

    protected final String fname;

    /** Page sizes, read off each image's header the first time that page is asked about. */
    private CodecPageInfo[] pageInfo;

    protected ImageDocument(final CodecContext context, final String fname) {
        super(context, HANDLES.incrementAndGet());
        this.fname = fname;
    }

    /** The size of a page, read without decoding it; an empty one when it cannot be read. */
    protected abstract CodecPageInfo readPageInfo(int pageNumber);

    /**
     * Decodes a page made <code>sample</code> times smaller each way, as BitmapFactory's
     * inSampleSize does.
     *
     * @return the image, or <code>null</code> when it cannot be read or held
     */
    protected abstract Bitmap decodePage(int pageNumber, int sample);

    @Override
    public int getPageCount(final int w, final int h, final int fsize) {
        // Images do not reflow: the page count is the number of them, whatever the screen is
        return getPageCount();
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        if (pageNumber < 0 || pageNumber >= getPageCount()) {
            return null;
        }
        return new ComicPage(this, fname, pageNumber);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        // The pages are whatever size each image is; there is no one size for all
        return null;
    }

    @Override
    public synchronized CodecPageInfo getPageInfo(final int pageNumber) {
        final int count = getPageCount();
        if (pageNumber < 0 || pageNumber >= count) {
            return null;
        }
        if (pageInfo == null || pageInfo.length != count) {
            pageInfo = new CodecPageInfo[count];
        }
        CodecPageInfo info = pageInfo[pageNumber];
        if (info == null) {
            info = readPageInfo(pageNumber);
            pageInfo[pageNumber] = info;
        }
        return info;
    }

    // ---------------------------------------------------------------- the rest of the contract

    @Override
    public List<? extends RectF> searchText(final int pageNumber, final String pattern) throws DocSearchNotSupported {
        // There is no text in an image to search, which is what MuPDF answers for one as well
        throw new DocSearchNotSupported();
    }

    @Override
    public String documentToHtml() {
        return "";
    }

    @Override
    public String getMeta(final String option) {
        return "";
    }

    @Override
    public void setMeta(final String key, final String value) {
    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public void deleteAnnotation(final long pageNumber, final int index) {
    }

    @Override
    public void saveAnnotations(final String path) {
    }
}
