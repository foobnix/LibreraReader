package com.foobnix.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.foobnix.android.utils.Objects;

public class AppTemp {

    private static AppTemp instance = new AppTemp();

    public static AppTemp get() {
        return instance;
    }

    public String lastBookPath;
    public String lastClosedActivity;
    public String lastMode;
    public long searchDate = 0;
    public int lastBookPage = 0;
    public int tempBookPage = 0;
    public volatile int lastBookParagraph = 0;
    public String lastBookTitle;
    public int lastBookWidth = 0;
    public int lastBookHeight = 0;
    public int lastFontSize = 0;
    public String lastBookLang = "";

    transient  SharedPreferences sp;

    public void init(Context c){
         sp = c.getSharedPreferences("AppTemp", Context.MODE_PRIVATE);
         load();
    }

    public void load(){
        Objects.loadFromSp(instance, sp);

    }

    public void save(){
        Objects.saveToSP(instance, sp);

    }


}
