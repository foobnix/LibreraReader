package com.foobnix.android.utils;

import java.util.Random;

public class Safe {

    public static final String TXT_SAFE_RUN = "file://SAFE_RUN-";

    static Random r = new Random();


    public static void run(final Runnable action) {

        action.run();


    }
}
