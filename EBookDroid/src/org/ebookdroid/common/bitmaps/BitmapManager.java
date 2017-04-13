package org.ebookdroid.common.bitmaps;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.ebookdroid.common.settings.AppSettings;
import org.emdev.utils.LengthUtils;

import com.foobnix.android.utils.LOG;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.util.LruCache;

public class BitmapManager {

    private final static long BITMAP_MEMORY_LIMIT = Runtime.getRuntime().freeMemory() / 2;

    private static final int GENERATION_THRESHOLD = 10;

    private static Queue<BitmapRef> pool = new ConcurrentLinkedQueue<BitmapRef>();

    private static Queue<Object> releasing = new ConcurrentLinkedQueue<Object>();

    private static final AtomicLong created = new AtomicLong();
    private static final AtomicLong reused = new AtomicLong();

    private static final AtomicLong memoryUsed = new AtomicLong();
    private static final AtomicLong memoryPooled = new AtomicLong();

    private static final LruCache<Integer, BitmapRef> used = new LruCache<Integer, BitmapRef>(100);

    private static AtomicLong generation = new AtomicLong();

    static int partSize = 1 << AppSettings.getInstance().bitmapSize;

    public static BitmapRef addBitmap(final String name, final Bitmap bitmap) {
        final BitmapRef ref = new BitmapRef(bitmap, generation.get());
        used.put(ref.id, ref);

        created.incrementAndGet();
        memoryUsed.addAndGet(ref.size);

        ref.name = name;
        return ref;
    }

    public static BitmapRef getBitmap(final String name, final int width, final int height, final Bitmap.Config config) {
        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            final Bitmap bmp = ref.bitmap;

            if (bmp != null && bmp.getConfig() == config && ref.width == width && ref.height >= height) {
                if (ref.used.compareAndSet(false, true)) {
                    it.remove();

                    ref.gen = generation.get();
                    used.put(ref.id, ref);

                    reused.incrementAndGet();
                    memoryPooled.addAndGet(-ref.size);
                    memoryUsed.addAndGet(ref.size);

                    bmp.eraseColor(Color.CYAN);
                    ref.name = name;
                    return ref;
                }
            }
        }

        // this error!
        Bitmap createBitmap;
        try {
            createBitmap = Bitmap.createBitmap(width, height, config);
        } catch (Throwable e) {
            LOG.e(e);
            createBitmap = Bitmap.createBitmap(1, 1, config);
        }
        BitmapRef ref = new BitmapRef(createBitmap, generation.get());

        used.put(ref.id, ref);

        created.incrementAndGet();
        memoryUsed.addAndGet(ref.size);

        shrinkPool(BITMAP_MEMORY_LIMIT);

        ref.name = name;
        return ref;
    }

    public static void clear(final String msg) {
        generation.addAndGet(GENERATION_THRESHOLD * 2);
        removeOldRefs();
        release();
        shrinkPool(0);
        print(msg, true);
    }

    private static void print(final String msg, final boolean showRefs) {
        long sum = 0;
        for (final BitmapRef ref : pool) {
            if (!ref.isRecycled()) {
                sum += ref.size;
            }
        }
        used.evictAll();
    }

    @SuppressWarnings("unchecked")
    public static void release() {

        generation.incrementAndGet();
        removeOldRefs();

        int count = 0;
        while (!releasing.isEmpty()) {
            final Object ref = releasing.poll();
            if (ref instanceof BitmapRef) {
                releaseImpl((BitmapRef) ref);
                count++;
            } else if (ref instanceof List) {
                final List<Bitmaps> list = (List<Bitmaps>) ref;
                for (final Bitmaps bmp : list) {
                    final BitmapRef[] bitmaps = bmp.clear();
                    if (bitmaps != null) {
                        for (final BitmapRef bitmap : bitmaps) {
                            if (bitmap != null) {
                                releaseImpl(bitmap);
                                count++;
                            }
                        }
                    }
                }
            } else {
            }
        }

        shrinkPool(BITMAP_MEMORY_LIMIT);

        print("After  release: ", false);
    }

    public static void release(final BitmapRef ref) {
        if (ref != null) {
            releasing.add(ref);
        }
    }

    public static void release(final List<Bitmaps> bitmapsToRecycle) {
        if (LengthUtils.isNotEmpty(bitmapsToRecycle)) {
            releasing.add(new ArrayList<Bitmaps>(bitmapsToRecycle));
        }
    }

    static void releaseImpl(final BitmapRef ref) {
        assert ref != null;
        if (ref.used.compareAndSet(true, false)) {
            if (null != used.remove(ref.id)) {
                memoryUsed.addAndGet(-ref.size);
            }
        }
        pool.add(ref);
        memoryPooled.addAndGet(ref.size);
    }

    private static void removeOldRefs() {
        final long gen = generation.get();
        final Iterator<BitmapRef> it = pool.iterator();
        while (it.hasNext()) {
            final BitmapRef ref = it.next();
            if (gen - ref.gen > GENERATION_THRESHOLD) {
                it.remove();
                ref.recycle();
                memoryPooled.addAndGet(-ref.size);
            }
        }
    }

    private static void shrinkPool(final long limit) {
        while (memoryPooled.get() + memoryUsed.get() > limit && !pool.isEmpty()) {
            final BitmapRef ref = pool.poll();
            if (ref != null) {
                ref.recycle();
                memoryPooled.addAndGet(-ref.size);
            }
        }

    }

    public static int getBitmapBufferSize(final int width, final int height, final Bitmap.Config config) {
        return getPixelSizeInBytes(config) * width * height;
    }

    public static int getBitmapBufferSize(final Bitmap parentBitmap, final Rect childSize) {
        int bytes = 4;
        if (parentBitmap != null) {
            bytes = BitmapManager.getPixelSizeInBytes(parentBitmap.getConfig());
        }
        return bytes * childSize.width() * childSize.height();
    }

    public static int getPartSize() {
        return partSize;
    }

    public static void setPartSize(final int partSize) {
        BitmapManager.partSize = partSize;
    }

    public static int getPixelSizeInBytes(final Bitmap.Config config) {
        switch (config) {
        case ALPHA_8:
            return 1;
        case ARGB_4444:
        case RGB_565:
            return 2;
        case ARGB_8888:
        default:
            return 4;
        }
    }
}
