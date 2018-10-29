package com.foobnix.sys;

import java.io.InputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public class Zips {

    public static ZipArchiveInputStream buildZipArchiveInputStream(InputStream inputStream) {
        return new ZipArchiveInputStream(inputStream, null, true, true);
    }

}
