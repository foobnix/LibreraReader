package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.djvu.codec.DjvuContext;

public class DjvuExtract {

    public static String getBookOverview(String unZipPath) {
        try {
            DjvuContext codecContex = new DjvuContext();
            CodecDocument openDocument = codecContex.openDocument(unZipPath, "");
            StringBuilder info = new StringBuilder();

            info.append(openDocument.getMeta("annotation"));
            info.append(openDocument.getMeta("Description"));
            info.append(openDocument.getMeta("summary"));
            info.append(openDocument.getMeta("comment"));

            return info.toString();
        } catch (Throwable e) {
            LOG.e(e);
        }
        return "";
    }

    public static EbookMeta getBookMetaInformation(String unZipPath) {
        DjvuContext codecContex = new DjvuContext();
        CodecDocument doc = null;
        try {
            doc = codecContex.openDocument(unZipPath, "");
        } catch (RuntimeException e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
        EbookMeta meta = new EbookMeta(doc.getBookTitle(), doc.getBookAuthor());
        meta.setPagesCount(doc.getPageCount());
        meta.setKeywords(doc.getMeta("keywords"));
        meta.setSequence(TxtUtils.joinTrim(" ", doc.getMeta("sequence"), doc.getMeta("seria")));
        meta.setGenre(TxtUtils.joinTrim(" ", doc.getMeta("subject"), doc.getMeta("subjects")));
        meta.setYear(doc.getMeta("year"));
        meta.setPublisher(doc.getMeta("publisher"));
        meta.setIsbn(doc.getMeta("isbn"));
        meta.setLang(doc.getMeta("Language"));
        String edition = doc.getMeta("Edition");
        if (TxtUtils.isNotEmpty(edition)) {
            meta.setTitle(meta.getTitle() + " - " + edition + " ed.");
        }
        LOG.d("DjvuExtract", meta.getAuthor(), meta.getTitle(), meta.getPagesCount(), unZipPath);
        doc.recycle();
        doc = null;
        return meta;
    }

}
