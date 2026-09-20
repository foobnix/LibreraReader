package org.ebookdroid.core.codec;

import android.graphics.Bitmap;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.sys.TempHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.File;

public abstract class AbstractCodecDocument implements CodecDocument {

    protected CodecContext context;

    protected final long documentHandle;

    public String cacheFilename;
    private final List<AutoCloseable> sourceLeases = new ArrayList<>();

    /** Retain every source in a delegated open until the final document is recycled. */
    public synchronized void retainSource(File file) {
        sourceLeases.add(BookCacheLeases.acquire(file));
    }

    private synchronized void releaseSources() {
        for (AutoCloseable lease : sourceLeases) {
            try { lease.close(); } catch (Exception e) { LOG.e(e); }
        }
        sourceLeases.clear();
    }

    protected AbstractCodecDocument(final CodecContext context, long documentHandle) {
        this.context = context;
        this.documentHandle = documentHandle;
    }

    @Override public long getDocumentHandle() {
        return documentHandle;
    }

    CodecPage pageCache;
    int pageNuberCache = -1;

    @Override public CodecPage getPage(int pageNuber) {
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

    @Override protected final void finalize() throws Throwable {
        // recycle();
        super.finalize();
    }

    @Override public List<OutlineLink> getOutline() {
        return Collections.emptyList();
    }

    @Override public CodecPageInfo getUnifiedPageInfo() {
        return null;
    }

    @Override public CodecPageInfo getPageInfo(final int pageIndex) {
        return null;
    }

    @Override public Map<String, String> getFootNotes() {
        return null;
    }

    @Override public final void recycle() {
        //LOG.d("ACD","recycle",isRecycled());
        TempHolder.lock.lock();
        try {
            TempHolder.get().lastRecycledDocument = documentHandle;
            if (!isRecycled()) {
                try {
                    context.recycle();
                    context = null;
                    freeDocument();
                } finally {
                    releaseSources();
                }
            }
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override public final boolean isRecycled() {
        return context == null || context.isRecycled();
    }

    protected void freeDocument() {

    }

    @Override public Bitmap getEmbeddedThumbnail() {
        return null;
    }

    @Override public String getBookAuthor() {
        return "";
    }

    @Override public String getBookTitle() {
        return "";
    }

    @Override public List<String> getMetaKeys() {
        return new ArrayList<String>();
    }

    @Override public String getBookmarkText(int page) {
        return null;
    }

    @Override public int findBookmarkPage(int page, String text, String pageText) {
        return -1;
    }

}
