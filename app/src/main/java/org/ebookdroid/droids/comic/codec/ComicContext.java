package org.ebookdroid.droids.comic.codec;

import android.graphics.Bitmap;

import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

/**
 * Comic archives - .cbz and .cbr - read as what they are: a list of images in an archive.
 *
 * Every build reads them here, MuPDF or not: the pages are already bitmaps, so the archive - zip
 * or RAR - is read in place with libarchive ({@link com.foobnix.ext.NativeArchive}), nothing
 * extracted or repacked, and each image decoded straight to a {@link Bitmap} by Android's own
 * decoder, which handles every format a comic uses - JPEG, PNG, WebP, GIF - and is
 * hardware-accelerated.
 *
 * It answers the same {@link CodecDocument} / {@link org.ebookdroid.core.codec.CodecPage} contract
 * as the other engines, so the reader, the thumbnails and the library's covers work unchanged.
 * There is no text layer and no annotation layer, which is true of a comic read by MuPDF too.
 *
 * {@link org.ebookdroid.BookType#CBZ} and {@link org.ebookdroid.BookType#CBR} name this context.
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
        // An archive is read one entry at a time, so the pages are read under the document's
        // lock, the same way the other two engines serialize on theirs.
        return false;
    }
}
