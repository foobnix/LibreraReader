package com.foobnix.ext;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.djvu.codec.DjvuContext;

import com.foobnix.android.utils.LOG;

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
        CodecDocument openDocument = null;
        try {
            openDocument = codecContex.openDocument(unZipPath, "");
        } catch (RuntimeException e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
        EbookMeta meta = new EbookMeta(openDocument.getBookTitle(), openDocument.getBookAuthor());
        meta.setPagesCount(openDocument.getPageCount());
        meta.setKeywords(openDocument.getMeta("keywords"));
        meta.setSequence(openDocument.getMeta("sequence") + "" + openDocument.getMeta("seria"));
        meta.setGenre(openDocument.getMeta("subject"));
        meta.setYear(openDocument.getMeta("year"));
        meta.setPublisher(openDocument.getMeta("publisher"));
        meta.setIsbn(openDocument.getMeta("isbn"));
        LOG.d("DjvuExtract", meta.getAuthor(), meta.getTitle(), meta.getPagesCount(), unZipPath);
        openDocument.recycle();
        openDocument = null;
        return meta;
    }

}
