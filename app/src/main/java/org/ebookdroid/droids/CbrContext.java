package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.pdf.info.BookCacheLeases;
import com.github.junrar.Junrar;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import net.lingala.zip4j.ZipFile;



public class CbrContext extends PdfContext {

    File cacheFile;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, ConversionCache.key(fileNameOriginal) + "-v2.cbz");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (cacheFile == null) cacheFile = getCacheFileName(fileName);
        try (BookCacheLeases.PublishedFile output = ConversionCache.buildFile(cacheFile, temporary -> {
            if (CbzCbrExtractor.isZip(fileName)) {
                CacheZipUtils.copyFile(new File(fileName), temporary);
            } else {
                File extracted = new File(CacheZipUtils.CACHE_BOOK_DIR,
                        "cbr-" + UUID.randomUUID() + ".part");
                try (AutoCloseable lease = BookCacheLeases.acquire(extracted)) {
                    if (!extracted.mkdir()) throw new IOException("Cannot create CBR extraction directory");
                    Junrar.extract(fileName, extracted.getPath());
                    CacheZipUtils.zipFolder(extracted.getPath(), temporary.getPath());
                } finally {
                    BookCacheLeases.evictTree(extracted);
                }
            }
            try (java.util.zip.ZipFile verified = new java.util.zip.ZipFile(temporary)) {
                if (!verified.entries().hasMoreElements())
                    throw new IOException("CBR conversion produced an empty CBZ");
            }
        })) {
            return new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                    output.file.getPath(), password);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot convert CBR book", failure);
        }
    }

}
