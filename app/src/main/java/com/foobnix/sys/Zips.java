package com.foobnix.sys;

public class Zips {

    public static ZipArchiveInputStream buildZipArchiveInputStream(String file) {
        final ZipArchiveInputStream zip = new ZipArchiveInputStream(file);
        return zip;
    }

}
