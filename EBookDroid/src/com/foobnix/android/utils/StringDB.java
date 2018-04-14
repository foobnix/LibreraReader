package com.foobnix.android.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
