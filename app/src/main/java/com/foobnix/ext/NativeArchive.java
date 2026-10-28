package com.foobnix.ext;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.foobnix.android.utils.LOG;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A comic archive - a .cbz (ZIP) or a .cbr (RAR 1.5-4.x or RAR5) - read with libarchive, in place:
 * nothing is extracted or copied. See Builder/jni/librera_archive.c, built into libarchive.so,
 * which every flavour carries - with MuPDF or without.
 *
 * Every build opens a comic in the comic engine, which reads its pages through this, and takes
 * the library's covers, page counts and ComicInfo.xml from it, so a .cbz and a .cbr are read the
 * same way everywhere.
 *
 * Entries are reached in the order they are stored: reading them in that order is cheap, an
 * entry before the last one read starts the archive again from the top. One instance is not
 * meant for two threads at once, and its methods are synchronized for that.
 */
public final class NativeArchive {

    /** libarchive.so, or <code>false</code> when it would not load and no archive opens. */
    private static final boolean LOADED = load();

    private long handle;
    private final String path;
    private final List<String> names;
    private final long[] sizes;

    private NativeArchive(final long handle, final String path, final List<String> names, final long[] sizes) {
        this.handle = handle;
        this.path = path;
        this.names = names;
        this.sizes = sizes;
    }

    /**
     * Opens an archive and reads its list of entries.
     *
     * @return the archive, or <code>null</code> when it is not one libarchive reads here, is
     *         broken, or the native library of this build has no archive reader
     */
    public static NativeArchive open(final String path) {
        if (path == null || !LOADED) {
            return null;
        }
        try {
            return listed(nativeOpen(path.getBytes(StandardCharsets.UTF_8)), path);
        } catch (Throwable e) {
            LOG.e(e, path);
            return null;
        }
    }

    /**
     * Opens a document on added storage (a content:// uri) in place, through the descriptor its
     * provider hands over: nothing is copied out.
     *
     * @return the archive, or <code>null</code> as {@link #open(String)}, and also when the
     *         provider's descriptor cannot seek (a stream it makes on the fly)
     */
    public static NativeArchive open(final Context c, final String uri) {
        if (c == null || uri == null || !LOADED) {
            return null;
        }
        ParcelFileDescriptor pfd = null;
        try {
            pfd = c.getContentResolver().openFileDescriptor(Uri.parse(uri), "r");
            if (pfd == null) {
                return null;
            }
            // the native side keeps a duplicate of the descriptor, this one is closed below
            return listed(nativeOpenFd(pfd.getFd()), uri);
        } catch (Throwable e) {
            LOG.e(e, uri);
            return null;
        } finally {
            if (pfd != null) {
                try {
                    pfd.close();
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        }
    }

    private static NativeArchive listed(final long handle, final String path) {
        if (handle == 0) {
            LOG.d("NativeArchive cannot open", path);
            return null;
        }
        try {
            final byte[][] raw = nativeNames(handle);
            final long[] sizes = nativeSizes(handle);
            if (raw == null || sizes == null) {
                nativeClose(handle);
                return null;
            }
            final List<String> names = new ArrayList<String>(raw.length);
            for (final byte[] name : raw) {
                // A name the archive did not store as UTF-8 decodes to something unreadable, but
                // still the same something each time, which is all a page needs of it
                names.add(name == null ? null : new String(name, StandardCharsets.UTF_8));
            }
            return new NativeArchive(handle, path, Collections.unmodifiableList(names), sizes);
        } catch (Throwable e) {
            LOG.e(e, path);
            nativeClose(handle);
            return null;
        }
    }

    private static boolean load() {
        try {
            System.loadLibrary("archive");
            return true;
        } catch (Throwable e) {
            LOG.e(e);
            return false;
        }
    }

    public String getPath() {
        return path;
    }

    /**
     * The entries, in the order they are stored; <code>null</code> stands for anything that is
     * not a file - a directory, a link.
     */
    public List<String> getNames() {
        return names;
    }

    /** The entry's size as its header gives it, or -1 where it does not. */
    public long getSize(final int index) {
        return sizes[index];
    }

    /** The index of the entry of that name, or -1. */
    public int indexOf(final String name) {
        return name == null ? -1 : names.indexOf(name);
    }

    /**
     * Reads one entry whole.
     *
     * @return its bytes, or <code>null</code> when it cannot be read (encrypted, broken) or the
     *         archive is closed
     */
    public synchronized byte[] read(final int index) {
        if (handle == 0 || index < 0 || index >= names.size()) {
            return null;
        }
        try {
            return nativeRead(handle, index);
        } catch (Throwable e) {
            LOG.e(e, path, index);
            return null;
        }
    }

    public synchronized void close() {
        if (handle != 0) {
            nativeClose(handle);
            handle = 0;
        }
    }

    private static native long nativeOpen(byte[] path);

    private static native long nativeOpenFd(int fd);

    private static native byte[][] nativeNames(long handle);

    private static native long[] nativeSizes(long handle);

    private static native byte[] nativeRead(long handle, int index);

    private static native void nativeClose(long handle);
}
