package org.ebookdroid.core.codec;

import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.RectF;

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
}
