package org.ebookdroid.core.codec;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;

public abstract class AbstractCodecDocument implements CodecDocument {

    protected final CodecContext context;

    protected final long documentHandle;

    protected AbstractCodecDocument(final CodecContext context, long documentHandle) {
        this.context = context;
        this.documentHandle = documentHandle;
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
