package org.ebookdroid.droids;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.libmobi.LibMobi;
import com.foobnix.pdf.info.model.BookCSS;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.File;

public class DocContext extends PdfContext {


    File cacheFile;

    @Override
    public File getCacheFileName(String fileNameOriginal) {
        fileNameOriginal = fileNameOriginal + BookCSS.get().isAutoHypens + BookCSS.get().hypenLang;
        cacheFile = new File(CacheZipUtils.CACHE_BOOK_DIR, fileNameOriginal.hashCode() + ".html");
        return cacheFile;
    }

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        if (!cacheFile.isFile()) {
            LibMobi.convertDocToHtml(fileName, cacheFile.getPath());
        }

         MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, cacheFile.getPath(), password);
         return muPdfDocument;
    }


}
