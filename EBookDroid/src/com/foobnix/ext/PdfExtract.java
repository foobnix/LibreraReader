package com.foobnix.ext;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

public class PdfExtract {

    public static String getBookOverview(final String unZipPath) {
        final StringBuilder info = new StringBuilder();
        PdfContext codecContex = new PdfContext();
        CodecDocument openDocument = codecContex.openDocument(unZipPath, "");

        info.append(openDocument.getMeta("info:Annotation"));
        info.append(openDocument.getMeta("info:Description"));

        return info.toString();
    }

    public static EbookMeta getBookMetaInformation(String unZipPath) {
        PdfContext codecContex = new PdfContext();
        CodecDocument openDocument = null;
        try {
            openDocument = codecContex.openDocument(unZipPath, "");
        } catch (RuntimeException e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
        EbookMeta meta = new EbookMeta(openDocument.getBookTitle(), openDocument.getBookAuthor());
        meta.setPagesCount(openDocument.getPageCount());
        meta.setKeywords(openDocument.getMeta("info:Keywords"));
        meta.setGenre(openDocument.getMeta("info:Subject"));
        String s1 = openDocument.getMeta("info:Sequence");
        String s2 = openDocument.getMeta("info:Seria");
        if (TxtUtils.isNotEmpty(s1)) {
            meta.setSequence(s1 + " " + s2);
        } else {
            meta.setSequence(s2);
        }

        if ("untitled".equals(meta.getTitle())) {
            meta.setTitle("");
        }
        LOG.d("PdfExtract", meta.getAuthor(), meta.getTitle(), unZipPath);
        openDocument.recycle();
        openDocument = null;
        return meta;
    }

}
