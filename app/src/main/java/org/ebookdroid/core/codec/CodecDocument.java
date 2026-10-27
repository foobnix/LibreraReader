package org.ebookdroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.RectF;

import org.ebookdroid.BookType;

import java.util.List;
import java.util.Map;

public interface CodecDocument {

    public long getDocumentHandle();

    int getPageCount();

    int getPageCount(int w, int h, int fsize);

    CodecPage getPage(int pageNuber);

    CodecPage getPageInner(int pageNuber);

    CodecPageInfo getUnifiedPageInfo();

    CodecPageInfo getPageInfo(int pageNuber);

    List<? extends RectF> searchText(int pageNuber, final String pattern) throws DocSearchNotSupported;

    List<OutlineLink> getOutline();

    Map<String, String> getFootNotes();

    List<String> getMediaAttachments();

    void recycle();

    BookType getBookType();

    /**
     * @return <code>true</code> if instance has been recycled
     */
    boolean isRecycled();

    Bitmap getEmbeddedThumbnail();

    public static class DocSearchNotSupported extends Exception {

        /**
         * Serial version UID
         */
        private static final long serialVersionUID = 6741243859033574916L;

    }

	boolean hasChanges();
	void deleteAnnotation(long pageNumber, int index);

	void saveAnnotations(String path);

    String documentToHtml();

    String getBookTitle();

    String getBookAuthor();

    String getMeta(String option);

    List<String> getMetaKeys();

    void setMeta(String key, String value);

    // page is 1-based, the text is saved to AppBookmark.pt
    /**
     * The exact place the reader is now, in whatever form this engine names places, or
     * <code>null</code> when it cannot name one.
     *
     * Only foliate-js can: a CFI, which survives the book being laid out again at any font size,
     * screen or device. It is saved beside the percentage, never instead of it - see
     * {@link com.foobnix.model.AppBook#cfi}.
     *
     * @param page the page the reader is on, 1-based
     */
    String getLocation(int page);

    /**
     * A location naming the characters under a selection, when the engine can name one.
     *
     * This is what anchors a mark the reader makes: the words it was put on, rather than the
     * place on the page they happen to be at this font size.
     *
     * @param page  the page the selection is on, 1-based
     * @param rects the selected rectangles, each normalized to the page
     */
    String getLocation(int page, java.util.List<android.graphics.RectF> rects);

    /**
     * The page a location names.
     *
     * @return the 1-based page, or -1 when the location is empty, not this engine's kind, or no
     *         longer resolvable in this book
     */
    int findLocationPage(String location);

    String getBookmarkText(int page);

    // page is estimated by the bookmark percent, returns the nearest page (+-10) with the text,
    // else the nearest page with the page text, else -1
    int findBookmarkPage(int page, String text, String pageText);
}
