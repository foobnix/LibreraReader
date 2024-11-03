package com.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.foobnix.android.utils.LOG;

public class Prefs {

    static Prefs instance = new Prefs();

    public static synchronized Prefs get() {
        return instance;
    }

    Context c;
    SharedPreferences sp;

    public void init(Context c) {
        sp = c.getSharedPreferences("TextErrors", Context.MODE_PRIVATE);
    }

    public void put(String path, int page) {
        sp.edit().putBoolean(makeHash(path, page), true).commit();
    }

    @NonNull
    private String makeHash(String path, int page) {
        return "" + path.hashCode() + page;
    }

    public boolean isErrorExist(String path, int page) {
        boolean isErrorExist = sp.contains(makeHash(path, page));
        LOG.d("isErrorExist", isErrorExist, path + page);
        return isErrorExist;
    }

    public void remove(String path, int page) {
        sp.edit().remove(makeHash(path, page)).commit();
    }

    // Add methods to store and retrieve progress tracking data
    public void putProgress(String bookId, int progress) {
        sp.edit().putInt("progress_" + bookId, progress).commit();
    }

    public int getProgress(String bookId) {
        return sp.getInt("progress_" + bookId, 0);
    }

    // Add methods to store and retrieve reminder settings
    public void putReminder(String reminderId, long time) {
        sp.edit().putLong("reminder_" + reminderId, time).commit();
    }

    public long getReminder(String reminderId) {
        return sp.getLong("reminder_" + reminderId, 0);
    }
}
