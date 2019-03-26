package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.AppDB;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class TagData {


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


    public static void saveTags(FileMeta meta) {
        saveTags(meta.getPath(), meta.getTag());
    }

    public static void saveTags(String path, String tags) {
        try {
            JSONObject obj = IO.readJsonObject(AppProfile.syncTags);
            obj.put(MyPath.toRelative(path), tags);
            IO.writeObjAsync(AppProfile.syncTags, obj);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void restoreTags() {
        LOG.d("restoreTags");

        JSONObject obj = IO.readJsonObject(AppProfile.syncTags);

        final Iterator<String> keys = obj.keys();

        while (keys.hasNext()) {
            final String key = keys.next();

            try {

                Tag tag = new Tag(key, obj.getString(key));
                LOG.d("restoreTags",tag.path, tag.tags);

                FileMeta load = AppDB.get().load(tag.getPath());
                if (load != null) {
                    load.setTag(tag.tags);
                    LOG.d("restoreTags", tag.getPath(), tag.tags);
                    AppDB.get().update(load);
                }
            } catch (JSONException e) {
                LOG.e(e);
            }

        }


    }

}
