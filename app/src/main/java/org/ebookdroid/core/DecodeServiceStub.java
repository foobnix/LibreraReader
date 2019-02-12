package org.ebookdroid.core;

import java.util.List;
import java.util.Map;

import org.ebookdroid.core.codec.Annotation;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPageHolder;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageLink;
import org.ebookdroid.droids.mupdf.codec.TextWord;

import com.foobnix.android.utils.ResultResponse;
import com.foobnix.pdf.info.model.AnnotationType;

import android.graphics.Bitmap.Config;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.util.Pair;

public class DecodeServiceStub implements DecodeService {

	private static final CodecPageInfo DEFAULT = new CodecPageInfo(0, 0);

	@Override
	public Map<Integer, CodecPageHolder> getPages() {
		return null;
	}

	@Override
	public void processTextForPages(Page[] pages) {
	}

	@Override
	public void searchText(String text, Page[] pages, ResultResponse<Integer> response, Runnable finish) {

	}

    @Override
    public CodecDocument getCodecDocument() {
        return null;
    }

    @Override
    public List<String> getAttachemnts() {
        return null;
    }

    @Override
    public List<PageLink> getLinksForPage(int page) {
        return null;
    }
	/**
	 * {@inheritDoc}1
	 * 
	 * @see org.ebookdroid.core.DecodeService#open(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public void open(String fileName, String password) {
	}

    @Override
    public String getPageHTML(int page) {
        return null;
    }

	@Override
	public void deleteAnnotation(long pageHandle, int page, int index, ResultResponse<List<Annotation>> resultResponse) {
		// TODO Auto-generated method stub
	}

    @Override
    public TextWord[][] getTextForPage(int page) {
        return null;
    }

	@Override
	public boolean hasAnnotationChanges() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void underlineText(int page, PointF[] points, int color, AnnotationType type, ResultResponse<List<Annotation>> resultResponse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveAnnotations(String path, Runnable runnable) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#decodePage(org.ebookdroid.core.ViewState,
	 *      org.ebookdroid.core.PageTreeNode)
	 */
	@Override
	public void decodePage(ViewState viewState, PageTreeNode node) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#stopDecoding(org.ebookdroid.core.PageTreeNode,
	 *      java.lang.String)
	 */
	@Override
	public void stopDecoding(PageTreeNode node, String reason) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#getPageCount()
	 */
	@Override
	public int getPageCount() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#getOutline()
	 */
	@Override
	public void getOutline(com.foobnix.android.utils.ResultResponse<java.util.List<OutlineLink>> response) {

	};

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#getUnifiedPageInfo()
	 */
	@Override
	public CodecPageInfo getUnifiedPageInfo() {
		return DEFAULT;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#getPageInfo(int)
	 */
	@Override
	public CodecPageInfo getPageInfo(int pageIndex) {
		return DEFAULT;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#recycle()
	 */
	@Override
	public void recycle() {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#updateViewState(org.ebookdroid.core.ViewState)
	 */
	@Override
	public void updateViewState(ViewState viewState) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#isPageSizeCacheable()
	 */
	@Override
	public boolean isPageSizeCacheable() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#getPixelFormat()
	 */
	@Override
	public int getPixelFormat() {
		return PixelFormat.RGBA_8888;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.ebookdroid.core.DecodeService#getBitmapConfig()
	 */
	@Override
	public Config getBitmapConfig() {
		return Config.ARGB_8888;
	}

	@Override
    public void updateAnnotation(int page, float[] color, PointF[][] points, float width, float alpha) {

	}

	@Override
    public void addAnnotation(Map<Integer, List<PointF>> points, int color, float width, float alpha, ResultResponse<Pair<Integer, List<Annotation>>> resultResponse) {
		// TODO Auto-generated method stub

	}

    @Override
    public String getFooterNote(String text) {
        return "";
    }

}
