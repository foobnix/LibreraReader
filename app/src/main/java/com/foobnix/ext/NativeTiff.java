package com.foobnix.ext;

import android.graphics.Bitmap;

import com.foobnix.android.utils.LOG;

import java.nio.charset.StandardCharsets;

/**
 * TIFF read with libtiff: the TIFF pages of a comic archive and a TIFF cover, which Android's own
 * image decoder cannot read, given as their bytes - and a .tif or .tiff book ({@link Book}). See Builder/jni/librera_tiff.c, built into libtiff.so, which every flavour
 * carries - with MuPDF or without - so a TIFF image is read the same way in all.
 *
 * Bytes are read for their first image.
 */
public final class NativeTiff {

    /** libtiff.so, or <code>false</code> when it would not load and no TIFF is read. */
    private static final boolean LOADED = load();

    private NativeTiff() {
    }

    /** Whether the bytes start the way a TIFF does, classic or BigTIFF, of either byte order. */
    public static boolean isTiff(final byte[] b) {
        return b != null && b.length >= 8
                && ((b[0] == 'I' && b[1] == 'I' && (b[2] == 42 || b[2] == 43) && b[3] == 0)
                || (b[0] == 'M' && b[1] == 'M' && b[2] == 0 && (b[3] == 42 || b[3] == 43)));
    }

    /** The width and height of the image, read off its header alone; <code>null</code> if it is not one. */
    public static int[] size(final byte[] data) {
        if (!LOADED || !isTiff(data)) {
            return null;
        }
        try {
            return nativeSize(data);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * Decodes the image, made smaller as it is decoded the way BitmapFactory's inSampleSize does:
     * <code>sample</code> times each way, each pixel the average of those it stands for.
     *
     * @return the image as an ARGB_8888 bitmap, or <code>null</code> when it cannot be read
     */
    public static Bitmap decode(final byte[] data, final int sample) {
        final int[] size = size(data);
        if (size == null) {
            return null;
        }
        return decode(size, sample, data, 0, 0);
    }

    /** A bitmap of the image shrunk by sample, filled from the bytes or else the book's page. */
    private static Bitmap decode(final int[] size, final int sample, final byte[] data, final long book,
                                 final int page) {
        final int s = Math.max(1, sample);
        final int width = (size[0] + s - 1) / s;
        final int height = (size[1] + s - 1) / s;
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            if (data != null ? nativeDecode(data, s, bitmap) : nativeDecodePage(book, page, s, bitmap)) {
                return bitmap;
            }
        } catch (Throwable e) {
            LOG.e(e, size[0], size[1], s);
        }
        if (bitmap != null) {
            bitmap.recycle();
        }
        return null;
    }

    /**
     * A .tif or .tiff opened as a book, its images its pages - all but the reduced copies, the
     * thumbnails, a file may carry of them. The file is read in place, not loaded, and stays open
     * until {@link #close()}. Its methods are synchronized: libtiff reads one image at a time.
     */
    public static final class Book {
        private long handle;
        private final int pages;

        private Book(final long handle) {
            this.handle = handle;
            this.pages = nativePageCount(handle);
        }

        /** @return the book, or <code>null</code> when it is not a TIFF libtiff reads, or has no page */
        public static Book open(final String path) {
            if (!LOADED || path == null) {
                return null;
            }
            try {
                final long handle = nativeOpenFile(path.getBytes(StandardCharsets.UTF_8));
                return handle == 0 ? null : new Book(handle);
            } catch (Throwable e) {
                LOG.e(e, path);
                return null;
            }
        }

        public int getPageCount() {
            return pages;
        }

        /** The width and height of a page, read off its header alone; <code>null</code> if it cannot be. */
        public synchronized int[] size(final int page) {
            if (handle == 0 || page < 0 || page >= pages) {
                return null;
            }
            return nativePageSize(handle, page);
        }

        /** A page made smaller as it is decoded - see {@link NativeTiff#decode(byte[], int)}. */
        public synchronized Bitmap decode(final int page, final int sample) {
            final int[] size = size(page);
            if (size == null) {
                return null;
            }
            return NativeTiff.decode(size, sample, null, handle, page);
        }

        public synchronized void close() {
            if (handle != 0) {
                nativeClose(handle);
                handle = 0;
            }
        }
    }

    private static boolean load() {
        try {
            System.loadLibrary("tiff");
            return true;
        } catch (Throwable e) {
            LOG.e(e);
            return false;
        }
    }

    private static native int[] nativeSize(byte[] data);

    private static native boolean nativeDecode(byte[] data, int sample, Bitmap bitmap);

    private static native long nativeOpenFile(byte[] path);

    private static native int nativePageCount(long book);

    private static native int[] nativePageSize(long book, int page);

    private static native boolean nativeDecodePage(long book, int page, int sample, Bitmap bitmap);

    private static native void nativeClose(long book);
}
