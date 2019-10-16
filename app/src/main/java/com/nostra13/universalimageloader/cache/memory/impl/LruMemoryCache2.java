package com.nostra13.universalimageloader.cache.memory.impl;

import android.graphics.Bitmap;

import androidx.collection.LruCache;

import com.nostra13.universalimageloader.cache.memory.MemoryCache;

import java.util.Collection;
import java.util.Collections;

/**
 * A cache that holds strong references to a limited number of Bitmaps. Each time a Bitmap is accessed, it is moved to
 * the head of a queue. When a Bitmap is added to a full cache, the Bitmap at the end of that queue is evicted and may
 * become eligible for garbage collection.<br />
 * <br />
 * <b>NOTE:</b> This cache uses only strong references for stored Bitmaps.
 *
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 * @since 1.8.1
 */
public class LruMemoryCache2 implements MemoryCache {


    final LruCache<String, Bitmap> cache;

    public LruMemoryCache2(int size) {
        cache = new LruCache<String, Bitmap>(size);
    }

    @Override
    public boolean put(String key, Bitmap value) {
        final Bitmap bitmap = cache.get(key);
        if (bitmap == null || bitmap.isRecycled()) {
            cache.put(key, value);
        }
        return true;
    }

    @Override
    public Bitmap get(String key) {
        //LOG.d("LruMemoryCache2 get", key);
        return cache.get(key);
    }

    @Override
    public Bitmap remove(String key) {
        //LOG.d("LruMemoryCache2 remove", key);
        return cache.remove(key);
    }

    @Override
    public Collection<String> keys() {
        return Collections.emptyList();
    }

    @Override
    public void clear() {
        //LOG.d("LruMemoryCache2 clear all");
        cache.evictAll();
    }
}