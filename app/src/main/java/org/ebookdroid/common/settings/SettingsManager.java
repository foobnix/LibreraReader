package org.ebookdroid.common.settings;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppBook;
import com.foobnix.pdf.info.ExtUtils;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.emdev.utils.listeners.ListenerProxy;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsManager {

    static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile static AppBook current;
    static ListenerProxy listeners = new ListenerProxy(IBookSettingsChangeListener.class);


    public static AppBook getBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {

            current = SharedBooks.load(fileName);
            LOG.d("load-getBookSettings", current.p, fileName);
            return current;
        } catch (Exception e) {
            return current;
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * The open book's record, brought up to date with the files when the reader comes back to
     * the front - read aloud in the background, or read on another device and synced.
     *
     * {@link #getBookSettings(String)} replaces the record with whatever the files hold, and
     * that was wrong here: sync clears the cache every time the reader is paused, so the files
     * were read afresh on every unlock, and a page turned just before the lock - not written
     * yet, or written after the files were read - was thrown away and the reader turned back to
     * the page before it. The record on screen is kept unless the files hold a newer position.
     */
    public static AppBook refreshBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {
            final AppBook held = current;
            final AppBook loaded = SharedBooks.load(fileName);
            if (held != null && held != loaded && held.t > loaded.t
                    && ExtUtils.getFileName(held.path).equals(ExtUtils.getFileName(fileName))) {
                LOG.d("load-refreshBookSettings", "keep held", held.p, loaded.p);
                held.path = fileName;
                SharedBooks.cache.put(ExtUtils.getFileName(fileName), held);
                return held;
            }
            current = loaded;
            LOG.d("load-refreshBookSettings", current.p, fileName);
            return current;
        } catch (Exception e) {
            LOG.e(e);
            return current;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static AppBook getBookSettings() {
        lock.readLock().lock();
        try {
            // LOG.d("load current");
            return current;
        } finally {
            lock.readLock().unlock();
        }
    }




    public static void zoomChanged(final float zoom, final boolean committed) {
        lock.readLock().lock();
        try {
            if (current != null) {
                LOG.d("zoom-chaged", zoom);
                current.setZoom(zoom);
                SharedBooks.save(current);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void positionChanged(final float offsetX, final float offsetY) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.x = offsetX;
                current.y = offsetY;
            }
        } finally {
            lock.readLock().unlock();
        }
    }


    public static void addListener(final Object l) {
        listeners.addListener(l);
    }

    public static void removeListener(final Object l) {
        listeners.removeListener(l);
    }

}
