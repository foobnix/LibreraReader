package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
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
        fileNameCache = fileNameOriginal + BookCSS.get().isAutoHypens + AppSP.get().hypenLang + AppSP.get().isDouble + AppState.get().isAccurateFontSize + BookCSS.get().isCapitalLetter;
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

            String tempFileName = fileNameCache.hashCode()+".tmp";

            DocumentTranslatorUtil.Output output = DocumentTranslatorUtil.provideOutput(openDocument, settings, tempFileName + "-", ".html");



            try {
                translator.translate(openDocument, output.getWriter(), settings);
            } finally {
                output.getWriter().close();
            }
            documentFile.close();

            LOG.d("OdtContext create", cacheFile.getPath());


            try {
                FileInputStream in = new FileInputStream(new File(root, tempFileName+"-0.html"));
                OutputStream out = new BufferedOutputStream(new FileOutputStream(cacheFile));

                HypenUtils.applyLanguage(AppSP.get().hypenLang);
                Fb2Extractor.generateHyphenFileEpub(new InputStreamReader(in), null, out, null,null,0);
                out.close();
                in.close();

            } catch (Exception e) {
                LOG.e(e);
            }

        } catch (Exception e) {
            LOG.e(e);
        }

        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, cacheFile.getPath(), password);
        return muPdfDocument;
    }

}
