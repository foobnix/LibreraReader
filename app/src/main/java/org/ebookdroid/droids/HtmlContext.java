package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.ext.HtmlExtractor;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.BookCacheLeases;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

public class HtmlContext extends PdfContext {
    File cacheDirectory;
    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override public CodecDocument openDocumentInner(String fileName, String password) {
        try {
            return openDocumentInnerForce(fileName, password, false);
        } catch (RuntimeException firstFailure) {
            LOG.e(firstFailure);
            return openDocumentInnerForce(fileName, password, true);
        }
    }

    public CodecDocument openDocumentInnerForce(String fileName, String password, boolean force) {
        File directory = cacheDirectory = new File(CacheZipUtils.CACHE_BOOK_DIR,
                ConversionCache.key(fileName + sourceRevisionKey(fileName)
                        + EpubProcessingSettings.key() + force
                        + (force ? "" : siblingImageRevision(new File(fileName)))) + "-html-v2");
        try (BookCacheLeases.PublishedFile output = ConversionCache.buildDirectory(
                directory, HtmlExtractor.OUT_FB2_XML,
                staging -> HtmlExtractor.extract(fileName, staging.getPath(), force))) {
            MuPdfDocument document = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                    output.file.getPath(), password);
            document.retainSource(directory);
            return document;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot convert HTML book", failure);
        }
    }

    /** HtmlExtractor copies every sibling image, so they are inputs to the cache key. */
    private static String siblingImageRevision(File source) {
        File parent = source.getParentFile();
        File[] images = parent == null ? null : parent.listFiles(ExtUtils::isImageFile);
        if (images == null) return UUID.randomUUID().toString();
        Arrays.sort(images, (left, right) -> left.getName().compareTo(right.getName()));
        StringBuilder revision = new StringBuilder();
        for (File image : images) {
            if (!image.isFile() || image.lastModified() <= 0)
                return UUID.randomUUID().toString();
            revision.append('|').append(image.getName()).append(':')
                    .append(image.length()).append(':').append(image.lastModified());
        }
        return revision.toString();
    }
}
