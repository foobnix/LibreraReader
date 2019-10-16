package com.foobnix.android.utils;

import android.app.Activity;
import android.database.Cursor;

public class Cursors {

    public static String getValue(Activity a, String key) {
        Cursor cursor = null;
        try {
            cursor = a.getContentResolver().query(a.getIntent().getData(), new String[]{key}, null, null, null);
            cursor.moveToFirst();
            return cursor.getColumnCount() > 0 ? cursor.getString(0) : null;
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
