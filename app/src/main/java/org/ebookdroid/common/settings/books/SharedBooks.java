package org.ebookdroid.common.settings.books;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;

import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SharedBooks {


    public static void updateProgress(List<FileMeta> list1, boolean updateTime, int limit) {
        // The limit is kept in the signature for callers that still pass one, but every book
        // is refreshed now: the files behind it are read once for the whole list rather than
        // once per book, so the cut that used to pay for itself no longer buys anything - and
        // a book below it was left showing whatever the database happened to hold.
        final List<FileMeta> list = list1;

        long a = System.currentTimeMillis();
        preloadAll();

        // Only the books whose bar has actually moved are written back: the whole shelf was
        // being rewritten row by row on every refresh, for a handful of changes at most.
        final List<FileMeta> changed = new ArrayList<>();
        for (FileMeta meta : list) {
            try {
                AppBook book = SharedBooks.load(meta.getPath());
                final Float was = meta.getIsRecentProgress();
                final Long wasTime = meta.getIsRecentTime();
                meta.setIsRecentProgress(book.p);
                if (updateTime) {
                    meta.setIsRecentTime(book.t);
                }
                if (was == null || was != book.p || (updateTime && (wasTime == null || wasTime != book.t))) {
                    changed.add(meta);
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        AppDB.get().updateAll(changed);
        long b = System.currentTimeMillis() - a;
        LOG.d("updateProgress-time:", list.size(), changed.size(), b / 1000.0);
    }

    public static Map<String, AppBook> cache = new ConcurrentHashMap<>();

    /**
     * How far ahead of this device's clock another device's timestamp may be before it is
     * treated as wrong. Devices merge by "whose record is newest", so one with its clock set
     * years forward would otherwise win every book for good, and no amount of reading here
     * would ever take a position back.
     */
    public static final long CLOCK_TOLERANCE = 24L * 60 * 60 * 1000;

    /**
     * Which of a book's records across devices carries the position: the newest of them,
     * passing over any stamped further ahead than {@link #CLOCK_TOLERANCE}. Returns -1 when
     * there is nothing to choose from - every record was in the future, or there were none.
     */
    public static int newestRecord(long[] times, long now) {
        int newest = -1;
        for (int i = 0; i < times.length; i++) {
            if (times[i] > now + CLOCK_TOLERANCE) {
                continue;
            }
            if (newest == -1 || times[i] >= times[newest]) {
                newest = i;
            }
        }
        return newest;
    }

    /** The content last written for each book, so an unchanged book is not written again. */
    public static final Map<String, Integer> written = new ConcurrentHashMap<>();

    /**
     * Whether this book has changed since it was last written. One book's write used to be
     * skipped because a different book had just been written with the same content: the last
     * hash was held in a single slot shared by every book, rather than one apiece.
     */
    public static boolean hasChanged(String key, int hash) {
        final Integer previous = written.get(key);
        if (previous != null && previous == hash) {
            return false;
        }
        written.put(key, hash);
        return true;
    }

    /** When each progress file was last read, so an unchanged one is not read again. */
    private static final Map<String, Long> lastRead = new ConcurrentHashMap<>();

    /**
     * Reads every device's progress file once and merges them into the cache.
     *
     * {@link #load(String)} opens and parses all of them for a single book, so refreshing a
     * list of books cost books x devices file reads - which is why callers only ever refreshed
     * the first few, and why a book below that cut kept whatever the database had. Read the
     * other way round the whole library costs one pass over each file.
     *
     * The merge is the same rule {@link #load(String)} applies: the record with the newest
     * time carries the position, and this device's own record carries it if it has one.
     */
    public static void preloadAll() {
        preloadAll(false);
    }

    public static void preloadAll(boolean force) {
        final List<File> files = AppProfile.getAllFiles(AppProfile.APP_PROGRESS_JSON);

        // Nothing has been written since the last pass, so the answer is the one already held.
        if (!force && !hasChangedOnDisk(files)) {
            LOG.d("SharedBooks-preloadAll", "unchanged", cache.size());
            return;
        }

        final Map<String, List<AppBook>> records = new HashMap<>();
        final Map<String, AppBook> ours = new HashMap<>();

        for (File file : files) {
            final boolean isThisDevice = file.equals(AppProfile.syncProgress);
            final LinkedJSONObject obj = IO.readJsonObject(file);
            for (String key : obj.keySet()) {
                final AppBook book = load(obj, key);
                if (TxtUtils.isEmpty(book.path)) {
                    continue;
                }
                List<AppBook> forKey = records.get(key);
                if (forKey == null) {
                    forKey = new ArrayList<>();
                    records.put(key, forKey);
                }
                forKey.add(book);
                if (isThisDevice) {
                    ours.put(key, book);
                }
            }
            lastRead.put(file.getPath(), file.lastModified());
        }

        for (Map.Entry<String, List<AppBook>> entry : records.entrySet()) {
            final AppBook merged = merge(entry.getValue(), ours.get(entry.getKey()));
            if (merged == null) {
                continue;
            }

            // A book just closed is written to its file on a background thread, so a pass over
            // the files can still be reading the page before it. What is already in hand wins
            // if it is the newer of the two, or the reader would watch the page they just left
            // turn back into the one before it.
            final AppBook held = cache.get(entry.getKey());
            if (held != null && held.t > merged.t) {
                continue;
            }
            cache.put(entry.getKey(), merged);
        }
        LOG.d("SharedBooks-preloadAll", cache.size());
    }

    /**
     * The record that carries the position, out of every device's record of one book. This
     * device's own record is the one handed back where it has one, so its zoom, crop and lock
     * are kept, but the position and time come from whichever record is newest.
     */
    private static AppBook merge(List<AppBook> forKey, AppBook own) {
        final long[] times = new long[forKey.size()];
        for (int i = 0; i < times.length; i++) {
            times[i] = forKey.get(i).t;
        }
        final int newest = newestRecord(times, System.currentTimeMillis());
        if (newest == -1) {
            return null;
        }
        final AppBook best = forKey.get(newest);
        if (own == null) {
            return best;
        }
        own.p = best.p;
        own.t = Math.max(best.t, own.t);
        return own;
    }

    private static boolean hasChangedOnDisk(List<File> files) {
        if (lastRead.size() != files.size()) {
            return true;
        }
        for (File file : files) {
            final Long seen = lastRead.get(file.getPath());
            if (seen == null || seen != file.lastModified()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes one book's progress into the row the lists are drawn from. The files are what the
     * devices agree on, the row is only what the screen reads, and nothing refreshed the row
     * for a book outside the recent and favourite lists - so a book read and closed kept its
     * old bar in the library until the whole shelf was scanned again.
     */
    public static void syncToDb(String path) {
        if (TxtUtils.isEmpty(path)) {
            return;
        }
        try {
            final AppBook book = load(path);
            final FileMeta meta = AppDB.get().getOrCreate(path);
            meta.setIsRecentProgress(book.p);
            AppDB.get().update(meta);
            LOG.d("SharedBooks-syncToDb", path, book.p);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void deleteProgress(String path) {
        cache.clear();
        for (File fileName : AppProfile.getAllFiles(AppProfile.APP_PROGRESS_JSON)) {
            LinkedJSONObject linkedJsonObject = IO.readJsonObject(fileName);
            String key = ExtUtils.getFileName(path);
            if (linkedJsonObject.has(key)) {
                linkedJsonObject.remove(key);
                IO.writeObjSync(fileName, linkedJsonObject);
                LOG.d("deleteProgress", path);
            }
        }
    }

    public static AppBook load(String fileName) {
        LOG.d("SharedBooks-load", fileName);

        // Keyed by the name the progress files themselves are keyed by, not by the full path.
        // Saving keys by that name, so a cache keyed by path was never the one a save updated:
        // a book closed at a new page went on being read back at the old one until something
        // else cleared the cache.
        final String key = ExtUtils.getFileName(fileName);
        AppBook cached = cache.get(key);
        if (cached != null) {
            LOG.d("SharedBooks-load-from-cache", fileName);
            // The record is shared by every file of that name; the path is whose it is now.
            cached.path = fileName;
            return cached;
        }

        AppBook res = new AppBook(fileName);
        AppBook original = null;

        for (File file : AppProfile.getAllFiles(AppProfile.APP_PROGRESS_JSON)) {
            final AppBook load = load(IO.readJsonObject(file), fileName);
            if (TxtUtils.isEmpty(load.path)) {
                continue;
            }
            load.path = fileName;

            if (file.equals(AppProfile.syncProgress)) {
                original = load;
            }

            if (load.t >= res.t) {
                res = load;
            }
        }
        if (original != null) {
            original.p = res.p;
            original.t = Math.max(res.t, original.t);
            LOG.d("SharedBooks-load1 original", fileName, res.p);
            cache.put(key, original);
            return original;
        }

        LOG.d("SharedBooks-load1 general", fileName, res.p);
        cache.put(key, res);
        return res;

    }

    private static AppBook load(LinkedJSONObject obj, String fileName) {
        AppBook bs = new AppBook(fileName);
        try {

            LOG.d("SharedBooks-load", bs.path);
            final String key = ExtUtils.getFileName(fileName);
            if (!obj.has(key)) {
                return bs;
            }
            final LinkedJSONObject rootObj = obj.getJSONObject(key);
            Objects.loadFromJson(bs, rootObj);
        } catch (Exception e) {
            LOG.e(e);
        }
        return bs;
    }

    public static void save(AppBook bs) {
        save(bs, true);
    }

    public static void saveAsync(AppBook bs) {
        save(bs, false);
    }

    private static void save(AppBook bs, boolean inThread) {
        if (bs == null) {
            LOG.d("SharedBooks-Save", "null");
            return;
        }

        if (TxtUtils.isEmpty(bs.path)) {
            LOG.d("Can't save AppBook");
            return;
        }

        if (!hasChanged(ExtUtils.getFileName(bs.path), bs.hashCode())) {
            LOG.d("SharedBooks-Save", "skip", bs.path);
            return;
        }
        LOG.d("SharedBooks-Save", "inThread " + inThread);

        try {
            final LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncProgress);

            if (bs.p > 1 || bs.p < 0) {
                bs.p = 0;
            }

            final String fileName = ExtUtils.getFileName(bs.path);
            final LinkedJSONObject value = Objects.toJSONObject(bs);
            obj.put(fileName, value);
            cache.put(fileName, bs);

            LOG.d("SharedBooks-Save", value);


            // Our own file is about to change, so the next pass over the files has to read it
            // again rather than trust what it read last time.
            lastRead.remove(AppProfile.syncProgress.getPath());

            if (inThread) {
                IO.writeObj(AppProfile.syncProgress, obj);
            } else {
                IO.writeObjSync(AppProfile.syncProgress, obj);
            }
        } catch (Exception e) {
            LOG.e(e);
        }


    }


}
