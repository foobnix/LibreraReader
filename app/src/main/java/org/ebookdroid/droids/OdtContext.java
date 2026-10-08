package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.ConversionCache;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.hypen.HypenUtils;
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

import at.stefl.opendocument.java.odf.LocatedOpenDocumentFile;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.translator.document.DocumentTranslatorUtil;
import at.stefl.opendocument.java.translator.document.TextTranslator;
import at.stefl.opendocument.java.translator.settings.ImageStoreMode;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;
import at.stefl.opendocument.java.util.DefaultFileCache;

public class OdtContext extends PdfContext {


    File cacheFile;
    String fileNameCache;

    @Override protected EpubProcessingSettings.Scope captureProcessingSettings(String path) {
        return EpubProcessingSettings.capture();
    }

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameCache = fileNameOriginal + EpubProcessingSettings.key();
        cacheFile = new File(new File(CacheZipUtils.CACHE_BOOK_DIR,
                ConversionCache.key(fileNameCache) + "-odt-v2"), "book.html");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        if (cacheFile == null) cacheFile = getCacheFileName(fileName);
        try (BookCacheLeases.PublishedFile output = ConversionCache.buildDirectory(
                cacheFile.getParentFile(), cacheFile.getName(), directory -> {
                    LocatedOpenDocumentFile documentFile = new LocatedOpenDocumentFile(fileName);
                    try {
                        OpenDocument openDocument = documentFile.getAsDocument();
                        TranslationSettings settings = new TranslationSettings();
                        settings.setCache(new DefaultFileCache(directory.getPath()));
                        settings.setImageStoreMode(ImageStoreMode.CACHE);
                        DocumentTranslatorUtil.Output translated = DocumentTranslatorUtil.provideOutput(
                                openDocument, settings, "source-", ".html");
                        try {
                            new TextTranslator().translate(openDocument, translated.getWriter(), settings);
                        } finally {
                            translated.getWriter().close();
                        }
                        try (FileInputStream in = new FileInputStream(new File(directory, "source-0.html"));
                             OutputStream out = new BufferedOutputStream(
                                     new FileOutputStream(new File(directory, cacheFile.getName())))) {
                            HypenUtils.applyLanguage(EpubProcessingSettings.language());
                            Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(in), null,
                                    out, null, null, 0, new ArrayList<>());
                        }
                    } finally {
                        documentFile.close();
                    }
                })) {
            MuPdfDocument document = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF,
                    output.file.getPath(), password);
            document.retainCacheSource(cacheFile.getParentFile());
            return document;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot convert ODT book", failure);
        }
    }

}
