package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.widget.RecentUpates;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.librera.JSONArray;
import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AppData {

    public static final int LIMIT = 3;
    static AppData inst = new AppData();

    public static AppData get() {
        return inst;
    }


    public void add(SimpleMeta s, File file) {
        List<SimpleMeta> current = getSimpleMeta(file);

        final SimpleMeta syncMeta = SimpleMeta.SyncSimpleMeta(s);
        current.remove(syncMeta);
        current.add(syncMeta);

        writeSimpleMeta(current, file);
        LOG.d("Objects-save-add", "SAVE Recent", s.getPath(), file.getPath(), s.time);
        RecentUpates.updateAll();
    }

    public synchronized void removeIt(SimpleMeta s) {
        List<SimpleMeta> res = getSimpleMeta(s.file);
        res.remove(s);
        writeSimpleMeta(res, s.file);
        LOG.d("AppData removeFavorite", s.getPath());
    }

    public void removeAll(FileMeta meta, String name) {
        final List<File> allFiles = AppProfile.getAllFiles(name);
        for (File file : allFiles) {
            List<SimpleMeta> res = getSimpleMeta(file);
            boolean find = false;
            final Iterator<SimpleMeta> iterator = res.iterator();
            while (iterator.hasNext()) {
                SimpleMeta it = iterator.next();
                if (ExtUtils.getFileName(it.getPath()).equals(ExtUtils.getFileName(meta.getPath()))) {
                    iterator.remove();
                    find = true;
                }
            }
            if (find) {
                writeSimpleMeta(res, file);
            }
        }
        RecentUpates.updateAll();
    }

    public void removeRecent(FileMeta meta) {
        LOG.d("removeRecent", meta.getPath());
        removeAll(meta, AppProfile.APP_RECENT_JSON);
    }

    public void removeFavorite(FileMeta meta) {
        removeAll(meta, AppProfile.APP_FAVORITE_JSON);
    }

    public void clearAll(String name) {
        final List<File> allFiles = AppProfile.getAllFiles(name);
        for (File file : allFiles) {
            writeSimpleMeta(new ArrayList<>(), file);
        }
        RecentUpates.updateAll();
    }


    private synchronized List<SimpleMeta> getAll(String name) {
        List<SimpleMeta> result = new ArrayList<>();
        final List<File> allFiles = AppProfile.getAllFiles(name);
        for (File file : allFiles) {
            addSimpleMeta(result, file);
        }
        return result;
    }

    public void addRecent(SimpleMeta simpleMeta) {
        if (simpleMeta.time == 0) {
            simpleMeta.time = System.currentTimeMillis();
        }
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

        Collections.sort(res, FileMetaComparators.BY_SYNC_DATE);
        Collections.reverse(res);
        return res;
    }

    public List<FileMeta> getAllFavoriteFiles(boolean updateProgress) {
        List<SimpleMeta> favorites = getAll(AppProfile.APP_FAVORITE_JSON);

        List<FileMeta> res = new ArrayList<>();

        AppDB.get().clearAllFavorites();

        for (SimpleMeta s : favorites) {
            s = SimpleMeta.SyncSimpleMeta(s);

            if (new File(s.getPath()).isFile() || Clouds.isCloudFile(s.getPath())) {
                FileMeta meta = AppDB.get().getOrCreate(s.getPath());
                meta.setIsStar(true);
                meta.setIsStarTime(s.time);
                meta.setIsSearchBook(true);
                meta.setCusType(null);
                res.remove(meta);
                res.add(meta);
                AppDB.get().update(meta);
            }
        }
        if (updateProgress) {
            SharedBooks.updateProgress(res, false, LIMIT);
        }
        try {
            Collections.sort(res, FileMetaComparators.BY_DATE);
        } catch (Exception e) {
            LOG.e(e);
        }

        Collections.reverse(res);
        return res;
    }

    public List<FileMeta> getAllFavoriteFolders() {
        List<SimpleMeta> favorites = getAll(AppProfile.APP_FAVORITE_JSON);

        List<FileMeta> res = new ArrayList<>();
        for (SimpleMeta s : favorites) {


            if (new File(s.getPath()).isDirectory() || Clouds.isCloudDir(s.getPath())) {
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

    public static boolean contains(List<FileMeta> list, String path) {
        for (FileMeta f : list) {
            if (ExtUtils.getFileName(f.getPath()).equals(ExtUtils.getFileName(path))) {
                return true;
            }
        }
        return false;

    }

    public List<SimpleMeta> getAllRecentSimple() {
        return getAll(AppProfile.APP_RECENT_JSON);
    }

    public List<FileMeta> getAllRecent(boolean updateProgress) {
        List<SimpleMeta> recent = getAll(AppProfile.APP_RECENT_JSON);
        Collections.sort(recent, FileMetaComparators.SIMPLE_META_BY_TIME);

        LOG.d("getAllRecent");
        List<FileMeta> res = new ArrayList<>();

        for (SimpleMeta it : recent) {
            SimpleMeta s = SimpleMeta.SyncSimpleMeta(it);

            if (!new File(s.getPath()).isFile()) {
                LOG.d("getAllRecent can't find file", s.getPath());
                continue;
            }

            FileMeta meta = AppDB.get().getOrCreate(s.getPath());
            if (res.contains(meta)) {
                continue;
            }
            if (updateProgress && TxtUtils.isEmpty(meta.getTitle())) {
                FileMetaCore.reUpdateIfNeed(meta);
            }

            meta.setIsRecentTime(s.time);
            res.add(meta);
        }

        if (updateProgress) {
            SharedBooks.updateProgress(res, false, LIMIT);
        }
        return res;
    }

    public List<SimpleMeta> getAllExcluded() {
        return getAll(AppProfile.APP_EXCLUDE_JSON);
    }

    static Map<String, List<SimpleMeta>> cacheSM = new HashMap<>();

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
                final LinkedJSONObject it = array.getJSONObject(i);
                SimpleMeta meta = new SimpleMeta();
                meta.path = it.optString(SimpleMeta.JSON_PATH);
                meta.time = it.optLong(SimpleMeta.JSON_TIME);
                meta.file = file;
                list.add(meta);
            }
        } catch (Exception e) {
            LOG.e(e);
        }


    }

    public static void writeSimpleMeta(List<SimpleMeta> list, File file) {
        JSONArray array = new JSONArray();
        for (SimpleMeta meta : list) {
            LinkedJSONObject o = new LinkedJSONObject();
            try {
                o.put(SimpleMeta.JSON_PATH, meta.path);
                o.put(SimpleMeta.JSON_TIME, meta.time);
            } catch (JSONException e) {
                LOG.e(e);
            }
            array.put(o);
            LOG.d("writeSimpleMeta", o);
        }
        //cacheSM.remove(file.getPath() + file.lastModified());
        IO.writeObjAsync(file, array);
        //cacheSM.put(file.getPath() + file.lastModified(), list);


    }

    public static List<SimpleMeta> convert(List<String> list) {
        List<SimpleMeta> res = new ArrayList<>();
        for (String string : list) {
            res.add(new SimpleMeta(string));
        }
        return res;

    }


}

