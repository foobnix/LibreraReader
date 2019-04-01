package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppData {


    private List<SimpleMeta> recent = new ArrayList<>();
    private List<SimpleMeta> favorites = new ArrayList<>();
    private List<SimpleMeta> exclude = new ArrayList<>();


    static AppData inst = new AppData();

    public static AppData get() {
        return inst;
    }


    public void addRecent(SimpleMeta s) {

        if (TxtUtils.isListNotEmpty(recent)) {
            Collections.sort(recent, FileMetaComparators.BY_RECENT_TIME_2);
            if (s.getPath().equals(recent.get(0).getPath())) {
                LOG.d("Skip-recent");
                return;
            }
        }
        recent.remove(s);
        recent.add(s);
        writeSimpleMeta(recent, AppProfile.syncRecent);
        LOG.d("Objects-save", "SAVE Recent");
    }

    public void removeRecent(SimpleMeta s) {
        recent.remove(s);
        writeSimpleMeta(recent, AppProfile.syncRecent);
        LOG.d("AppData removeRecent", s.getPath());
    }

    public void addFavorite(SimpleMeta s) {
        favorites.remove(s);
        favorites.add(s);
        LOG.d("AppData addFavorite", s.getPath());
        writeSimpleMeta(favorites, AppProfile.syncFavorite);
        LOG.d("Objects-save", "SAVE Favorite");
    }

    public List<String> getAllExcluded() {
        readSimpleMeta(exclude, AppProfile.syncExclude, SimpleMeta.class);
        ArrayList<String> res = new ArrayList<String>();
        for (SimpleMeta s : exclude) {
            res.add(s.getPath());
        }
        return res;
    }


    public void addExclue(String path) {
        final SimpleMeta sm = new SimpleMeta(path);
        exclude.remove(sm);
        exclude.add(sm);
        LOG.d("AppData addFavorite", path);
        writeSimpleMeta(exclude, AppProfile.syncExclude);
        LOG.d("Objects-save", "SAVE Favorite");
    }

    public void removeFavorite(SimpleMeta s) {
        favorites.remove(s);
        writeSimpleMeta(favorites, AppProfile.syncFavorite);
        LOG.d("AppData removeFavorite", s.getPath());

    }

    public void clearRecents() {
        recent.clear();
        writeSimpleMeta(recent, AppProfile.syncRecent);
        LOG.d("Objects-save", "SAVE Recent");
    }

    public void clearFavorites() {
        favorites.clear();
        writeSimpleMeta(favorites, AppProfile.syncFavorite);
        LOG.d("Objects-save", "SAVE Favorite");

    }

    public void loadFavorites() {
        readSimpleMeta(favorites, AppProfile.syncFavorite, SimpleMeta.class);
    }

    public List<FileMeta> getAllSyncBooks() {
        List<FileMeta> res = new ArrayList<>();

        SearchCore.search(res, AppProfile.SYNC_FOLDER_BOOKS, null);

        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);
        return res;

    }

    public List<FileMeta> getAllFavoriteFiles() {
        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : favorites) {
            if (new File(s.getPath()).isFile()) {
                FileMeta meta = AppDB.get().getOrCreate(s.getPath());
                meta.setIsStar(true);
                meta.setIsStarTime(s.time);
                meta.setIsSearchBook(true);
                res.add(meta);
            }
        }
        SharedBooks.updateProgress(res);
        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);
        return res;
    }

    public List<FileMeta> getAllFavoriteFolders() {
        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : favorites) {
            if (new File(s.getPath()).isDirectory()) {
                FileMeta meta = AppDB.get().getOrCreate(s.getPath());
                meta.setIsStar(true);
                meta.setPathTxt(ExtUtils.getFileName(s.getPath()));
                meta.setIsSearchBook(false);
                meta.setIsStarTime(s.time);
                meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
                res.add(meta);
            }
        }

        SharedBooks.updateProgress(res);


        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);
        return res;
    }


    public synchronized List<FileMeta> getAllRecent() {
        readSimpleMeta(recent, AppProfile.syncRecent, SimpleMeta.class);
        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : recent) {
            if (Clouds.isLibreraSyncFile(s.getPath())) {
                s.setPath(new File(AppProfile.SYNC_FOLDER_BOOKS, ExtUtils.getFileName(s.getPath())).getPath());
            }

            if (!new File(s.getPath()).isFile()) {
                LOG.d("getAllRecent can't find file", s.getPath());
                continue;
            }

            FileMeta meta = AppDB.get().getOrCreate(s.getPath());
            meta.setIsRecent(true);
            //meta.setIsSearchBook(true);
            meta.setIsRecentTime(s.time);
            res.add(meta);
        }
        SharedBooks.updateProgress(res);
        Collections.sort(res, FileMetaComparators.BY_RECENT_TIME);
        Collections.reverse(res);
        return res;
    }


    public static <T> void readSimpleMeta(List<T> list, File file, Class<T> clazz) {
        list.clear();
        if (!file.exists()) {
            return;
        }
        String in = IO.readString(file);
        if (TxtUtils.isEmpty(in)) {
            return;
        }

        try {
            JSONArray array = new JSONArray(in);
            for (int i = 0; i < array.length(); i++) {
                T meta = clazz.newInstance();
                Objects.loadFromJson(meta, array.getJSONObject(i));
                list.add(meta);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static <T> void writeSimpleMeta(List<T> list, File file) {
        JSONArray array = new JSONArray();
        for (T meta : list) {
            JSONObject o = Objects.toJSONObject(meta);
            array.put(o);
            LOG.d("writeSimpleMeta", o);
        }
        IO.writeObjAsync(file, array);

    }
}

