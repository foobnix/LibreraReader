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
        for (FileMeta meta : list) {
            try {
                AppBook book = SharedBooks.load(meta.getPath());
                meta.setIsRecentProgress(book.p);
                if (updateTime) {
                    meta.setIsRecentTime(book.t);
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        AppDB.get().updateAll(list);
        long b = System.currentTimeMillis() - a;
        LOG.d("updateProgress-time:", list.size(), b / 1000.0);
    }

    public static Map<String, AppBook> cache = new ConcurrentHashMap<>();

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
        final Map<String, AppBook> newest = new java.util.HashMap<>();
        final Map<String, AppBook> ours = new java.util.HashMap<>();

        for (File file : AppProfile.getAllFiles(AppProfile.APP_PROGRESS_JSON)) {
            final boolean isThisDevice = file.equals(AppProfile.syncProgress);
            final LinkedJSONObject obj = IO.readJsonObject(file);
            for (String key : obj.keySet()) {
                final AppBook book = load(obj, key);
                if (TxtUtils.isEmpty(book.path)) {
                    continue;
                }
                final AppBook best = newest.get(key);
                if (best == null || book.t >= best.t) {
                    newest.put(key, book);
                }
                if (isThisDevice) {
                    ours.put(key, book);
                }
            }
        }

        for (Map.Entry<String, AppBook> entry : newest.entrySet()) {
            final AppBook best = entry.getValue();
            final AppBook own = ours.get(entry.getKey());
            final AppBook merged;
            if (own != null) {
                own.p = best.p;
                own.t = Math.max(best.t, own.t);
                merged = own;
            } else {
                merged = best;
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

    static int phash = -1;

    private static void save(AppBook bs, boolean inThread) {
        if (bs == null) {
            LOG.d("SharedBooks-Save", "null");
            return;
        }

        int hash = bs.hashCode();
        if (phash == hash) {
            LOG.d("SharedBooks-Save", "skip", hash);
            return;
        }
        phash = hash;
        LOG.d("SharedBooks-Save", "inThread " + inThread);


        if (TxtUtils.isEmpty(bs.path)) {
            LOG.d("Can't save AppBook");
            return;
        }

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
