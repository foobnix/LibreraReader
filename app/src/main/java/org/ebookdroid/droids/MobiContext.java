package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubExtractor;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.MobiExtract;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class MobiContext extends PdfContext {

    String fileNameEpub = null;

    File cacheFile;

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override
    public File getCacheFileName(String fileName) {
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, ConversionCache.key(fileName + EpubProcessingSettings.key()) + "-mobi.epub");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        LOG.d("Context", "MobiContext", fileName);
        if (cacheFile == null) cacheFile = getCacheFileName(fileName);
        ConversionCache.prepare(cacheFile);
        AutoCloseable outputLease = null;
        synchronized (BookCacheLeases.class) {
            if (cacheFile.isFile()) outputLease = BookCacheLeases.acquire(cacheFile);
        }
        if (outputLease == null) {
            String stem = "mobi-" + UUID.randomUUID();
            File converted = new File(CacheZipUtils.CACHE_BOOK_DIR, stem + ".epub");
            File processed = null;
            AutoCloseable processingLease = null;
            try (AutoCloseable conversionLease = BookCacheLeases.acquire(converted)) {
                MobiExtract.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), stem);
                File publishable = converted;
                if (EpubProcessingSettings.isAutoHypens()) {
                    synchronized (BookCacheLeases.class) {
                        processed = BookCacheLeases.temporary(CacheZipUtils.CACHE_BOOK_DIR, "mobi-process-");
                        processingLease = BookCacheLeases.acquire(processed);
                    }
                    EpubExtractor.proccessHypensApache(converted.getPath(), processed.getPath(), null);
                    if (TempHolder.get().loadingCancelled.get())
                        throw new IOException("MOBI processing cancelled");
                    publishable = processed;
                }
                synchronized (BookCacheLeases.class) {
                    if (!cacheFile.isFile()) BookCacheLeases.publish(publishable, cacheFile);
                    outputLease = BookCacheLeases.acquire(cacheFile);
                }
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot convert MOBI book", failure);
            } finally {
                if (processingLease != null) {
                    try { processingLease.close(); } catch (Exception failure) { LOG.e(failure); }
                }
                BookCacheLeases.evict(converted);
                if (processed != null) BookCacheLeases.evict(processed);
            }
        }

        fileNameEpub = cacheFile.getPath();
        final MuPdfDocument muPdfDocument;
        try {
            muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileNameEpub, password);
        } finally {
            try { outputLease.close(); } catch (Exception failure) { LOG.e(failure); }
        }

        final File jsonFile = new File(cacheFile + ".json");
        if (JsonHelper.isValidMapFile(jsonFile)) {
            muPdfDocument.setFootNotes(JsonHelper.fileToMap(jsonFile));
            LOG.d("Load notes from file", jsonFile);
        } else {

            final String metadataPath = fileNameEpub;
            BookCacheLeases.startLeasedThread("@T mobi set footernotes", Thread.NORM_PRIORITY, () -> {
                    Map<String, String> notes = null;
                    try {
                        notes = EpubExtractor.get().getFooterNotes(metadataPath);
                        LOG.d("new file name", metadataPath);
                        muPdfDocument.setFootNotes(notes);

                        // a cancelled extraction is empty or partial, it must not stay in the cache
                        if (!TempHolder.get().loadingCancelled.get()) {
                            JsonHelper.mapToCacheFile(jsonFile, notes);
                            LOG.d("save notes to file", jsonFile);
                        }

                        removeTempFilesIfCancel();

                    } catch (OutOfMemoryError e) {
                        System.gc();
                        notes = null;
                        LOG.e(e);
                    } catch (Exception e) {
                        notes = null;
                        LOG.e(e);
                    }
            }, new File(metadataPath));
        }


        return muPdfDocument;
    }

}
