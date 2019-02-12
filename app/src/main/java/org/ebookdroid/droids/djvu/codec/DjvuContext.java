package org.ebookdroid.droids.djvu.codec;

import org.ebookdroid.core.codec.AbstractCodecContext;

import com.foobnix.sys.TempHolder;

public class DjvuContext extends AbstractCodecContext {

    public DjvuContext() {
        super(createT());
    }

    @Override
    public DjvuDocument openDocumentInner(String fileName, final String password) {
        return new DjvuDocument(this, fileName);
    }

    @Override
    protected void freeContext() {
        try {
            TempHolder.lock.lock();
            free(getContextHandle());
        } catch (Throwable th) {
        } finally {
            TempHolder.lock.unlock();
        }
    }

    public static long createT() {
        try {
            TempHolder.lock.lock();
            return create();
        } finally {
            TempHolder.lock.unlock();
        }

    }

    private static native long create();

    private static native void free(long contextHandle);
}
