package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
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
import java.util.Iterator;
import java.util.List;

public class AppData {

    static AppData inst = new AppData();

    public static AppData get() {
        return inst;
    }


    public synchronized void add(SimpleMeta s, File file) {
        List<SimpleMeta> current = getSimpleMeta(file);

        final SimpleMeta syncMeta = SimpleMeta.SyncSimpleMeta(s);
        current.remove(syncMeta);
        current.add(syncMeta);

        writeSimpleMeta(current, file);
        LOG.d("Objects-save-add", "SAVE Recent");
    }

    public synchronized void removeIt(SimpleMeta s) {
        List<SimpleMeta> res = getSimpleMeta(s.file);
        res.remove(s);
        writeSimpleMeta(res, s.file);
        LOG.d("AppData removeFavorite", s.getPath());
    }

    public void removeAll(FileMeta meta, String name) {
        SimpleMeta s = SimpleMeta.SyncSimpleMeta(meta.getPath());
        for (File file : AppProfile.getAllFiles(name)) {
            List<SimpleMeta> res = getSimpleMeta(file);
            if (res.contains(s)) {
                res.remove(s);
                writeSimpleMeta(res, file);
            }
        }
    }

    public void removeRecent(FileMeta meta) {
        removeAll(meta, AppProfile.APP_RECENT_JSON);
    }

    public void removeFavorite(FileMeta meta) {
        removeAll(meta, AppProfile.APP_FAVORITE_JSON);
    }

    public void clearAll(String name) {
        for (File file : AppProfile.getAllFiles(name)) {
            writeSimpleMeta(new ArrayList<>(), file);
        }
    }


    private synchronized List<SimpleMeta> getAll(String name) {
        List<SimpleMeta> exclude = new ArrayList<>();
        for (File file : AppProfile.getAllFiles(name)) {
            addSimpleMeta(exclude, file);
        }
        return exclude;
    }

    public void addRecent(SimpleMeta simpleMeta) {
        add(simpleMeta, AppProfile.syncRecent);
    }

    public void addFavorite(SimpleMeta simpleMeta) {
        add(simpleMeta, AppProfile.syncFavorite);
    }

    public void addExclue(String path) {
        add(new SimpleMeta(path), AppProfile.syncExclude);
    }

    public void clearFavorites() {
        clearAll(AppProfile.APP_FAVORITE_JSON);
    }

    public void clearRecents() {
        clearAll(AppProfile.APP_RECENT_JSON);
    }


    public synchronized List<FileMeta> getAllSyncBooks() {
        List<FileMeta> res = new ArrayList<>();

        SearchCore.search(res, AppProfile.SYNC_FOLDER_BOOKS, null);

        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);
        return res;
    }

    public synchronized List<FileMeta> getAllFavoriteFiles() {
        List<SimpleMeta> favorites = getAll(AppProfile.APP_FAVORITE_JSON);

        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : favorites) {
            s = SimpleMeta.SyncSimpleMeta(s);

            if (new File(s.getPath()).isFile()) {
                FileMeta meta = AppDB.get().getOrCreate(s.getPath());
                meta.setIsStar(true);
                meta.setIsStarTime(s.time);
                meta.setIsSearchBook(true);
                if (!res.contains(meta)) {
                    res.add(meta);
                }
            }
        }
        SharedBooks.updateProgress(res,false);
        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);
        return res;
    }

    public synchronized List<FileMeta> getAllFavoriteFolders() {
        List<SimpleMeta> favorites = getAll(AppProfile.APP_FAVORITE_JSON);

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

        Collections.sort(res, FileMetaComparators.BY_DATE);
        Collections.reverse(res);
        return res;
    }


    public synchronized List<FileMeta> getAllRecent() {
        List<SimpleMeta> recent = getAll(AppProfile.APP_RECENT_JSON);


        LOG.d("getAllRecent");
        List<FileMeta> res = new ArrayList<>();


        final Iterator<SimpleMeta> iterator = recent.iterator();
        while (iterator.hasNext()) {
            SimpleMeta s = SimpleMeta.SyncSimpleMeta(iterator.next());

            if (!new File(s.getPath()).isFile()) {
                LOG.d("getAllRecent can't find file", s.getPath());
                continue;
            }

            FileMeta meta = AppDB.get().getOrCreate(s.getPath());
            meta.setIsRecentTime(s.time);

            //meta.setIsRecent(true);

            LOG.d("meta-aa",meta.getPath(), s.time);

            if (!res.contains(meta)) {
                res.add(meta);
            }


        }
        SharedBooks.updateProgress(res, false);
        Collections.sort(res, FileMetaComparators.BY_RECENT_TIME);
        Collections.reverse(res);
        return res;
    }

    public synchronized List<SimpleMeta> getAllExcluded() {
        return getAll(AppProfile.APP_EXCLUDE_JSON);
    }


    public static List<SimpleMeta> getSimpleMeta(File file) {
        List<SimpleMeta> list = new ArrayList<>();
        readSimpleMeta1(list, file, true);
        return list;
    }

    public static void addSimpleMeta(List<SimpleMeta> list, File file) {
        readSimpleMeta1(list, file, false);
    }

    private static void readSimpleMeta1(List<SimpleMeta> list, File file, boolean clear) {
        if (clear) {
            list.clear();
        }
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
                SimpleMeta meta = new SimpleMeta();
                meta.file = file;
                Objects.loadFromJson(meta, array.getJSONObject(i));
                list.add(meta);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void writeSimpleMeta(List<SimpleMeta> list, File file) {
        JSONArray array = new JSONArray();
        for (SimpleMeta meta : list) {
            JSONObject o = Objects.toJSONObject(meta);
            array.put(o);
            LOG.d("writeSimpleMeta", o);
        }
        IO.writeObjAsync(file, array);

    }

    public static List<SimpleMeta> convert(List<String> list) {
        List<SimpleMeta> res = new ArrayList<>();
        for (String string : list) {
            res.add(new SimpleMeta(string));
        }
        return res;

    }


}

