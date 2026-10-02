package com.foobnix.ui2.adapter;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;

/** SAF rows can have a byte count without the legacy formatted size field. */
public final class BookSizeText {
    private BookSizeText() {}

    public static String format(FileMeta book) {
        String size = book.getSizeTxt();
        if (size == null || size.isEmpty()) {
            size = book.getSize() == null ? "" : ExtUtils.readableFileSize(book.getSize());
        }
        Integer pages = book.getPages();
        return pages == null || pages == 0 ? size : size + (size.isEmpty() ? "" : " ") + "(" + pages + ")";
    }
}
