/*
 * BasicArrayCache
 *
 * Author: Lasse Collin <lasse.collin@tukaani.org>
 *
 * This file has been put into the public domain.
 * You can do whatever you want with this file.
 */

package org.tukaani.xz;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A basic {@link ArrayCache} implementation.
 * <p>
 * This caches exact array sizes, that is, {@code getByteArray} will return
 * an array whose size is exactly the requested size. A limited number
 * of different array sizes are cached at the same time; least recently used
 * sizes will be dropped from the cache if needed (can happen if several
 * different (de)compression options are used with the same cache).
 * <p>
 * The current implementation uses
 * {@link java.util.LinkedHashMap LinkedHashMap} to map different array sizes
 * to separate array-based data structures which hold
 * {@link java.lang.ref.SoftReference SoftReferences} to the cached arrays.
 * In the common case this should give good performance and fairly low
 * memory usage overhead.
 * <p>
 * A statically allocated global {@code BasicArrayCache} instance is
 * available via {@link #getInstance()} which is a good choice in most
 * situations where caching is wanted.
 *
 * @since 1.7
 */
public class BasicArrayCache extends ArrayCache {
    /**
     * Arrays smaller than this many elements will not be cached.
     */
    private static final int CACHEABLE_SIZE_MIN = 32 << 10;

    /**
     * Number of stacks i.e. how many different array sizes to cache.
     */
    private static final int STACKS_MAX = 32;

    /**
     * Number of arrays of the same type and size to keep in the cache.
     * (ELEMENTS_PER_STACK - 1) is used as a bit mask so ELEMENTS_PER_STACK
     * must be a power of two!
     */
    private static final int ELEMENTS_PER_STACK = 512;

    /**
     * A thread-safe stack-like data structure whose {@code push} method
     * overwrites the oldest element in the stack if the stack is full.
     */
    private static class CyclicStack<T> {
        /**
         * Array that holds the elements in the cyclic stack.
         */
        @SuppressWarnings("unchecked")
        private final T[] elements = (T[])new Object[ELEMENTS_PER_STACK];

        /**
         * Read-write position in the {@code refs} array.
         * The most recently added element is in {@code refs[pos]}.
         * If it is {@code null}, then the stack is empty and all
         * elements in {@code refs} are {@code null}.
         * <p>
         * Note that {@code pop()} always modifies {@code pos}, even if
         * the stack is empty. This means that when the first element is
         * added by {@code push(T)}, it can get added in any position in
         * {@code refs} and the stack will start growing from there.
         */
        private int pos = 0;

        /**
         * Gets the most recently added element from the stack.
         * If the stack is empty, {@code null} is returned.
         */
        public synchronized T pop() {
            T e = elements[pos];
            elements[pos] = null;
            pos = (pos - 1) & (ELEMENTS_PER_STACK - 1);
            return e;
        }

        /**
         * Adds a new element to the stack. If the stack is full, the oldest
         * element is overwritten.
         */
        public synchronized void push(T e) {
            pos = (pos + 1) & (ELEMENTS_PER_STACK - 1);
            elements[pos] = e;
        }
    }

    /**
     * Maps Integer (array size) to stacks of references to arrays. At most
     * STACKS_MAX number of stacks are kept in the map (LRU cache).
     */
    private static class CacheMap<T>
            extends LinkedHashMap<Integer, CyclicStack<Reference<T>>> {
        /**
         * This class won't be serialized but this is needed
         * to silence a compiler warning.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates a new CacheMap.
         */
        public CacheMap() {
            // The map may momentarily have at most STACKS_MAX + 1 entries
            // when put(K,V) has inserted a new entry but hasn't called
            // removeEldestEntry yet. Using 2 * STACKS_MAX as the initial
            // (and the final) capacity should give good performance. 0.75 is
            // the default load factor and in this case it guarantees that
            // the map will never need to be rehashed because
            // (STACKS_MAX + 1) / 0.75 < 2 * STACKS_MAX.
            //
            // That last argument is true to get LRU cache behavior together
            // with the overriden removeEldestEntry method.
            super(2 * STACKS_MAX, 0.75f, true);
        }

        /**
         * Returns true if the map is full and the least recently used stack
         * should be removed.
         */
        protected boolean removeEldestEntry(
                Map.Entry<Integer, CyclicStack<Reference<T>>> eldest) {
            return size() > STACKS_MAX;
        }
    }

    /**
     * Helper class for the singleton instance.
     * This is allocated only if {@code getInstance()} is called.
     */
    private static final class LazyHolder {
        static final BasicArrayCache INSTANCE = new BasicArrayCache();
    }

    /**
     * Returns a statically-allocated {@code BasicArrayCache} instance.
     * This is often a good choice when a cache is needed.
     */
    public static BasicArrayCache getInstance() {
        return LazyHolder.INSTANCE;
    }

    /**
     * Stacks for cached byte arrays.
     */
    private final CacheMap<byte[]> byteArrayCache = new CacheMap<byte[]>();

    /**
     * Stacks for cached int arrays.
     */
    private final CacheMap<int[]> intArrayCache = new CacheMap<int[]>();

    /**
     * Gets {@code T[size]} from the given {@code cache}.
     * If no such array is found, {@code null} is returned.
     */
    private static <T> T getArray(CacheMap<T> cache, int size) {
        // putArray doesn't add small arrays to the cache and so it's
        // pointless to look for small arrays here.
        if (size < CACHEABLE_SIZE_MIN)
            return null;

        // Try to find a stack that holds arrays of T[size].
        CyclicStack<Reference<T>> stack;
        synchronized(cache) {
            stack = cache.get(size);
        }

        if (stack == null)
            return null;

        // Try to find a non-cleared Reference from the stack.
        T array;
        do {
            Reference<T> r = stack.pop();
            if (r == null)
                return null;

            array = r.get();
        } while (array == null);

        return array;
    }

    /**
     * Puts the {@code array} of {@code size} elements long into
     * the {@code cache}.
     */
    private static <T> void putArray(CacheMap<T> cache, T array, int size) {
        // Small arrays aren't cached.
        if (size < CACHEABLE_SIZE_MIN)
            return;

        CyclicStack<Reference<T>> stack;

        synchronized(cache) {
            // Get a stack that holds arrays of T[size]. If no such stack
            // exists, allocate a new one. If the cache already had STACKS_MAX
            // number of stacks, the least recently used stack is removed by
            // cache.put (it calls removeEldestEntry).
            stack = cache.get(size);
            if (stack == null) {
                stack = new CyclicStack<Reference<T>>();
                cache.put(size, stack);
            }
        }

        stack.push(new SoftReference<T>(array));
    }

    /**
     * Allocates a new byte array, hopefully reusing an existing
     * array from the cache.
     *
     * @param       size        size of the array to allocate
     *
     * @param       fillWithZeros
     *                          if true, all the elements of the returned
     *                          array will be zero; if false, the contents
     *                          of the returned array is undefined
     */
    public byte[] getByteArray(int size, boolean fillWithZeros) {
        byte[] array = getArray(byteArrayCache, size);

        if (array == null)
            array = new byte[size];
        else if (fillWithZeros)
            Arrays.fill(array, (byte)0x00);

        return array;
    }

    /**
     * Puts the given byte array to the cache. The caller must no longer
     * use the array.
     * <p>
     * Small arrays aren't cached and will be ignored by this method.
     */
    public void putArray(byte[] array) {
        putArray(byteArrayCache, array, array.length);
    }

    /**
     * This is like getByteArray but for int arrays.
     */
    public int[] getIntArray(int size, boolean fillWithZeros) {
        int[] array = getArray(intArrayCache, size);

        if (array == null)
            array = new int[size];
        else if (fillWithZeros)
            Arrays.fill(array, 0);

        return array;
    }

    /**
     * Puts the given int array to the cache. The caller must no longer
     * use the array.
     * <p>
     * Small arrays aren't cached and will be ignored by this method.
     */
    public void putArray(int[] array) {
        putArray(intArrayCache, array, array.length);
    }
}
