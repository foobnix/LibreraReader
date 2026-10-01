package com.foobnix.ui2.adapter;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class BookSizeTextTest {
    @Test public void safSizeWithoutFormattedFieldIncludesPages() {
        FileMeta book = new FileMeta("content://fixture/book");
        book.setSize(16456139L); book.setPages(1011);
        assertEquals(ExtUtils.readableFileSize(16456139L) + " (1011)", BookSizeText.format(book));
    }
    @Test public void unknownSizeDoesNotRenderNull() {
        FileMeta book = new FileMeta("content://fixture/book");
        assertEquals("", BookSizeText.format(book));
        book.setPages(1011); assertEquals("(1011)", BookSizeText.format(book));
    }
    @Test public void existingLocalSizeLabelIsPreserved() {
        FileMeta book = new FileMeta("/fixture/book.pdf");
        book.setSizeTxt("16.46MB");
        assertEquals("16.46MB", BookSizeText.format(book));
    }
}
