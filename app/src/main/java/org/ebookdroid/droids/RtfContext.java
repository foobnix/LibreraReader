package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.ext.RtfExtract;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;

public class RtfContext extends PdfContext {

    File cacheFile;

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + EpubProcessingSettings.key();
        cacheFile = new File(new File(CacheZipUtils.CACHE_BOOK_DIR,
                ConversionCache.key(fileNameOriginal) + "-rtf-v2"), "book.html");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (cacheFile == null) {
            getCacheFileName(fileName + sourceRevisionKey(fileName));
        }
        try (BookCacheLeases.PublishedFile output = ConversionCache.buildDirectory(
                cacheFile.getParentFile(), cacheFile.getName(), directory ->
                        RtfExtract.extract(fileName, directory.getPath(), cacheFile.getName()))) {
            MuPdfDocument document = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                    output.file.getPath(), password);
            document.retainCacheSource(cacheFile.getParentFile());
            return document;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot convert RTF book", failure);
        }
    }
}
