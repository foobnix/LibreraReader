package org.ebookdroid.core.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.foobnix.sys.TempHolder;

import android.graphics.Bitmap;

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

    @Override
    public CodecPage getPage(int pageNuber) {
        CodecPage pageInner = getPageInner(pageNuber);
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
        try {
            TempHolder.lock.lock();
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
