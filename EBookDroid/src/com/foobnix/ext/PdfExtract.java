package com.foobnix.ext;

import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.droids.mupdf.codec.PdfContext;

public class PdfExtract {

    public static EbookMeta getBookMetaInformation(String unZipPath) {
        CodecContext codecContex = new PdfContext();
        CodecDocument openDocument = codecContex.openDocument(unZipPath, "");
        EbookMeta meta = new EbookMeta(openDocument.getBookTitle(), openDocument.getBookAuthor());
        openDocument.recycle();
        return meta;
    }

}
