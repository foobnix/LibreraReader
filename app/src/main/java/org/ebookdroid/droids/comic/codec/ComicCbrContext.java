package org.ebookdroid.droids.comic.codec;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CbzCbrExtractor;

import org.ebookdroid.core.codec.CodecDocument;

import java.io.File;

/**
 * A .cbr - a RAR archive of images - read as a comic.
 *
 * It is repacked into a .cbz in the book cache first, exactly as
 * {@link org.ebookdroid.droids.CbrContext} does it for MuPDF, and the images are then read out of
 * that zip. See {@link ComicContext}.
 */
public class ComicCbrContext extends ComicContext {

    private File cacheFile;

    @Override
    public File getCacheFileName(final String fileNameOriginal) {
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".cbz");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, final String password) {
        CbzCbrExtractor.extractCbrToCbz(fileName, cacheFile);
        return ComicDocument.open(this, cacheFile.getPath());
    }
}
