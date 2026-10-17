package com.foobnix.pdf.info;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppAnnotation;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.presentation.BookmarksAdapter;

import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class BookmarksData {


    final static BookmarksData instance = new BookmarksData();

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
            IO.writeObjSync(AppProfile.syncBookmarks, obj);
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    public void remove(AppBookmark bookmark) {
        LOG.d("BookmarksData", "remove", bookmark.t, bookmark.file);

        // A mark shown beside the bookmarks is still a mark: it is taken out of the marks, not
        // looked for among the bookmarks, where it was never written.
        if (bookmark instanceof BookmarksAdapter.Mark) {
            AnnotationsData.get()
                           .remove(((BookmarksAdapter.Mark) bookmark).mark);
            return;
        }

        try {
            LinkedJSONObject obj = IO.readJsonObject(bookmark.file);
            if (obj.has("" + bookmark.t)) {
                obj.remove("" + bookmark.t);
            }
            IO.writeObjSync(bookmark.file, obj);
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
        LOG.d("AppBookmark-get","getAll");

        final List<AppBookmark> all = getAll();
        final Iterator<AppBookmark> iterator = all.iterator();
        String fast = c.getString(R.string.fast_bookmark);
        while (iterator.hasNext()) {
            final AppBookmark next = iterator.next();

            if (AppState.get().isShowOnlyAvailabeBooks && !isBookAvailable(next.getPath())) {
                iterator.remove();
                continue;
            }

            if (!AppState.get().isShowFastBookmarks) {
                if (fast.equals(next.text)) {
                    iterator.remove();
                }

            }

        }
        return all;
    }


    /**
     * Whether the book a mark was made in is still to be had.
     *
     * A book on added storage is read from a copy of itself, and the copy is only kept while
     * there is room for it. The book is still there on the storage, so a mark in it is not a mark
     * in a book that has gone.
     */
    private static boolean isBookAvailable(String path) {
        if (path == null) {
            return false;
        }
        if (CacheZipUtils.isSafBookCopy(path) || ExtUtils.isExteralSD(path)) {
            return true;
        }
        return new File(path).isFile();
    }

    /**
     * What the bookmarks screen shows: the bookmarks, and the reader's own marks beside them.
     *
     * A passage marked by selecting it is a bookmark to the reader who made it, and in a book
     * read by an engine that works in locations rather than pages - foliate-js and its CFIs - it
     * is the only kind of mark that names an exact place. The dialog inside the book has always
     * shown the two together; the screen outside it now does too.
     */
    public List<AppBookmark> getAllWithMarks(Context c) {
        final List<AppBookmark> all = getAll(c);
        try {
            for (AppAnnotation mark : AnnotationsData.get()
                                                     .all()) {
                if (AppState.get().isShowOnlyAvailabeBooks && !isBookAvailable(mark.getPath())) {
                    continue;
                }
                all.add(new BookmarksAdapter.Mark(mark, mark.getPath()));
            }
            Collections.sort(all, BY_TIME);
        } catch (Exception e) {
            LOG.e(e);
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


//        Iterator<AppBookmark> iterator = all.iterator();
//        while (iterator.hasNext()) {
//            AppBookmark next = iterator.next();
//            if (next.getText().equals(quick)) {
//                iterator.remove();
//            }
//        }

        LOG.d("getAll-size", all.size());
        Collections.sort(all, BY_TIME);
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
        Collections.sort(all, BY_PERCENT);
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

    static final Comparator<AppBookmark> BY_PERCENT = new Comparator<AppBookmark>() {

        @Override
        public int compare(AppBookmark o1, AppBookmark o2) {
            return Float.compare(o1.getPercent(), o2.getPercent());
        }
    };

    static final Comparator<AppBookmark> BY_TIME = new Comparator<AppBookmark>() {

        @Override
        public int compare(AppBookmark o1, AppBookmark o2) {
            return Float.compare(o2.getTime(), o1.getTime());
        }
    };


    public Map<String, List<AppBookmark>> getBookmarksMap() {
        return null;
    }


    public void cleanBookmarks() {
        //IO.writeObj(AppProfile.syncBookmarks.getPath(), "{}");
        AppData.get().clearAll(AppProfile.APP_BOOKMARKS_JSON);
    }


}
