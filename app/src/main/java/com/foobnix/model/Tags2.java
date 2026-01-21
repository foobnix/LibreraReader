package com.foobnix.model;

import androidx.core.util.Pair;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StringDB;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.AppDB;

import org.librera.JSONArray;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Tags2 {

    public static void createTag(String name) {
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        obj.put(name, new JSONArray());
        IO.writeObjAsync(AppProfile.syncTags2, obj);
        LOG.d("Tags2", "createTag", name);
    }

    public static void deleteTag(String name) {
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        obj.remove(name);
        IO.writeObjAsync(AppProfile.syncTags2, obj);
        LOG.d("Tags2", "deleteTag", name);
    }

    public static List<String> getAllTags() {
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        LOG.d("Tags2", "getAllTags", obj.length());
        ArrayList<String> res = new ArrayList<>(obj.keySet());
        Collections.sort(res, String.CASE_INSENSITIVE_ORDER);
        return res;
    }

    public static List<Pair<String, Integer>> getAllTagsWithCount() {
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        LOG.d("Tags2", "getAllTags", obj.length());
        ArrayList<Pair<String, Integer>> res = new ArrayList<>();
        for (String key : obj.keySet()) {
            res.add(new Pair<>(key, obj.getJSONArray(key)
                                       .length()));
        }

        return res;
    }

    public static List<String> getAllTagsByFile(File file) {
        if (file == null) {
            return Collections.emptyList();
        }
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        List<String> res = new ArrayList<>();
        for (String key : obj.keySet()) {
            JSONArray array = obj.getJSONArray(key);
            int index = getIndex(array, file.getPath());
            if (index >= 0) {
                res.add(key);
            }
        }
        return res;
    }

    public static List<String> getAllFilesByTag(String tag) {
        if (tag == null) {
            return Collections.emptyList();
        }
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        JSONArray array = obj.optJSONArray(tag);
        if(array==null){
            return Collections.emptyList();
        }


        return array.toList()
                    .stream()
                    .map(o -> (String) o)
                    .collect(Collectors.toList());
    }

    public static void setTags(File file, List<String> tags) {
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        for (String allTag : obj.keySet()) {
            boolean isCurrentTag = tags.contains(allTag);
            JSONArray array = obj.getJSONArray(allTag);
            int index = getIndex(array, file.getPath());

            if (isCurrentTag) {
                if (index < 0) {
                    array.put(file.getPath());
                }
            } else {
                array.remove(index);
            }

        }
        IO.writeObjAsync(AppProfile.syncTags2, obj);
        LOG.d("Tags2", "addTags", file, tags);
    }

    public static int getIndex(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            if (array.getString(i)
                     .equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public static void updateTagsDB() {
        LOG.d("Tags2", "updateTagsDB");
        final List<FileMeta> allWithTag = AppDB.get()
                                               .getAllWithTag();
        for (FileMeta m : allWithTag) {
            m.setTag(null);
        }
        AppDB.get()
             .updateAll(allWithTag);

        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        Map<File, Set<String>> mapTags = new LinkedHashMap<>();
        for (String tag : obj.keySet()) {
            for (Object fileO : obj.getJSONArray(tag)
                                   .toList()) {
                mapTags.computeIfAbsent(new File(fileO.toString()), k -> new HashSet<>())
                       .add(tag);
            }
        }
        for (File file : mapTags.keySet()) {
            FileMeta load = AppDB.get()
                                 .getOrCreate(file.getPath());
            load.setIsSearchBook(true);
            String tags = mapTags.get(file)
                                 .stream()
                                 .collect(Collectors.joining(","));
            load.setTag(tags + ",");
            LOG.d("Tags2", "set", tags, load.getPath());
            AppDB.get()
                 .updateUpdate(load);
        }
    }

}
