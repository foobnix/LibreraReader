package org.ebookdroid.core.codec;


import java.lang.ref.SoftReference;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CodecPageHolder {

    private final GetPageOp OP_GET_PAGE_OP = new GetPageOp();

    private final IsInvalidOp OP_IS_INVALID = new IsInvalidOp();

    private final RecycleOp OP_RECYCLE = new RecycleOp();

    private final AtomicLong lock = new AtomicLong();

    private final AtomicBoolean access = new AtomicBoolean();

    private final CodecDocument document;
    private final int pageIndex;

    private SoftReference<CodecPage> ref = new SoftReference<CodecPage>(null);

    public CodecPageHolder(final CodecDocument document, final int pageIndex) {
        this.document = document;
        this.pageIndex = pageIndex;
    }

    public CodecPage getPage(long taskId) {
        return access(taskId, OP_GET_PAGE_OP, true, null);
    }

    public boolean isInvalid(long taskId) {
        return access(taskId, OP_IS_INVALID, false, false);
    }

    public boolean recycle(long taskId, final boolean shutdown) {
        if (!shutdown && locked()) {
            return false;
        }
        return access(taskId, OP_RECYCLE, true, false);
    }

    protected <T> T access(long taskId, final Callable<T> operation, final boolean wait, final T defValue) {
        final String name = operation.getClass().getSimpleName();
        while (true) {
            if (access.compareAndSet(false, true)) {
                T result = null;
                try {
                    result = operation.call();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                } finally {
                    access.set(false);
                    synchronized (access) {
                        access.notifyAll();
                    }
                }
                return result;
            } else {
                if (!wait) {
                    return defValue;
                }
                synchronized (access) {
                    try {
                        access.wait(100);
                    } catch (final InterruptedException ex) {
                        Thread.interrupted();
                    }
                }
            }
        }
    }

    public final boolean lock() {
        long res = lock.incrementAndGet();
        return res > 0;
    }

    public final boolean locked() {
        return lock.get() > 0;
    }

    public final boolean unlock() {
        long res = lock.decrementAndGet();
        return res > 0;
    }

    private class GetPageOp implements Callable<CodecPage> {

        @Override
        public CodecPage call() {
            CodecPage page = ref.get();
            if (page == null || page.isRecycled()) {
                page = null;
                page = document.getPage(pageIndex);
                ref = new SoftReference<CodecPage>(page);
            }
            lock();
            return page;
        }
    }

    private class IsInvalidOp implements Callable<Boolean> {

        @Override
        public Boolean call() {
            final CodecPage page = ref.get();
            return page == null || page.isRecycled();
        }
    }

    private class RecycleOp implements Callable<Boolean> {

        @Override
        public Boolean call() {
            final CodecPage page = ref.get();
            if (page != null && !page.isRecycled()) {
                page.recycle();
                ref = new SoftReference<CodecPage>(null);
                return true;
            }
            return false;
        }
    }

}
