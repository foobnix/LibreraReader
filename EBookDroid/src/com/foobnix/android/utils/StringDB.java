package com.foobnix.android.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.graphics.Color;

public class StringDB {
    public static String DIVIDER = ",";

    public static String add(String db, String text) {
        if (TxtUtils.isEmpty(text)) {
            return db;
        }
        text = text.replace(DIVIDER, "").trim();
        db = db + text + DIVIDER;
        return db;
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

    public static String delete(String db, String text) {
        if (TxtUtils.isEmpty(text)) {
            return db;
        }
        db = db.replace(text + DIVIDER, "");
        return db;
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

}
