package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppData;
import com.foobnix.pdf.info.wrapper.AppBookmark;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BookmarksData {

    public static File BOOKMARKS_ROOT = new File(AppsConfig.SYNC_FOLDER, "bookmarks");

    public static File getCacheFile(String path) {
        BOOKMARKS_ROOT.mkdirs();

        if (path.startsWith(BOOKMARKS_ROOT.getPath())) {
            return new File(path);
        }

        return new File(BOOKMARKS_ROOT, ExtUtils.getFileName(path) + ".json");
    }


    final static BookmarksData instance = new BookmarksData();

    public static BookmarksData get() {
        return instance;
    }


    public void add(AppBookmark bookmark) {
        LOG.d("BookmarksData", "add", bookmark.p, bookmark.text);
        List<AppBookmark> res = getBookmarksByBook(bookmark.path);
        res.add(bookmark);
        AppData.writeSimpleMeta(res, getCacheFile(bookmark.path));

    }

    public void remove(AppBookmark bookmark) {
        LOG.d("BookmarksData", "remove", bookmark.p, bookmark.text);
        List<AppBookmark> res = getBookmarksByBook(bookmark.path);
        res.remove(bookmark);
        AppData.writeSimpleMeta(res, getCacheFile(bookmark.path));
    }

    public boolean hasBookmark(String path, int page) {

        return false;

    }

    public List<AppBookmark> getBookmarksByBook(File file) {
        return getBookmarksByBook(file.getPath());
    }

    public List<AppBookmark> getAll() {
        List<AppBookmark> all = new ArrayList<>();
        File[] files = BOOKMARKS_ROOT.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".json");
            }
        });
        for (File file : files) {
            LOG.d("getAll-path", file.getPath());
            all.addAll(getBookmarksByBook(file));
        }
        LOG.d("getAll-size", all.size());
        return all;
    }


    public List<AppBookmark> getBookmarksByBook(String path) {
        List<AppBookmark> res = new ArrayList<AppBookmark>();
        AppData.readSimpleMeta(res, getCacheFile(path), AppBookmark.class);
        LOG.d("getBookmarksByBook", path, res.size());
        Collections.sort(res, BY_PERCENT);
        return res;

    }

    static final Comparator<AppBookmark> BY_PERCENT = new Comparator<AppBookmark>() {

        @Override
        public int compare(AppBookmark o1, AppBookmark o2) {
            return Float.compare(o1.getPercent(), o2.getPercent());
        }
    };


    public Map<String, List<AppBookmark>> getBookmarksMap() {
        return null;
    }


    public void cleanBookmarks() {

    }


}
