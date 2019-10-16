package org.ebookdroid.core.codec;

import android.graphics.Bitmap;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractCodecDocument implements CodecDocument {

    protected final CodecContext context;

    protected final long documentHandle;

    protected AbstractCodecDocument(final CodecContext context, long documentHandle) {
        this.context = context;
        this.documentHandle = documentHandle;
    }

    @Override
    public long getDocumentHandle() {
        return documentHandle;
    }

    CodecPage pageCache;
    int pageNuberCache = -1;

    @Override
    public CodecPage getPage(int pageNuber) {
        if (pageNuber == pageNuberCache) {
            LOG.d("getPage-cache", pageNuber);
            if (pageCache != null && !pageCache.isRecycled()) {
                return pageCache;
            }
        }

        pageNuberCache = pageNuber;
        pageCache = getPageInner(pageNuber);
        return pageCache;
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
        TempHolder.lock.lock();
        try {
            TempHolder.get().lastRecycledDocument = documentHandle;
            if (!isRecycled()) {
                context.recycle();
                freeDocument();
            }
        } finally {
            TempHolder.lock.unlock();
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

    @Override
    public String getBookAuthor() {
        return "";
    }

    @Override
    public String getBookTitle() {
        return "";
    }

    @Override
    public List<String> getMetaKeys() {
        return new ArrayList<String>();
    }

}
