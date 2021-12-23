package org.ebookdroid.droids.djvu.codec;

import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.AbstractCodecContext;

public class DjvuContext extends AbstractCodecContext {

    public DjvuContext() {
        super(createT());
    }

    public static long createT() {
        TempHolder.lock.lock();
        try {
            return create();
        } finally {
            TempHolder.lock.unlock();
        }

    }


    private static native long create();

    private static native void free(long contextHandle);

    @Override
    public DjvuDocument openDocumentInner(String fileName, final String password) {
        return new DjvuDocument(this, fileName);
    }

    @Override
    protected void freeContext() {
        TempHolder.lock.lock();
        try {
            free(getContextHandle());
        } catch (Throwable th) {
        } finally {
            TempHolder.lock.unlock();
        }
    }
}
