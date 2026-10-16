package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.FooterNote;
import com.foobnix.ext.HtmlExtractor;
import com.foobnix.hypen.HypenUtils;

import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import java.util.Map;

public class HtmlContext extends PdfContext {

    @Override
    public CodecDocument openDocumentInner(String fileName, String password) {
        try {
            final CodecDocument document = openDocumentInnerForce(fileName, password, false);
            if (document != null) {
                return document;
            }
            // No engine could lay the page out as it is; the cleaned-up version is its second
            // chance, which is what a throw from the engine has always been answered with
            LOG.d("HtmlContext the page did not open, cleaning it up", fileName);
        } catch (Exception e1) {
            LOG.e(e1);
        }
        return openDocumentInnerForce(fileName, password, true);
    }

    public CodecDocument openDocumentInnerForce(String fileName, String password, boolean forse) {


//        if(AppsConfig.IS_LOG){
//            MuPdfDocument muPdfDocument = new MuPdfDocument(this, MuPdfDocument.FORMAT_PDF, fileName, password);
//            return muPdfDocument;
//        }



        Map<String, String> notes = null;
        try {
            FooterNote extract = HtmlExtractor.extract(fileName, CacheZipUtils.CACHE_BOOK_DIR.getPath(), forse);

            fileName = extract.path;
            notes = extract.notes;
            LOG.d("new file name", fileName);
        } catch (Exception e) {
            LOG.e(e);
        }

        AbstractCodecDocument document = openReflowableHtml(fileName, password);
        if (document == null) {
            // No engine could open it; openReflowable has logged why
            return null;
        }
        document.setFootNotes(notes);
        return document;
    }

}
