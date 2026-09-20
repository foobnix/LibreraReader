package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
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
import net.lingala.zip4j.ZipFile;

public class Fb2Context extends PdfContext {

    File cacheFile;

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + EpubProcessingSettings.key();
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, ConversionCache.key(fileNameOriginal) + "-v2.epub");
        return cacheFile;
    }

    MuPdfDocument muPdfDocument;

    @Override
    public CodecDocument openDocumentInner(final String fileName, String password) {
        if(cacheFile==null){
            cacheFile = getCacheFileName(fileName);
        }
        Map<String, String> notes = null;
        if (EpubProcessingSettings.isShowFooterNotesInText()) {
            notes = getNotes(fileName);

        }

        File publishedOutput = cacheFile;
        try {
            try (BookCacheLeases.PublishedFile output = convertFb2(fileName, cacheFile, false, notes)) {
                muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                        output.file.getPath(), password);
                muPdfDocument.getPageCount();
            }
        } catch (Exception firstFailure) {
            LOG.e(firstFailure);
            if (muPdfDocument != null) {
                muPdfDocument.recycle();
                muPdfDocument = null;
            }
            File corrected = new File(cacheFile.getParentFile(), cacheFile.getName() + "-fixed.epub");
            try (BookCacheLeases.PublishedFile output = convertFb2(fileName, corrected, true, notes)) {
                muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                        output.file.getPath(), password);
                publishedOutput = corrected;
            } catch (Exception secondFailure) {
                throw new IllegalStateException("Cannot convert FB2 book", secondFailure);
            }
        }

        if (notes != null) {
            muPdfDocument.setFootNotes(notes);
        } else {
            final MuPdfDocument metadataDocument = muPdfDocument;
            BookCacheLeases.startLeasedThread("@T fb2 set footnotes", Thread.NORM_PRIORITY, () -> {
                    try {
                        metadataDocument.setFootNotes(getNotes(fileName));
                        removeTempFilesIfCancel();
                    } catch (Throwable e) {
                        LOG.e(e);
                    }
            }, new File(fileName), publishedOutput);
        }

        return muPdfDocument;
    }

    private BookCacheLeases.PublishedFile convertFb2(String source, File target,
                                                    boolean fixMarkup, Map<String, String> notes)
            throws Exception {
        return ConversionCache.buildFile(target, temporary -> {
            if (!Fb2Extractor.get().convert(source, temporary.getPath(), fixMarkup, notes))
                throw new IOException("FB2 conversion failed");
            if (TempHolder.get().loadingCancelled.get())
                throw new IOException("FB2 conversion was cancelled");
            try (java.util.zip.ZipFile verified = new java.util.zip.ZipFile(temporary)) {
                if (verified.getEntry("OEBPS/fb2.fb2") == null)
                    throw new IOException("FB2 conversion omitted its document entry");
            }
        });
    }

    public Map<String, String> getNotes(String fileName) {
        Map<String, String> notes = null;
        final File jsonFile = new File(cacheFile + ".json");
        if (JsonHelper.isValidMapFile(jsonFile)) {
            notes = JsonHelper.fileToMap(jsonFile);
        } else {
            notes = Fb2Extractor.get().getFooterNotes(fileName);
            // a cancelled extraction is empty or partial, it must not stay in the cache
            if (!TempHolder.get().loadingCancelled.get()) {
                JsonHelper.mapToCacheFile(jsonFile, notes);
                LOG.d("save notes to file", jsonFile);
            }
        }
        return notes;
    }

}
