/*
 * ResettableArrayCache
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz;

import java.util.ArrayList;
import java.util.List;

/**
 * An ArrayCache wrapper that remembers what has been allocated
 * and allows returning all allocations to the underlying cache at once.
 *
 * @since 1.7
 */
public class ResettableArrayCache extends ArrayCache {
    private final ArrayCache arrayCache;

    // Lists of arrays that have been allocated from the arrayCache.
    private final List<byte[]> byteArrays;
    private final List<int[]> intArrays;

    /**
     * Creates a new ResettableArrayCache based on the given ArrayCache.
     */
    public ResettableArrayCache(ArrayCache arrayCache) {
        this.arrayCache = arrayCache;

        // Treat the dummy cache as a special case since it's a common case.
        // With it we don't need to put the arrays back to the cache and
        // thus we don't need to remember what has been allocated.
        if (arrayCache == ArrayCache.getDummyCache()) {
            byteArrays = null;
            intArrays = null;
        } else {
            byteArrays = new ArrayList<byte[]>();
            intArrays = new ArrayList<int[]>();
        }
    }

    public byte[] getByteArray(int size, boolean fillWithZeros) {
        byte[] array = arrayCache.getByteArray(size, fillWithZeros);

        if (byteArrays != null) {
            synchronized(byteArrays) {
                byteArrays.add(array);
            }
        }

        return array;
    }

    public void putArray(byte[] array) {
        if (byteArrays != null) {
            // The array is more likely to be near the end of the list so
            // start the search from the end.
            synchronized(byteArrays) {
                int i = byteArrays.lastIndexOf(array);
                if (i != -1)
                    byteArrays.remove(i);
            }

            arrayCache.putArray(array);
        }
    }

    public int[] getIntArray(int size, boolean fillWithZeros) {
        int[] array = arrayCache.getIntArray(size, fillWithZeros);

        if (intArrays != null) {
            synchronized(intArrays) {
                intArrays.add(array);
            }
        }

        return array;
    }

    public void putArray(int[] array) {
        if (intArrays != null) {
            synchronized(intArrays) {
                int i = intArrays.lastIndexOf(array);
                if (i != -1)
                    intArrays.remove(i);
            }

            arrayCache.putArray(array);
        }
    }

    /**
     * Puts all allocated arrays back to the underlying ArrayCache
     * that haven't already been put there with a call to
     * {@code putArray}.
     */
    public void reset() {
        if (byteArrays != null) {
            // Put the arrays to the cache in reverse order: the array that
            // was allocated first is returned last.
            synchronized(byteArrays) {
                for (int i = byteArrays.size() - 1; i >= 0; --i)
                    arrayCache.putArray(byteArrays.get(i));

                byteArrays.clear();
            }

            synchronized(intArrays) {
                for (int i = intArrays.size() - 1; i >= 0; --i)
                    arrayCache.putArray(intArrays.get(i));

                intArrays.clear();
            }
        }
    }
}
