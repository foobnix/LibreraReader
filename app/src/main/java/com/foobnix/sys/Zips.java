package com.foobnix.sys;

import com.foobnix.android.utils.LOG;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;


public class Zips {

    public static ZipArchiveInputStream buildZipArchiveInputStream(String file) {
        final ZipArchiveInputStream zip = new ZipArchiveInputStream(file);
        return zip;
    }

    public static void test(File file) throws IOException {
        //case 1
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            LocalFileHeader nextEntry = zis.getNextEntry();
            while (nextEntry != null) {
                LOG.d("Zip-1", nextEntry.getFileName());
                nextEntry = zis.getNextEntry();
            }
            zis.close();
        } catch (Exception e) {
            LOG.e(e, "Zip-1");
        }
        //case 2
        ZipFile zipFile = new ZipFile(file);
        List<FileHeader> fileHeaders = zipFile.getFileHeaders();
        for (FileHeader h : fileHeaders) {
            LOG.d("Zip-2", h.getFileName());
        }


    }

}
