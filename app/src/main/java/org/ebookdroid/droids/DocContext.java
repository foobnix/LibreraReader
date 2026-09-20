package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.ext.CbzCbrExtractor;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.libmobi.LibMobi;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

public class DocContext extends PdfContext {

    @FunctionalInterface interface NativeConverter {
        int convert(String source, String output);
    }

    private final NativeConverter nativeConverter;

    public DocContext() { this(LibMobi::convertDocToHtml); }

    DocContext(NativeConverter nativeConverter) { this.nativeConverter = nativeConverter; }

    public static String EXT_DOC_HTML = ".doc.html";

    File cacheFile;

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + EpubProcessingSettings.key();
        cacheFile = new File(new File(CacheZipUtils.CACHE_BOOK_DIR,
                ConversionCache.key(fileNameOriginal) + "-doc-v2"), "book" + EXT_DOC_HTML);
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        boolean isDoc = CbzCbrExtractor.isDoc(fileName);
        LOG.d("isDOC", "isDOC" + isDoc);
        if (!isDoc) {
            return new TxtContext().openDocumentInner(fileName, password);
        }

        if (cacheFile == null) cacheFile = getCacheFileName(fileName);
        try (BookCacheLeases.PublishedFile output = ConversionCache.buildDirectory(
                cacheFile.getParentFile(), cacheFile.getName(), directory -> {
                    File nativeOutput = new File(directory, "source.html");
                    int status = nativeConverter.convert(fileName, nativeOutput.getPath());
                    if (status == 0 || !nativeOutput.isFile() || nativeOutput.length() == 0)
                        throw new UnsupportedDocException();
                    try (FileInputStream in = new FileInputStream(nativeOutput);
                         OutputStream out = new BufferedOutputStream(
                                 new FileOutputStream(new File(directory, cacheFile.getName())))) {
                        HypenUtils.applyLanguage(EpubProcessingSettings.language());
                        Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(in), null,
                                out, null, null, 0, new ArrayList<>());
                    }
                })) {
            MuPdfDocument document = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                    output.file.getPath(), password);
            document.retainCacheSource(cacheFile.getParentFile());
            return document;
        } catch (UnsupportedDocException fallback) {
            return new RtfContext().openDocumentInner(fileName, password);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot convert DOC book", failure);
        }
    }

    private static final class UnsupportedDocException extends Exception { }


}
