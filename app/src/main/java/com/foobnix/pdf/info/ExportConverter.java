package com.foobnix.pdf.info;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;
import com.foobnix.sys.ImageExtractor;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import org.ebookdroid.BookType;
import org.ebookdroid.common.settings.books.SharedBooks;
import org.ebookdroid.core.codec.CodecDocument;
import org.librera.JSONArray;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExportConverter {


    public static void copyPlaylists() {
        File oldDir = new File(AppProfile.DOWNLOADS_DIR, "Librera/Playlist");
        File[] list = oldDir.listFiles();

        if (list != null) {
            AppProfile.syncPlaylist.mkdirs();
            for (File file : list) {
                LOG.d("copyPlaylists", file.getPath());
                IO.copyFile(file, new File(AppProfile.syncPlaylist, file.getName()));
            }
        }
    }

    public static void covertJSONtoNew(Context c, File file) throws Exception {
        LOG.d("covertJSONtoNew", file);


        String st = IO.readString(file);
        LinkedJSONObject obj = new LinkedJSONObject(st);


        IO.writeString(AppProfile.syncState, obj.getJSONObject("pdf").toString());
        IO.writeString(AppProfile.syncCSS, obj.getJSONObject("BookCSS").toString());

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
            LinkedJSONObject it = tags.getJSONObject(i);
            String path = it.getString("path");
            String tag = it.getString("tag");
            TagData.saveTags(path, tag);
        }

        TagData.restoreTags();

        LinkedJSONObject books = obj.getJSONObject("BOOKS");
        Iterator<String> keys = books.keys();
        Map<String, Integer> cache = new HashMap<>();

        LinkedJSONObject resObj = IO.readJsonObject(AppProfile.syncProgress);
        while (keys.hasNext()) {

            String stringObj = books.getString(keys.next());
            stringObj = stringObj.replace("\\\"", "").replace("\\", "");

            LOG.d(stringObj);

            LinkedJSONObject value = new LinkedJSONObject(stringObj);


            AppBook appBook = new AppBook(value.getString("fileName"));
            appBook.z = value.getInt("zoom");
            appBook.sp = value.getBoolean("splitPages");
            appBook.cp = value.getBoolean("cropPages");
            appBook.dp = value.getBoolean("doublePages");
            appBook.dc = value.getBoolean("doublePagesCover");
            appBook.setLock(value.getBoolean("isLocked"));
            appBook.s = value.getInt("speed");
            appBook.d = value.optInt("pageDelta", 0);

            LinkedJSONObject currentPage = value.getJSONObject("currentPage");
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


            //SharedBooks.save(appBook, false);

            if (appBook.p > 1) {
                appBook.p = 0;
            }
            final String fileName = ExtUtils.getFileName(appBook.path);
            resObj.put(fileName, Objects.toJSONObject(appBook));
        }
        IO.writeObj(AppProfile.syncProgress, resObj);

        LinkedJSONObject bookmarks = obj.getJSONObject("ViewerPreferences");
        Iterator<String> bKeys = bookmarks.keys();

        LinkedJSONObject resObj2 = IO.readJsonObject(AppProfile.syncBookmarks);

        while (bKeys.hasNext()) {
            String value = bookmarks.getString(bKeys.next());
            LOG.d(value);
            String[] it = value.split("~");

            AppBookmark bookmark = new AppBookmark();
            final String path = it[0];
            bookmark.setPath(path);
            bookmark.text = it[1];
            try {
                bookmark.t = Long.parseLong(it[4]);
            } catch (Exception e) {
                LOG.d("Error covertJSONtoNew", value);
                LOG.e(e);
            }
            try {
                bookmark.t = Long.parseLong(it[4]);
                if (it.length > 5) {
                    bookmark.p = Float.parseFloat(it[5]);
                } else {
                    if (!cache.containsKey(path)) {
                        if (new File(path).isFile() && (BookType.PDF.is(path) || BookType.DJVU.is(path)) ) {
                            final CodecDocument doc = ImageExtractor.singleCodecContext(path, "", 0, 0);
                            int pageCount = doc.getPageCount();
                            cache.put(path, pageCount);
                            LOG.d("Page-counts update ", path, pageCount);
                            doc.recycle();
                        }else{
                            LOG.d("Page-counts not found", path);
                        }
                    }
                    if (cache.containsKey(path)) {
                        try {
                            bookmark.p = (float) Integer.parseInt(it[2]) / cache.get(path);
                            LOG.d("Page-counts percent", path, bookmark.p);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.d("Error covertJSONtoNew", value);

                LOG.e(e);
            }

            if (bookmark.p > 1) {
                bookmark.p = 0;
            }

            resObj2.put("" + bookmark.t, Objects.toJSONObject(bookmark));
        }

        IO.writeObjAsync(AppProfile.syncBookmarks, resObj2);

        SharedBooks.cache.clear();

    }

    public static void zipFolder(File input, File output) throws ZipException {
        LOG.d("ZipFolder", input, output);
        ZipFile zipFile = new ZipFile(output);

        ZipParameters parameters = new ZipParameters();

        //parameters.setIncludeRootFolder(false);
        parameters.setCompressionMethod(CompressionMethod.DEFLATE);
        parameters.setCompressionLevel(CompressionLevel.NORMAL);

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


}
