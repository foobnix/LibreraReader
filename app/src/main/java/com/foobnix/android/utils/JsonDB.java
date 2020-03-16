package com.foobnix.android.utils;

import android.text.Spanned;

import com.foobnix.model.MyPath;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JsonDB {


    public static Spanned fromHtml(String db) {
        StringBuilder res = new StringBuilder();
        for (String item : get(db)) {
            res.append(item.replace(MyPath.INTERNAL_ROOT, "...") + "<br>");
        }
        String text = res.toString();
        text = TxtUtils.replaceLast(text, "<br>", "");
        return TxtUtils.fromHtml(text);
    }

    public static String set(List<String> list) {
        JSONArray array = new JSONArray();
        for (String s : list) {
            array.put(s);
        }
        return array.toString();
    }

    public static boolean contains(String db, String item) {
        return get(db).contains(item);
    }

    public static String add(String db, String line) {
        final List<String> list = get(db);
        list.add(line);
        return set(list);
    }

    public static String remove(String db, String line) {
        final List<String> list = get(db);
        list.remove(line);
        return set(list);
    }

    public static boolean isEmpty(String db) {
        return get(db).isEmpty();
    }

    public static List<String> get(String db) {
        try {

            List<String> res = new ArrayList<>();
            JSONArray array = TxtUtils.isEmpty(db) ? new JSONArray() : new JSONArray(db);
            for (int i = 0; i < array.length(); i++) {
                res.add(array.getString(i));
            }
            Collections.sort(res);
            return res;
        } catch (Exception e) {
            LOG.e(e);

        }
        return Collections.emptyList();
    }
}
