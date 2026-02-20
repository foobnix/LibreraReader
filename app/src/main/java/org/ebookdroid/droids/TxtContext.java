package org.ebookdroid.droids;

import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.TxtExtract;
import com.foobnix.model.AppState;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.io.IOException;

public class TxtContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        String extractFile = null;
        try {
            CacheZipUtils.emptyAllCacheDirs();
            if (AppState.get().isPreText) {
                extractFile = TxtExtract.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath());
                MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, extractFile, "");
                return muPdfDocument;
            }else {
                extractFile = TxtExtract.extract1(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new Fb2Context().openDocumentInner(extractFile,"");

    }
}
