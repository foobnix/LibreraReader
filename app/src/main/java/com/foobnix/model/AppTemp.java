package com.foobnix.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.foobnix.android.utils.Objects;

public class AppTemp {

    private static AppTemp instance = new AppTemp();
    public String lastBookPath;
    public String lastClosedActivity;
    public String lastMode;
    public int lastBookPage = 0;
    public int lastBookPageCount = 0;
    public int tempBookPage = 0;
    public volatile int lastBookParagraph = 0;
    public String lastBookTitle;
    public int lastBookWidth = 0;
    public int lastBookHeight = 0;
    public int lastFontSize = 0;
    public String lastBookLang = "";
    public boolean isLocked = false;
    public boolean isFirstTimeVertical = true;
    public boolean isFirstTimeHorizontal = true;
    public int readingMode = AppState.READING_MODE_BOOK;
    public long syncTime;
    public int syncTimeStatus;
    public String hypenLang;
    public boolean isCut = false;
    public boolean isDouble = false;
    public boolean isDoubleCoverAlone = false;
    public boolean isCrop = false;
    public boolean isCropSymetry = false;
    public boolean isEnableSync;
    public String syncRootID;
    transient SharedPreferences sp;

    public static AppTemp get() {
        return instance;
    }

    public void init(Context c) {
        sp = c.getSharedPreferences("AppTemp", Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        Objects.loadFromSp(instance, sp);

    }

    public void save() {
        Objects.saveToSP(instance, sp);

    }


}
