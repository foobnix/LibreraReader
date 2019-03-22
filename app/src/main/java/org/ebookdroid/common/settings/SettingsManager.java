package org.ebookdroid.common.settings;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppState;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.emdev.utils.listeners.ListenerProxy;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsManager {

    static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile static AppBook current;
    private volatile static String currentPath;
    static ListenerProxy listeners = new ListenerProxy(IBookSettingsChangeListener.class);


    public static void clearCache() {
        current = null;
    }

    public static AppBook getBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {
            if (currentPath != null && current != null && fileName.equals(currentPath)) {
                return current;
            }
            currentPath = fileName;
            LOG.d("load", fileName);
            current = SharedBooks.load(fileName);
            return current;
        } catch (Exception e) {
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


    public static boolean toggleCropMode(boolean isCrop) {
        if (current == null) {
            return false;
        }
        lock.writeLock().lock();
        try {
            if (current != null) {
                current.cp = isCrop;
                SharedBooks.save(current);
            }
        } finally {
            lock.writeLock().unlock();
        }

        return current.cp;
    }


    public static void zoomChanged(final float zoom, final boolean committed) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.setZoom(zoom);
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

    public static void storeBookSettings1() {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.s = AppState.get().autoScrollSpeed;
                SharedBooks.save(current);
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
