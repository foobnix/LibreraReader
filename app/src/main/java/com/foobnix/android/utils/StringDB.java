package com.foobnix.android.utils;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StringDB {
    public static String DIVIDER = ",";

    public static void add(String db, String text, final StringResult result) {
        if (TxtUtils.isEmpty(text)) {
            result.onResult(db);
            return;
        }
        if (db == null) {
            db = "";
        }
        if (!db.endsWith(DIVIDER)) {
            db += DIVIDER;
        }

        text = text.replace(DIVIDER, "").trim();
        db = db + text + DIVIDER;
        result.onResult(db);
    }

    public static List<Integer> converToColor(String db, int last) {
        List<Integer> res = converToColor(db);
        res.add(last);
        return res;

    }

    public static List<Integer> converToColor(String db) {
        List<Integer> colors = new ArrayList<Integer>();
        for (String color : db.split(DIVIDER)) {
            try {
                colors.add(Color.parseColor(color.trim()));
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        return colors;
    }

    public static String merge(String db1, String db2) {
        if (TxtUtils.isEmpty(db1)) {
            return db2;
        }

        if (TxtUtils.isEmpty(db2)) {
            return db1;
        }

        Set<String> res = new LinkedHashSet<String>();
        res.addAll(asList(db1));
        res.addAll(asList(db2));
        String join = TxtUtils.joinList(DIVIDER, res) + DIVIDER;
        LOG.d("MergeTags", db1, db2, ">>", join);
        return join;

    }

    public static void delete(String db, String text, final StringResult result) {
        if (TxtUtils.isEmpty(text)) {
            result.onResult(db);
            return;
        }
        db = db.replace(text + DIVIDER, "");
        result.onResult(db);
    }

    public static boolean contains(String db, String tag) {
        if (TxtUtils.isEmpty(db) || TxtUtils.isEmpty(tag)) {
            return false;
        }
        return db.startsWith(tag + DIVIDER) || db.contains(DIVIDER + tag + DIVIDER);
    }

    public static List<String> asList(String db) {
        if (TxtUtils.isEmpty(db)) {
            return new ArrayList<String>();
        }
        db = TxtUtils.replaceLast(db, DIVIDER, "");
        return new ArrayList<String>(Arrays.asList(db.split(",")));
    }

    public static String filter(String trim) {
        return TxtUtils.replaceLast(trim, DIVIDER, "");
    }

}
