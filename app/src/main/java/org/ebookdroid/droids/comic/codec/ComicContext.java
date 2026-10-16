package org.ebookdroid.droids.comic.codec;

import android.graphics.Bitmap;

import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

/**
 * Comic archives - .cbz and .cbr - read as what they are: a list of images in a zip.
 *
 * MuPDF opens a .cbz as a document whose every page is one image, and that is how the flavours
 * built with it still read them ({@link org.ebookdroid.droids.CbrContext} and
 * {@link org.ebookdroid.BookType#CBZ}). The flavours built without MuPDF have no such document to
 * open, and they do not need one: the pages are already bitmaps, so the archive is read with
 * zip4j and each image decoded straight to a {@link Bitmap} by Android's own decoder, which
 * handles every format a comic uses - JPEG, PNG, WebP, GIF - and is hardware-accelerated.
 *
 * It answers the same {@link CodecDocument} / {@link org.ebookdroid.core.codec.CodecPage} contract
 * as the other engines, so the reader, the thumbnails and the library's covers work unchanged.
 * There is no text layer and no annotation layer, which is true of a comic read by MuPDF too.
 *
 * {@link org.ebookdroid.BookType} picks this context over MuPDF's when
 * {@link com.foobnix.pdf.info.Engines#IS_MUPDF} is false.
 */
public class ComicContext extends AbstractCodecContext {

    @Override
    public CodecDocument openDocumentInner(final String fileName, final String password) {
        return ComicDocument.open(this, fileName);
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return AppsConfig.CURRENT_BITMAP_ARGB;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        // One zip4j archive hands out one entry stream at a time, so the pages are read under the
        // document's lock, the same way the other two engines serialize on theirs.
        return false;
    }
}
