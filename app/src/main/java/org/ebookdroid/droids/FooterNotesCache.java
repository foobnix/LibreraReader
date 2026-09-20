package org.ebookdroid.droids;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.JsonHelper;
import com.foobnix.sys.TempHolder;

import java.io.File;
import java.util.Map;

/**
 * The extracted footnotes of a book, kept as JSON next to its cache file. Extracting them from a
 * big EPUB takes seconds, so the map is cached, and the cache is written off the thread that is
 * opening the book.
 */
final class FooterNotesCache {

    private FooterNotesCache() {
    }

    static File fileFor(File cacheFile) {
        return new File(cacheFile + ".json");
    }

    /**
     * Writes the map, unless the loading was cancelled or another thread got there first: a
     * cancelled extraction is empty or partial and must not stay in the cache. Returns whether
     * this call is the one that wrote the file.
     */
    static boolean save(File jsonFile, Map<String, String> notes) {
        if (notes == null || TempHolder.get().loadingCancelled.get()) {
            return false;
        }
        if (!JsonHelper.mapToCacheFile(jsonFile, notes)) {
            return false;
        }
        if (TempHolder.get().loadingCancelled.get()) {
            // cancelled while it was being written
            jsonFile.delete();
            return false;
        }
        LOG.d("save notes to file", jsonFile);
        return true;
    }

    /** The same, on a thread of its own: the open must not wait for the disk. */
    static void saveAsync(final File jsonFile, final Map<String, String> notes) {
        Thread save = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
                } catch (Throwable e) {
                    // the priority is a wish, not a requirement
                }
                try {
                    save(jsonFile, notes);
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        }, "@T save footnotes");
        save.setPriority(Thread.MIN_PRIORITY);
        save.start();
    }
}
