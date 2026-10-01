package org.ebookdroid.common.settings;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppBook;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.common.settings.listeners.IBookSettingsChangeListener;
import org.emdev.utils.listeners.ListenerProxy;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SettingsManager {

    public static AppBook getBookSettings(android.content.Intent intent, String localPath) {
        String identity = com.foobnix.pdf.info.ExtUtils.recentPathFromIntent(intent, localPath);
        lock.writeLock().lock();
        try {
            AppBook book = getBookSettings(identity);
            // Earlier builds persisted reading state under the staged filename. Seed only
            // an unused canonical entry; never replace saved canonical state, including 0%.
            if (book != null && !identity.equals(localPath)
                    && !com.foobnix.pdf.info.ArchiveMemberIdentity.isIdentity(identity)
                    && book.t == 0 && book.p == 0) {
                AppBook legacy = SharedBooks.load(localPath);
                if (legacy.t > 0 || legacy.p > 0) {
                    com.foobnix.android.utils.Objects.loadFromJson(book,
                            com.foobnix.android.utils.Objects.toJSONObject(legacy));
                    SharedBooks.cache.put(com.foobnix.pdf.info.ExtUtils.getFileName(identity), book);
                }
            }
            return book;
        } finally {
            lock.writeLock().unlock();
        }
    }

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
