package com.foobnix.android.utils;

public class MyMath {
    public static float percent(int page, int pages) {
        LOG.d("MyMath", "percent", page, pages);
        return (float) page / pages;
    }
}
