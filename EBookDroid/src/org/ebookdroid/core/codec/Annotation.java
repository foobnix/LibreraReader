package org.ebookdroid.core.codec;

import android.graphics.RectF;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.model.AnnotationType;

public class Annotation extends RectF {

    private int index;
    private int page;
    private long pageHandler;
    public final AnnotationType type;
    public final String text;

    public Annotation(final float x0, final float y0, final float x1, final float y1, final int _type, String text) {
        super(x0, y0, x1, y1);
        type = _type == -1 ? AnnotationType.UNKNOWN : AnnotationType.values()[_type];
        this.text = text;
        LOG.d("Annotation text2", text);
    }

    public Annotation(int page, int index) {
        this.page = page;
        this.index = index;
        this.type = AnnotationType.INK;
        this.text = "";
    }

    @Override
    public boolean equals(Object o) {
        return index == ((Annotation) o).index && page == ((Annotation) o).page;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getPageHandler() {
        return pageHandler;
    }

    public void setPageHandler(long pageHandler) {
        this.pageHandler = pageHandler;
    }

}
