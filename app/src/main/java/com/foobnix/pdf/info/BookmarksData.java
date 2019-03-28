package com.foobnix.pdf.info;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppProfile;

import org.json.JSONObject;

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
        LOG.d("BookmarksData", "add", bookmark.p, bookmark.text);
        try {
            JSONObject obj = IO.readJsonObject(AppProfile.syncBookmarks);
            final String fileName = ExtUtils.getFileName(bookmark.path);
            obj.put("" + bookmark.t, Objects.toJSONObject(bookmark));
            IO.writeObjAsync(AppProfile.syncBookmarks, obj);
        } catch (Exception e) {
            LOG.e(e);
        }
    }


    public void remove(AppBookmark bookmark) {
        LOG.d("BookmarksData", "remove", bookmark.p, bookmark.text);

        try {
            JSONObject obj = IO.readJsonObject(AppProfile.syncBookmarks);
            if (obj.has("" + bookmark.t)) {
                obj.remove("" + bookmark.t);
            }
            IO.writeObjAsync(AppProfile.syncBookmarks, obj);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public List<AppBookmark> getBookmarksByBook(File file) {
        return getBookmarksByBook(file.getPath());
    }

    public List<AppBookmark> getAll(Context c) {
        String quick = c.getString(R.string.fast_bookmark);

        List<AppBookmark> all = new ArrayList<>();

        try {
            if(!AppProfile.syncBookmarks.isFile()){
                return all;
            }
            JSONObject obj = IO.readJsonObject(AppProfile.syncBookmarks);

            if(!AppProfile.syncBookmarks.isFile()){
                return all;
            }

            final Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                final String next = keys.next();

                AppBookmark appBookmark = new AppBookmark();
                final JSONObject local = obj.getJSONObject(next);
                Objects.loadFromJson(appBookmark, local);
                all.add(appBookmark);
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

        try {
            JSONObject obj = IO.readJsonObject(AppProfile.syncBookmarks);

            final Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                final String next = keys.next();

                AppBookmark appBookmark = new AppBookmark();
                final JSONObject local = obj.getJSONObject(next);
                Objects.loadFromJson(appBookmark, local);
                if (appBookmark.getPath().equals(path)) {
                    all.add(appBookmark);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        LOG.d("getBookmarksByBook", path, all.size());
        Collections.sort(all, BY_PERCENT);
        return all;
    }

    public boolean hasBookmark(String lastBookPath, int page, int pages) {
        //TODO Implement
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
        IO.writeObj(AppProfile.syncBookmarks.getPath(), "{}");
    }


}
