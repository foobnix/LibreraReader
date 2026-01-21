package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TagData {


//    public static void saveTags(FileMeta meta) {
//        saveTags(meta.getPath(), meta.getTag());
//    }
//
//    public static void saveTags(String path, String tags) {
//        try {
//            LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags);
//            obj.put(MyPath.toRelative(path), tags);
//            IO.writeObjAsync(AppProfile.syncTags, obj);
//            LOG.d("saveTags", tags, path);
//            restoreTags();
//        } catch (Exception e) {
//            LOG.e(e);
//        }
//    }

//    public static String getTags(String path) {
//        try {
//            LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags);
//            return obj.getString(MyPath.toRelative(path));
//        } catch (Exception e) {
//            LOG.e(e);
//        }
//        return "";
//    }

//    public static List<String> getAllTagsByFile() {
//        Set<String> all = new HashSet<>();
//        for (File file : AppProfile.getAllFiles(AppProfile.APP_TAGS_JSON)) {
//            LinkedJSONObject obj = IO.readJsonObject(file);
//            final Iterator<String> keys = obj.keys();
//            while (keys.hasNext()) {
//                final String key = keys.next();
//                String tags =  obj.getString(key);
//                List<String> ids = StringDB.asList(tags);
//                all.addAll(ids);
//            }
//        }
//        return new ArrayList<>(all);
//
//    }

    public static void restoreTags() {
        if(true){
            return;
        }
        LOG.d("restoreTags");

        final List<FileMeta> allWithTag = AppDB.get().getAllWithTag();
        for (FileMeta m : allWithTag) {
            m.setTag(null);
        }
        AppDB.get().updateAll(allWithTag);


        for (File file : AppProfile.getAllFiles(AppProfile.APP_TAGS_JSON)) {
            LinkedJSONObject obj = IO.readJsonObject(file);

            final Iterator<String> keys = obj.keys();

            while (keys.hasNext()) {
                final String key = keys.next();

                try {

                    Tag tag = new Tag(key, obj.getString(key));
                    LOG.d("restoreTags-in", tag.path, tag.tags);
                    if(TxtUtils.isEmpty(tag.tags) || tag.tags.equals(",")){
                        continue;
                    }


                    FileMeta load = AppDB.get().getOrCreate(tag.getPath());

                    if (load.getTag() != null) {
                        load.setTag(StringDB.merge(load.getTag(), tag.tags));
                        LOG.d("restoreTags-do-merge", tag.getPath(), load.getTag());
                    } else {
                        load.setTag(tag.tags);
                        LOG.d("restoreTags-do", tag.getPath(), tag.tags);

                    }
                    load.setIsSearchBook(true);
                    AppDB.get().update(load);


                } catch (JSONException e) {
                    LOG.e(e);
                }

            }
        }
        AppDB.get().clearSession();


    }

    public static class Tag implements MyPath.RelativePath {
        public String path;
        public String tags;

        public Tag() {
        }

        public Tag(String path, String tags) {
            this.path = MyPath.toRelative(path);
            this.tags = tags;
        }

        public String getPath() {
            return MyPath.toAbsolute(path);
        }

        public void setPath(String path) {
            this.path = MyPath.toRelative(path);
        }
    }

}
