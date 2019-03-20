package com.foobnix.model;

import android.os.Environment;

import java.io.File;

public class MyFile extends File {

    final static String INTERNAL_ROOT = Environment.getExternalStorageDirectory().getPath();
    final static String INTERNAL_PREFIX = "internal-storage:";


    public MyFile(String path) {
        super(toAbsolute(path));
    }


    public static String toRelative(String path) {
        return path.replace(INTERNAL_ROOT, INTERNAL_PREFIX);
    }

    public static String toAbsolute(String path) {
        return path.replace(INTERNAL_PREFIX, INTERNAL_ROOT);
    }

    public  interface  RelativePath {
        String getPath();

        void setPath(String path);
    }
}
