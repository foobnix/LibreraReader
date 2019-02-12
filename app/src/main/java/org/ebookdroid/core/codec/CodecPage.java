package org.ebookdroid.core.codec;

import java.util.List;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import com.foobnix.pdf.info.model.AnnotationType;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;

public interface CodecPage {

	int getWidth();

	int getHeight();

	BitmapRef renderBitmap(int width, int height, RectF pageSliceBounds);

    BitmapRef renderBitmapSimple(int width, int height, RectF pageSliceBounds);


	Bitmap renderThumbnail(int width);

	Bitmap renderThumbnail(int width, int originW, int originH);

	List<PageLink> getPageLinks();

	List<Annotation> getAnnotations();

	public TextWord[][] getText();

	void recycle();

	boolean isRecycled();

    public void addAnnotation(float[] color, PointF[][] points, float width, float alpha);

	long getPageHandle();

	void addMarkupAnnotation(PointF[] quadPoints, AnnotationType type, float[] color);

    String getPageHTML();

    String getPageHTMLWithImages();

    int getCharCount();

}
