package org.ebookdroid.droids.tiff.codec;

import android.graphics.Bitmap;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.NativeTiff;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.droids.comic.codec.ImageDocument;

/** A .tif or .tiff book: its images are its pages. See {@link TiffContext}. */
public class TiffDocument extends ImageDocument {

    private final NativeTiff.Book book;

    private TiffDocument(final TiffContext context, final String fname, final NativeTiff.Book book) {
        super(context, fname);
        this.book = book;
    }

    /**
     * @return the document, or <code>null</code> when the file is not a TIFF libtiff reads - which
     *         is what tells the reader to report it as broken
     */
    static TiffDocument open(final TiffContext context, final String fname) {
        final NativeTiff.Book book = NativeTiff.Book.open(fname);
        if (book == null) {
            LOG.d("TIFF! cannot open", fname);
            return null;
        }
        LOG.d("TIFF! >>> open [document]", fname, book.getPageCount(), "pages");
        return new TiffDocument(context, fname, book);
    }

    @Override
    public int getPageCount() {
        return book.getPageCount();
    }

    @Override
    protected CodecPageInfo readPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        final int[] size = isRecycled() ? null : book.size(pageNumber);
        if (size != null) {
            info.width = size[0];
            info.height = size[1];
        }
        return info;
    }

    @Override
    protected Bitmap decodePage(final int pageNumber, final int sample) {
        return isRecycled() ? null : book.decode(pageNumber, sample);
    }

    @Override
    public BookType getBookType() {
        return BookType.TIFF;
    }

    @Override
    protected void freeDocument() {
        book.close();
    }
}
