package org.ebookdroid.droids.tiff.codec;

import android.graphics.Bitmap;

import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

/**
 * .tif and .tiff books, in every build, MuPDF or not: every image of the file a page, read with
 * libtiff ({@link com.foobnix.ext.NativeTiff.Book}) and drawn the way a comic's pages are.
 *
 * {@link org.ebookdroid.BookType#TIFF} names this context.
 */
public class TiffContext extends AbstractCodecContext {

    @Override
    public CodecDocument openDocumentInner(final String fileName, final String password) {
        return TiffDocument.open(this, fileName);
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return AppsConfig.CURRENT_BITMAP_ARGB;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        // libtiff reads one image at a time, so the pages are read under the book's lock
        return false;
    }
}
