package org.ebookdroid.core.codec;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.foobnix.android.utils.LOG;

import android.graphics.Bitmap;

public abstract class AbstractCodecDocument implements CodecDocument {

    protected final CodecContext context;

    protected final long documentHandle;

    protected AbstractCodecDocument(final CodecContext context, long documentHandle) {
        this.context = context;
        this.documentHandle = documentHandle;

        number = -1;
        pageCodec = null;
    }

    int number = -1;
    CodecPage pageCodec = null;

    @Override
    public CodecPage getPage(int pageNuber) {
        if (number == pageNuber && pageCodec != null && !pageCodec.isRecycled()) {
            LOG.d("CodecPage cache", pageNuber);
            return pageCodec;
        }
        CodecPage pageInner = getPageInner(pageNuber);
        pageCodec = pageInner;
        number = pageNuber;
        return pageInner;
    }

    @Override
    protected final void finalize() throws Throwable {
        // recycle();
        super.finalize();
    }

    @Override
    public List<OutlineLink> getOutline() {
        return Collections.emptyList();
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        return null;
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageIndex) {
        return null;
    }

    @Override
    public Map<String, String> getFootNotes() {
        return null;
    }

    @Override
    public final void recycle() {
        if (!isRecycled()) {
            context.recycle();
            freeDocument();
            number = -1;
            pageCodec = null;
        }
    }

    @Override
    public final boolean isRecycled() {
        return context == null || context.isRecycled();
    }

    protected void freeDocument() {
    }

    @Override
    public Bitmap getEmbeddedThumbnail() {
        return null;
    }

}
