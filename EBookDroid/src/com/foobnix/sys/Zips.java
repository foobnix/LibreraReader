package com.foobnix.sys;

public class Zips {

    public static ZipArchiveInputStream buildZipArchiveInputStream(String file) {
        return new ZipArchiveInputStream(file);
    }

}
