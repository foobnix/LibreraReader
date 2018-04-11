package org.ebookdroid.common.settings;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.common.settings.books.SharedDB;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.ebookdroid.core.PageIndex;
import org.emdev.utils.listeners.ListenerProxy;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.wrapper.AppState;

import android.content.Context;

public class SettingsManager {

    static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public static SharedDB db;
    private volatile static BookSettings current;
    private volatile static String currentPath;
    static ListenerProxy listeners = new ListenerProxy(IBookSettingsChangeListener.class);

    public static void init(final Context context) {
        db = new SharedDB(context);
    }

    public static void clearCache() {
        current = null;
    }

    public static BookSettings getBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {
            if (currentPath != null && current != null && fileName.equals(currentPath)) {
                return current;
            }
            currentPath = fileName;
            LOG.d("getBookSettings", fileName);
            current = db.getBookSettings(fileName);
            return current;
        } catch (Exception e) {
            return current;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void updateTempPage(String fileName, int pageNumber) {
        try {
            BookSettings bookSettings = getTempBookSettings(fileName);
            bookSettings.currentPage = new PageIndex(pageNumber, pageNumber);
            db.storeBookSettings(bookSettings);
            LOG.d("updateTempPage", fileName, pageNumber);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static BookSettings getTempBookSettings(final String fileName) {
        lock.writeLock().lock();
        try {
            LOG.d("getTempBookSettings", fileName);
            return db.getBookSettings(fileName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void clearCurrentBookSettings2() {
        lock.writeLock().lock();
        try {
            storeBookSettings1();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void removeCurrentBookSettings() {
        lock.writeLock().lock();
        try {
            db.delete(current);
            current = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static void deleteBookSettings(final BookSettings bs) {
        lock.writeLock().lock();
        try {
            db.delete(bs);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static BookSettings getBookSettings() {
        lock.readLock().lock();
        try {
            LOG.d("getBookSettings current");
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
                current.cropPages = isCrop;
                db.storeBookSettings(current);
            }
        } finally {
            lock.writeLock().unlock();
        }

        return current.cropPages;
    }

    public static void currentPageChanged(final PageIndex newIndex, int pages) {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.currentPageChanged(newIndex, pages);
            }
        } finally {
            lock.readLock().unlock();
        }
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
                current.offsetX = offsetX;
                current.offsetY = offsetY;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void storeBookSettings1() {
        lock.readLock().lock();
        try {
            if (current != null) {
                current.speed = AppState.get().autoScrollSpeed;
                db.storeBookSettings(current);
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
