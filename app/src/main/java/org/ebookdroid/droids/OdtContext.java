package org.ebookdroid.droids;

import java.io.File;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppState;

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

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameCache = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang + AppState.get().isDouble + AppState.get().isAccurateFontSize + BookCSS.get().isCapitalLetter;
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameCache.hashCode() + "-0.html");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        try {
            if (cacheFile.isFile()) {
                LOG.d("OdtContext cache", cacheFile.getPath());
                MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, cacheFile.getPath(), password);
                return muPdfDocument;
            }

            CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles());

            LocatedOpenDocumentFile documentFile = new LocatedOpenDocumentFile(fileName);

            OpenDocument openDocument = documentFile.getAsDocument();

            TextTranslator translator = new TextTranslator();

            TranslationSettings settings = new TranslationSettings();

            String root = CacheZipUtils.CACHE_BOOK_DIR.getPath();
            settings.setCache(new DefaultFileCache(root));
            settings.setImageStoreMode(ImageStoreMode.CACHE);
            DocumentTranslatorUtil.Output output = DocumentTranslatorUtil.provideOutput(openDocument, settings, fileNameCache.hashCode() + "-", ".html");

            LOG.d("OdtContext create", cacheFile.getPath());

            try {
                translator.translate(openDocument, output.getWriter(), settings);
            } finally {
                output.getWriter().close();
            }
            documentFile.close();
        } catch (Exception e) {
            LOG.e(e);
        }

        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, cacheFile.getPath(), password);
        return muPdfDocument;
    }

}
