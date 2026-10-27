package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.HtmlExtractor;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.droids.mupdf.codec.MuPdfDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.util.Map;

public class MhtContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {

        Map<String, String> notes = null;
        try {
            FooterNote extract = HtmlExtractor.extractMht(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath());
            fileName = extract.path;
            notes = extract.notes;
            LOG.d("new file name", fileName);
        } catch (Exception e) {
            LOG.e(e);
        }

        AbstractCodecDocument muPdfDocument = openReflowableHtml(fileName, password);
        if (muPdfDocument == null) {
            // No engine could open it; openReflowable has logged why
            return null;
        }
        muPdfDocument.setFootNotes(notes);
        return muPdfDocument;
    }

}
