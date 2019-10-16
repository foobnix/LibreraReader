package com.foobnix.ext;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

public class PdfExtract {

    public static int SUBJECT_LIMIT = 200;

    public static String getBookOverview(final String unZipPath) {
        final StringBuilder info = new StringBuilder();
        PdfContext codecContex = new PdfContext();
        CodecDocument openDocument = codecContex.openDocument(unZipPath, "");

        info.append(openDocument.getMeta("info:Annotation"));
        info.append(openDocument.getMeta("info:Description"));

        String subjectLikeDescription = openDocument.getMeta("info:Subject");
        if (subjectLikeDescription != null && subjectLikeDescription.length() > SUBJECT_LIMIT) {
            info.append(subjectLikeDescription);
        }

        return info.toString();
    }

    public static EbookMeta getBookMetaInformation(String unZipPath) {
        PdfContext codecContex = new PdfContext();
        CodecDocument doc = null;
        try {
            doc = codecContex.openDocument(unZipPath, "");
        } catch (RuntimeException e) {
            LOG.e(e);
            return EbookMeta.Empty();
        }
        if (doc == null) {
            return EbookMeta.Empty();
        }
        EbookMeta meta = new EbookMeta(doc.getBookTitle(), doc.getBookAuthor());
        meta.setPagesCount(doc.getPageCount());
        meta.setKeywords(doc.getMeta("info:Keywords"));
        String subjectLikeGenre = doc.getMeta("info:Subject");
        LOG.d("subjectLikeGenre", subjectLikeGenre, subjectLikeGenre != null ? subjectLikeGenre.length() : 0);
        if (subjectLikeGenre != null && (subjectLikeGenre.contains(";") || subjectLikeGenre.length() <= SUBJECT_LIMIT)) {
            meta.setGenre(subjectLikeGenre);
        }
        meta.setYear(doc.getMeta("info:CreationDate"));
        meta.setPublisher(doc.getMeta("info:Publisher"));
        meta.setIsbn(doc.getMeta("info:ISBN"));
        meta.setPublisher(TxtUtils.joinTrim(" ", doc.getMeta("info:Publisher"), doc.getMeta("info:EBX_PUBLISHER")));
        meta.setSequence(TxtUtils.joinTrim(" ", doc.getMeta("info:Sequence"), doc.getMeta("info:Seria")));

        if ("untitled".equals(meta.getTitle())) {
            meta.setTitle("");
        }

        String edition = doc.getMeta("info:Edition");
        if (TxtUtils.isNotEmpty(edition)) {
            meta.setTitle(meta.getTitle() + " - " + edition + " ed.");
        }

        LOG.d("PdfExtract", meta.getAuthor(), meta.getTitle(), unZipPath);
        doc.recycle();
        doc = null;
        return meta;
    }

}
