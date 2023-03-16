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

}
