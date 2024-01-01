package com.foobnix.android.utils;

public class MyMath {
    public static float percent(int page, int pages) {
        LOG.d("MyMath", "percent", page, pages);
        return (float) page / pages;
    }

    public static long longValueOfNoException(String number) {
        if (TxtUtils.isEmpty(number)) {
            return 0;
        }
        try {
            return Long.parseLong(number);
        } catch (Exception e) {
            LOG.e(e);
        }
        return 0;
    }
}
