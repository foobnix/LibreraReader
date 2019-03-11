package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppData  {

    public static final File syncRecent = new File(AppsConfig.SYNC_FOLDER, "app-Recent.json");
    public static final File syncFavorite = new File(AppsConfig.SYNC_FOLDER, "app-Favorite.json");


    private List<SimpleMeta> recent = new ArrayList<>();
    private List<SimpleMeta> favorites = new ArrayList<>();


    static AppData inst = new AppData();

    public static AppData get() {
        return inst;
    }


    public void addRecent(SimpleMeta s) {
        recent.remove(s);
        recent.add(s);
        writeSimpleMeta(recent, syncRecent);
    }
    public void removeRecent(SimpleMeta s){
        recent.remove(s);
        writeSimpleMeta(recent, syncRecent);
        LOG.d("AppData removeRecent",s.path);
    }

    public void addFavorite(SimpleMeta s) {
        favorites.remove(s);
        favorites.add(s);
        LOG.d("AppData addFavorite",s.path);
        writeSimpleMeta(favorites, syncFavorite);
    }
    public void removeFavorite(SimpleMeta s){
        favorites.remove(s);
        writeSimpleMeta(favorites, syncFavorite);
        LOG.d("AppData removeFavorite",s.path);

    }

    public void clearRecents(){
        recent.clear();
        writeSimpleMeta(recent, syncRecent);
    }
    public void clearFavorites(){
        favorites.clear();
        writeSimpleMeta(favorites, syncFavorite);

    }

    public void loadFavorites() {
        readSimpleMeta(favorites, syncFavorite);
    }

    public List<FileMeta> getAllFavoriteFiles() {
        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : favorites) {
            if (new File(s.path).isFile()) {
                FileMeta meta = AppDB.get().getOrCreate(s.path);
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
            if (new File(s.path).isDirectory()) {
                FileMeta meta = AppDB.get().getOrCreate(s.path);
                meta.setIsStar(true);
                meta.setPathTxt(ExtUtils.getFileName(s.path));
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

    public List<FileMeta> getAllRecent() {
        readSimpleMeta(recent, syncRecent);
        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : recent) {
            FileMeta meta = AppDB.get().getOrCreate(s.path);
            meta.setIsRecent(true);
            meta.setIsSearchBook(true);
            meta.setIsRecentTime(s.time);
            res.add(meta);
        }
        SharedBooks.updateProgress(res);
        Collections.sort(res, FileMetaComparators.BY_RECENT_TIME);
        Collections.reverse(res);
        return res;
    }





    private void readSimpleMeta(List<SimpleMeta> list, File file) {
        list.clear();
        String in = IO.readString(file);
        if (TxtUtils.isEmpty(in)) {
            return;
        }

        try {
            JSONArray array = new JSONArray(in);
            for (int i = 0; i < array.length(); i++) {
                SimpleMeta meta = new SimpleMeta();
                Objects.loadFromJson(meta, array.getJSONObject(i));
                list.add(meta);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private void writeSimpleMeta(List<SimpleMeta> list, File file) {
        JSONArray array = new JSONArray();
        for (SimpleMeta meta : list) {
            JSONObject o = Objects.toJSONObject(meta);
            array.put(o);
            LOG.d("writeSimpleMeta", o);
        }
        IO.writeObjAsync(file, array);

    }
}

