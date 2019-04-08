package com.foobnix.pdf.info;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExportConverter {


    public static void copyPlaylists() {
        File syncDir = new File(AppProfile.SYNC_FOLDER_PROFILE, "playlists");
        File oldDir = new File(AppProfile.DOWNLOADS_DIR, "Librera/Playlist");
        File[] list = oldDir.listFiles();

        if (list != null) {
            syncDir.mkdirs();
            for (File file : list) {
                LOG.d("copyPlaylists", file.getPath());
                IO.copyFile(file, new File(syncDir, file.getName()));
            }
        }
    }

    public static void covertJSONtoNew(Context c, File file) throws Exception {
        LOG.d("covertJSONtoNew", file);

        String st = IO.readString(file);
        JSONObject obj = new JSONObject(st);


        IO.writeString(AppProfile.syncState, obj.getJSONObject("pdf").toString());
        IO.writeString(AppProfile.syncCSS, obj.getJSONObject("BookCSS").toString());

        AppProfile.load(c);

        JSONArray recent = obj.getJSONArray("Recent");
        long t = System.currentTimeMillis();
        for (int i = 0; i < recent.length(); i++) {
            AppData.get().addRecent(new SimpleMeta(recent.getString(i), t - i));
        }

        JSONArray favorites = obj.getJSONArray("StarsBook");
        for (int i = 0; i < favorites.length(); i++) {
            AppData.get().addFavorite(new SimpleMeta(favorites.getString(i), i));
        }

        JSONArray folders = obj.getJSONArray("StarsFolder");
        for (int i = 0; i < folders.length(); i++) {
            AppData.get().addFavorite(new SimpleMeta(folders.getString(i), i));
        }

        JSONArray tags = obj.getJSONArray("TAGS");
        for (int i = 0; i < tags.length(); i++) {
            JSONObject it = tags.getJSONObject(i);
            String path = it.getString("path");
            String tag = it.getString("tag");
            TagData.saveTags(path, tag);
        }

        TagData.restoreTags();

        JSONObject books = obj.getJSONObject("BOOKS");
        Iterator<String> keys = books.keys();
        Map<String, Integer> cache = new HashMap<>();
        while (keys.hasNext()) {

            String stringObj = books.getString(keys.next());
            stringObj = stringObj.replace("\\\"", "").replace("\\", "");

            LOG.d(stringObj);

            JSONObject value = new JSONObject(stringObj);


            AppBook appBook = new AppBook(value.getString("fileName"));
            appBook.z = value.getInt("zoom");
            appBook.sp = value.getBoolean("splitPages");
            appBook.cp = value.getBoolean("cropPages");
            appBook.dp = value.getBoolean("doublePages");
            appBook.dc = value.getBoolean("doublePagesCover");
            appBook.setLock(value.getBoolean("isLocked"));
            appBook.s = value.getInt("speed");
            appBook.d = value.optInt("pageDelta", 0);

            JSONObject currentPage = value.getJSONObject("currentPage");
            int pages = value.optInt("pages", 0);
            final int docIndex = currentPage.getInt("docIndex") + 1;
            if (pages > 0) {
                appBook.p = (float) docIndex / pages;
            } else if (docIndex >= 2) {//old import support
                appBook.p = docIndex;
            }
            appBook.x = value.getInt("offsetX");
            appBook.y = value.getInt("offsetY");

            cache.put(appBook.path, pages);
            LOG.d("Export-PUT", appBook.path, pages);


            SharedBooks.save(appBook);


        }

        JSONObject bookmarks = obj.getJSONObject("ViewerPreferences");
        Iterator<String> bKeys = bookmarks.keys();
        while (bKeys.hasNext()) {
            String value = bookmarks.getString(bKeys.next());
            LOG.d(value);
            String[] it = value.split("~");

            AppBookmark bookmark = new AppBookmark();
            final String path = it[0];
            bookmark.setPath(path);
            bookmark.text = it[1];
            bookmark.t = Long.parseLong(it[4]);
            if (it.length > 5) {
                bookmark.p = Float.parseFloat(it[5]);
            } else {
                try {
                    bookmark.p = (float) Integer.parseInt(it[2]) / cache.get(path);
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
            BookmarksData.get().add(bookmark);

        }


    }

    public static void zipFolder(File input, File output) throws ZipException {
        LOG.d("ZipFolder", input, output);
        ZipFile zipFile = new ZipFile(output);

        ZipParameters parameters = new ZipParameters();

        //parameters.setIncludeRootFolder(false);
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

        final File[] files = input.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(AppProfile.PROFILE_PREFIX)) {
                    zipFile.addFolder(file, parameters);
                }
            }
        }


        //zipFile.createZipFile(input, parameters);
    }

    public static void unZipFolder(File input, File output) throws ZipException {
        ZipFile zipFile = new ZipFile(input);
        zipFile.extractAll(output.getPath());
        LOG.d("UnZipFolder", input, output);
    }

    public static boolean mergeBookProgrss(File temp, File original) throws JSONException {
        LOG.d("mergeBookProgrss", temp, original);

        JSONObject f1 = IO.readJsonObject(temp);
        JSONObject f2 = IO.readJsonObject(original);


        boolean isMerged = false;
        final Iterator<String> keys = f1.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!f2.has(key)) {
                f2.put(key, f1.getJSONObject(key));
                LOG.d("Merge-book-missing", key);
                isMerged = true;

            } else {
                LOG.d("getJSONObject by key", key);
                try {
                    final long p1 = f1.getJSONObject(key).optLong("t", 0L);
                    final long p2 = f2.getJSONObject(key).optLong("t", 0L);
                    if (p1 > p2) {
                        LOG.d("Merge-book-update", key, p1, p2);
                        f2.put(key, f1.getJSONObject(key));
                        isMerged = true;
                    }
                } catch (JSONException e) {
                    LOG.e(e);
                }
            }
        }

        IO.writeObjAsync(original, f2);
        temp.delete();
        LOG.d("Merge-", temp, original);
        return isMerged;
    }

    public static SimpleMeta merge(SimpleMeta s1, SimpleMeta s2) {
        if (s1.time > s2.time) {
            return s1;
        }
        return s2;
    }

    public static void mergeSimpleMeta(File temp, File original) {
        JSONObject f1 = IO.readJsonObject(temp);
        JSONObject f2 = IO.readJsonObject(original);

        List<SimpleMeta> res2 = new ArrayList<>();
        AppData.readSimpleMeta(res2, original, SimpleMeta.class);


        List<SimpleMeta> res1 = new ArrayList<>();
        AppData.readSimpleMeta(res1, temp, SimpleMeta.class);

        for (SimpleMeta s : res1) {
            if (!res2.contains(s)) {
                res2.add(s);
                LOG.d("Merge-book", s.getPath());
            }
        }
        AppData.writeSimpleMeta(res2, original);
        LOG.d("Merge-", temp, original);
    }
}
