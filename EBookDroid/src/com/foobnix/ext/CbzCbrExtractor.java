package com.foobnix.ext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.ebookdroid.BookType;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ExtUtils;

import junrar.Archive;
import junrar.rarfile.FileHeader;

public class CbzCbrExtractor {

    public static boolean isZip(String path) {
        try {
            byte[] buffer = new byte[2];
            FileInputStream is = new FileInputStream(path);
            is.read(buffer);
            is.close();
            String archType = new String(buffer);
            if ("pk".equalsIgnoreCase(archType)) {
                return true;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static int getPageCount(String path) {
        int count = 0;
        try {
            if (BookType.CBZ.is(path) || isZip(path)) {
                FileInputStream is = new FileInputStream(new File(path));

                ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(is);

                while (zipInputStream.getNextZipEntry() != null) {
                        count++;
                }
                zipInputStream.close();
                is.close();

            } else if (BookType.CBR.is(path)) {
                Archive archive = new Archive(new File(path));

                for (FileHeader it : archive.getFileHeaders()) {
                    count++;
                }

                archive.close();
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        return count;
    }

    public static byte[] getBookCover(String path) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (BookType.CBZ.is(path) || isZip(path)) {
                FileInputStream is = new FileInputStream(new File(path));

                ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(is, "cp1251");
                ZipArchiveEntry nextEntry = null;

                List<String> names = new ArrayList<String>();
                while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                    String name = nextEntry.getName();
                    LOG.d("Name", name);
                    if (ExtUtils.isImagePath(name)) {
                        names.add(name);
                    }
                }
                zipInputStream.close();
                is.close();
                Collections.sort(names);

                is = new FileInputStream(new File(path));

                zipInputStream = new ZipArchiveInputStream(is, "cp1251");
                nextEntry = null;

                String first = names.get(0);
                while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                    if (nextEntry.getName().equals(first)) {
                        CacheZipUtils.writeToStream(zipInputStream, out);
                        break;
                    }
                }
                zipInputStream.close();
                is.close();

            } else if (BookType.CBR.is(path)) {
                Archive archive = new Archive(new File(path));

                List<FileHeader> fileHeaders = archive.getFileHeaders();
                Collections.sort(fileHeaders, new Comparator<FileHeader>() {

                    @Override
                    public int compare(FileHeader o1, FileHeader o2) {
                        return o1.getFileNameString().compareTo(o2.getFileNameString());
                    }
                });

                FileHeader fileHeader = fileHeaders.get(0);

                if (fileHeader.isDirectory()) {
                    fileHeader = fileHeaders.get(1);

                }

                for (FileHeader it : fileHeaders) {
                    if (it.isDirectory()) {
                        continue;
                    }
                    String lowerCase = it.getFileNameString().toLowerCase(Locale.US);
                    if (lowerCase.contains("\\")) {
                        lowerCase = lowerCase.substring(lowerCase.indexOf("\\") + 1);
                    }

                    if (lowerCase.contains("cover")) {
                        fileHeader = it;
                        break;
                    }
                }

                LOG.d("fileHeader", fileHeader.getFileNameString());

                LOG.d("EXtract CBR", fileHeader.getFileNameString());
                archive.extractFile(fileHeader, out);
                archive.close();
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        return out.toByteArray();
    }

}
