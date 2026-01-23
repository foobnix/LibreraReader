package com.foobnix.model;

import androidx.core.util.Pair;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.ui2.AppDB;

import org.greenrobot.eventbus.EventBus;
import org.librera.JSONArray;
import org.librera.LinkedJSONObject;
import org.zwobble.mammoth.internal.documents.Run;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Tags2 {

    public static void createTag(String tag) {
        if (TxtUtils.isEmpty(tag)) {
            return;
        }
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        if (!obj.has(tag)) {
            obj.put(tag, new JSONArray());
        }
        IO.writeObjAsync(AppProfile.syncTags2, obj);
        LOG.d("Tags2", "createTag", tag);
    }

    public static void deleteTag(String tag) {
        if (TxtUtils.isEmpty(tag)) {
            return;
        }
        LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags2);
        obj.remove(tag);
        IO.writeObjAsync(AppProfile.syncTags2, obj);
        LOG.d("Tags2", "deleteTag", tag);
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
            JSONArray array = obj.optJSONArray(key);
            if (array == null) {
                res.add(new Pair<>(key, 0));
                continue;
            }
            long count = array.toList()
                              .stream()
                              .filter(o -> new File(o.toString()).exists())
                              .count();
            res.add(new Pair<>(key, (int) count));
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
            JSONArray array = obj.optJSONArray(key);
            if (array == null) {
                continue;
            }
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
        if (array == null) {
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
        try {
            if (array == null || value == null) {
                return -1;
            }

            for (int i = 0; i < array.length(); i++) {
                if (value.equals(array.optString(i,null))) {
                    return i;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return -1;
    }

    public static void migration() {
        try {
            if (AppProfile.syncTags.exists() && !AppProfile.syncTags2.exists()) {
                LinkedJSONObject obj = IO.readJsonObject(AppProfile.syncTags);

                final Iterator<String> keys = obj.keys();
                Map<String, HashSet<File>> outMap = new HashMap<>();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    if (key.isEmpty()) {
                        continue;
                    }
                    final String file = MyPath.toAbsolute(key);
                    String tagsLine = obj.optString(key,"");
                    if (tagsLine.isEmpty()) {
                        continue;
                    }
                    List<String> tags = Arrays.stream(tagsLine.split(","))
                                              .map(String::trim)
                                              .filter(o -> !o.isEmpty())
                                              .collect(Collectors.toList());
                    if (tags.isEmpty()) {
                        continue;
                    }

                    tags.forEach(it -> {
                        outMap.computeIfAbsent(it, k -> new HashSet<>())
                              .add(new File(file));
                    });

                    LOG.d("migrationTag", file, tags, tags.size());
                }
                LinkedJSONObject objOut = new LinkedJSONObject();
                outMap.forEach((key, value) -> {
                    LOG.d("migrationTagRes", key, value);
                    objOut.put(key, new JSONArray(value));
                });
                IO.writeObjAsync(AppProfile.syncTags2, objOut);
                LOG.d("migrationTag", "Success");
            } else {
                LOG.d("migrationTag", "No Need");
            }
        } catch (Exception e) {
            LOG.e(e);
            if (AppsConfig.IS_LOG) {
                throw new RuntimeException(e);
            }
        }
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
            if (!file.exists()) {
                continue;
            }
            FileMeta load = AppDB.get()
                                 .getOrCreate(file.getPath());
            load.setIsSearchBook(true);
            String tags = mapTags.get(file)
                                 .stream()
                                 .collect(Collectors.joining(","));
            String tagLine = tags + ",";
            load.setTag(tagLine);
            LOG.d("Tags2", "set", tagLine, load.getPath());
            AppDB.get()
                 .updateUpdate(load);
        }

        EventBus.getDefault()
                .post(new NotifyAllFragments());
    }

}
