package org.ebookdroid.core;

import java.util.List;
import java.util.Map;

import org.ebookdroid.common.bitmaps.BitmapRef;
import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageHolder;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.model.AnnotationType;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;

public interface DecodeService {

	void open(String fileName, String password);

	void decodePage(ViewState viewState, PageTreeNode node);

	void stopDecoding(PageTreeNode node, String reason);

	int getPageCount();

	void getOutline(final ResultResponse<List<OutlineLink>> response);

    String getFooterNote(String text);

    List<String> getAttachemnts();

	CodecPageInfo getUnifiedPageInfo();

	CodecPageInfo getPageInfo(int pageIndex);

	void recycle();

    void updateAnnotation(int page, float[] color, PointF[][] points, float width, float alpha);

	void updateViewState(ViewState viewState);

	boolean isPageSizeCacheable();

	int getPixelFormat();

	Bitmap.Config getBitmapConfig();

	interface DecodeCallback {

		void decodeComplete(CodecPage codecPage, BitmapRef bitmap, Rect bitmapBounds, RectF croppedPageBounds);

	}

    void addAnnotation(Map<Integer, List<PointF>> points, int color, float width, float alpha, ResultResponse<Pair<Integer, List<Annotation>>> resultResponse);

	Map<Integer, CodecPageHolder> getPages();

	void deleteAnnotation(final long pageHandle, final int page, final int index, final ResultResponse<List<Annotation>> response);

	boolean hasAnnotationChanges();

	void saveAnnotations(String path, Runnable runnable);

	void underlineText(int page, PointF[] points, int color, AnnotationType type, ResultResponse<List<Annotation>> resultResponse);

	void processTextForPages(Page[] pages);

	void searchText(String text, Page[] pages, ResultResponse<Integer> response, Runnable finish);

    TextWord[][] getTextForPage(int page);

    public String getPageHTML(int page);

    List<PageLink> getLinksForPage(int page);

    CodecDocument getCodecDocument();

}
