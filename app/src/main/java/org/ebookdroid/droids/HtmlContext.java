package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.HtmlExtractor;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.util.Map;

public class HtmlContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        try {
            return openDocumentInnerForce(fileName, password, false);
        } catch (Exception e1) {
            LOG.e(e1);
            return openDocumentInnerForce(fileName, password, true);
        }

    }

    public CodecDocument openDocumentInnerForce(String fileName, String password, boolean forse) {

        Map<String, String> notes = null;
        try {
            FooterNote extract = HtmlExtractor.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), forse);
            fileName = extract.path;
            notes = extract.notes;
            LOG.d("new file name", fileName);
        } catch (Exception e) {
            LOG.e(e);
        }

        MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
        muPdfDocument.setFootNotes(notes);
        return muPdfDocument;
    }

}
