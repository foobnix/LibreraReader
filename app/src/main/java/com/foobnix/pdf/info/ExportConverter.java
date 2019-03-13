package com.foobnix.pdf.info;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppBookmark;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;

public class ExportConverter {

    public static void covertJSONtoNew(Context c, File file) throws Exception {
        LOG.d("covertJSONtoNew", file);

        String st = IO.readString(file);
        JSONObject obj = new JSONObject(st);


        IO.writeString(AppState.syncFile, obj.getJSONObject("pdf").toString());
        IO.writeString(BookCSS.syncFile, obj.getJSONObject("BookCSS").toString());

        AppState.get().loadIn(c);
        BookCSS.get().load(c);
        TintUtil.init();

        JSONArray recent = obj.getJSONArray("Recent");
        for (int i = 0; i < recent.length(); i++) {
            AppData.get().addRecent(new SimpleMeta(recent.getString(i), i));
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
            appBook.l = value.getBoolean("isLocked");
            appBook.s = value.getInt("speed");
            appBook.d = value.getInt("pageDelta");

            JSONObject currentPage = value.getJSONObject("currentPage");
            appBook.p = (float) currentPage.getInt("docIndex") / value.getInt("pages");
            appBook.x = value.getInt("offsetX");
            appBook.y = value.getInt("offsetY");


            SharedBooks.save(appBook);


        }

        JSONObject bookmarks = obj.getJSONObject("ViewerPreferences");
        Iterator<String> bKeys = bookmarks.keys();
        while (bKeys.hasNext()) {
            String value = bookmarks.getString(bKeys.next());
            LOG.d(value);
            String[] it = value.split("~");

            AppBookmark bookmark = new AppBookmark();
            bookmark.path = it[0];
            bookmark.text = it[1];
            bookmark.t = Long.parseLong(it[4]);
            if (it.length > 5) {
                bookmark.p = Float.parseFloat(it[5]);
            }

            BookmarksData.get().add(bookmark);

        }


    }

}
