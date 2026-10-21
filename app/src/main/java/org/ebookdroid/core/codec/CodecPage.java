package org.ebookdroid.core.codec;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

import com.foobnix.pdf.info.model.AnnotationType;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import java.util.List;

public interface CodecPage {

	int getWidth();

	int getHeight();

	BitmapRef renderBitmap(int width, int height, RectF pageSliceBounds, boolean cache);

    BitmapRef renderBitmapSimple(int width, int height, RectF pageSliceBounds);


	Bitmap renderThumbnail(int width);

	Bitmap renderThumbnail(int width, int originW, int originH);

	List<PageLink> getPageLinks();

	List<Annotation> getAnnotations();
	List<Annotation> getAnnotationsImpl();

	public TextWord[][] getText();

	/**
	 * The rectangles a stored location covers on this page, each 0..1 of the page, or empty.
	 *
	 * This is how a mark Librera keeps itself - a highlight on an epub, say - finds its words
	 * again after the book has been laid out at a different size. Only an engine that can name a
	 * place can answer: foliate-js resolves the CFI back to the characters it was taken from.
	 * Empty means "not on this page", which is the usual answer.
	 */
	List<RectF> getLocationRects(String location);

	void recycle();

	boolean isRecycled();

    public void addAnnotation(float[] color, PointF[][] points, float width, float alpha);

	long getPageHandle();

	void addMarkupAnnotation(PointF[] quadPoints, AnnotationType type, float[] color);

    String getPageHTML();

    String getPageHTMLWithImages();

    int getCharCount();

}
