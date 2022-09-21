package com.foobnix.pdf.info;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;

import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BookmarksData {
    private static final BookmarksData instance = new BookmarksData();

    public static BookmarksData get() {
        return instance;
    }

    public void add(AppBookmark bookmark) {
        LOG.d("BookmarksData", "add", bookmark.p, bookmark.text, bookmark.path);


        if (bookmark.p > 1) {
            bookmark.p = 0;
        }
        try {
            LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncBookmarks);
            obj.put("" + bookmark.t, Objects.toJSONObject(bookmark));
            IO.writeObjAsync(AppProfile.syncBookmarks, obj);
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    public void remove(AppBookmark bookmark) {
        LOG.d("BookmarksData", "remove", bookmark.t, bookmark.file);

        try {
            LinkedJSONObject obj = IO.readJsonObject(bookmark.file);
            if (obj.has("" + bookmark.t)) {
                obj.remove("" + bookmark.t);
            }
            IO.writeObjAsync(bookmark.file, obj);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public List<AppBookmark> getBookmarksByBook(File file) {
        if (file == null) {
            return new ArrayList<>();
        }
        return getBookmarksByBook(file.getPath());
    }

    public synchronized List<AppBookmark> getAll(Context c) {
        final List<AppBookmark> all = getAll();
        final Iterator<AppBookmark> iterator = all.iterator();
        String fast = c.getString(R.string.fast_bookmark);
        while (iterator.hasNext()) {
            final AppBookmark next = iterator.next();

            if (AppState.get().isShowOnlyAvailabeBooks) {
                if (!new File(next.getPath()).isFile()) {
                    iterator.remove();
                    continue;
                }
            }

            if (!AppState.get().isShowFastBookmarks) {
                if (fast.equals(next.text)) {
                    iterator.remove();
                }
            }
        }
        return all;
    }

    public List<AppBookmark> getAll() {
        List<AppBookmark> all = new ArrayList<>();

        try {
            List<File> allFiles = AppProfile.getAllFiles(AppProfile.APP_BOOKMARKS_JSON);
            for (File file : allFiles) {
                LinkedJSONObject obj = IO.readJsonObject(file);

                final Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    final String next = keys.next();

                    AppBookmark appBookmark = new AppBookmark();
                    appBookmark.file = file;
                    final LinkedJSONObject local = obj.getJSONObject(next);
                    Objects.loadFromJson(appBookmark, local);
                    all.add(appBookmark);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        LOG.d("getAll-size", all.size());
        all.sort(Comparator.comparingDouble(AppBookmark::getTime).reversed());
        return all;
    }

    public List<AppBookmark> getBookmarksByBook(String path) {
        List<AppBookmark> all = new ArrayList<>();
        List<File> allFiles = AppProfile.getAllFiles(AppProfile.APP_BOOKMARKS_JSON);

        for (File file : allFiles) {
            LinkedJSONObject obj = IO.readJsonObject(file);
            try {
                final Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    final String next = keys.next();

                    AppBookmark appBookmark = new AppBookmark();
                    appBookmark.file = file;
                    final LinkedJSONObject local = obj.getJSONObject(next);
                    Objects.loadFromJson(appBookmark, local);
                    String path1 = ExtUtils.getFileName(path);
                    String path2 = ExtUtils.getFileName(appBookmark.getPath());
                    if (path1.equals(path2)) {
                        appBookmark.path = path;//update path
                        all.add(appBookmark);
                    }
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }


        LOG.d("getBookmarksByBook", path, all.size());
        all.sort(Comparator.comparingDouble(AppBookmark::getPercent));
        return all;
    }

    public boolean hasBookmark(String lastBookPath, int page, int pages) {
        final List<AppBookmark> bookmarksByBook = getBookmarksByBook(lastBookPath);
        for (AppBookmark appBookmark : bookmarksByBook) {
            if (appBookmark.getPercent() * pages == page) {
                return true;
            }
        }
        return false;
    }

    public Map<String, List<AppBookmark>> getBookmarksMap() {
        return null;
    }

    public void cleanBookmarks() {
        AppData.get().clearAll(AppProfile.APP_BOOKMARKS_JSON);
    }
}
