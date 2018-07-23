package com.foobnix.ext;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.ebookdroid.BookType;
import org.ebookdroid.common.cache.CacheManager;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ExtUtils;

import android.content.Context;
import android.os.Environment;
import android.support.v4.util.Pair;

public class CacheZipUtils {
    private static final int BUFFER_SIZE = 16 * 1024;

    public enum CacheDir {
        ZipApp("ZipApp"), //
        ZipService("ZipService"); //

        private final String type;

        private CacheDir(String type) {
            this.type = type;
        }

        public static File parent;

        public String getType() {
            return type;
        }

        public static void createCacheDirs() {
            for (CacheDir folder : values()) {
                File root = new File(parent, folder.getType());
                if (!root.exists()) {
                    root.mkdirs();
                }
            }
        }

        public void removeCacheContent() {
            try {
                removeFiles(getDir().listFiles());
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        public File getDir() {
            return new File(parent, type);
        }

    }

    public static File CACHE_UN_ZIP_DIR;
    public static File CACHE_BOOK_DIR;
    public static File CACHE_WEB;
    public static File ATTACHMENTS_CACHE_DIR;
    public static final Lock cacheLock = new ReentrantLock();

    public static void init(Context c) {
        File externalCacheDir = c.getExternalCacheDir();
        if (externalCacheDir == null) {
            externalCacheDir = c.getCacheDir();
        }
        if (externalCacheDir == null) {
            externalCacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
        CacheDir.parent = externalCacheDir;

        CACHE_BOOK_DIR = new File(externalCacheDir, "Book");
        CACHE_UN_ZIP_DIR = new File(externalCacheDir, "UnZip");
        ATTACHMENTS_CACHE_DIR = new File(externalCacheDir, "Attachments");
        CACHE_WEB = new File(externalCacheDir, "WEB");

        CacheZipUtils.createAllCacheDirs();
        CacheDir.createCacheDirs();
        CacheManager.clearAllTemp();
    }

    public static void createAllCacheDirs() {
        if (!CACHE_BOOK_DIR.exists()) {
            CACHE_BOOK_DIR.mkdirs();
        }
        if (!ATTACHMENTS_CACHE_DIR.exists()) {
            ATTACHMENTS_CACHE_DIR.mkdirs();
        }
        if (!CACHE_WEB.exists()) {
            CACHE_WEB.mkdirs();
        }
    }

    public static class FilerByDate implements Comparator<File> {
        @Override
        public int compare(final File lhs, final File rhs) {
            return new Long(lhs.lastModified()).compareTo(new Long(rhs.lastModified()));
        }
    }

    public static void clearBookDir() {
        List<File> asList = Arrays.asList(CACHE_BOOK_DIR.listFiles());

        int cacheSize = 3;
        if (asList.size() <= cacheSize) {
            return;
        }

        Collections.sort(asList, new FilerByDate());

        for (int i = cacheSize; i < asList.size(); i++) {
            File file = asList.get(i);
            LOG.d("Remove file", file.getName());
            file.delete();
        }
    }

    public static void removeFiles(File[] files) {
        try {
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file != null) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void removeFiles(File[] files, File exept) {
        try {
            if (files == null || exept == null) {
                return;
            }
            for (File file : files) {
                if (file != null && !file.getName().startsWith(exept.getName())) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static Pair<Boolean, String> isSingleAndSupportEntryFile(File file) {
        try {
            return isSingleAndSupportEntry(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            return new Pair<Boolean, String>(false, "");
        }
    }

    public static Pair<Boolean, String> isSingleAndSupportEntry(InputStream is) {
        if (is == null) {
            return new Pair<Boolean, String>(false, "");
        }
        String name = "";
        try {
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(is, "cp1251");
            boolean find = false;
            ZipArchiveEntry nextEntry = null;

            while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                name = nextEntry.getName();
                if (find) {
                    zipInputStream.close();
                    is.close();
                    return new Pair<Boolean, String>(false, "");
                }
                find = true;
            }
            zipInputStream.close();
            is.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return new Pair<Boolean, String>(BookType.isSupportedExtByPath(name), name);
    }

    public static class UnZipRes {
        public String originalPath;
        public String unZipPath;
        public String entryName;

        public UnZipRes(String originalPath, String unZipPath, String entryName) {
            this.originalPath = originalPath;
            this.unZipPath = unZipPath;
            this.entryName = entryName;
        }
    }

    public static UnZipRes extracIfNeed(String path, CacheDir folder) {
        return extracIfNeed(path, folder, -1);

    }

    public static UnZipRes extracIfNeed(String path, CacheDir folder, long salt) {
        if (!path.endsWith(".zip")) {
            return new UnZipRes(path, path, null);
        }

        folder.removeCacheContent();

        try {
            InputStream in = new FileInputStream(new File(path));
            if (!isSingleAndSupportEntry(in).first) {
                return new UnZipRes(path, path, null);
            }
            in = new FileInputStream(new File(path));
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(in, "cp1251");

            ZipArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                if (BookType.isSupportedExtByPath(nextEntry.getName())) {
                    String name = nextEntry.getName();
                    if (salt != -1) {
                        name = salt + name;
                    }
                    File file = new File(folder.getDir(), name);
                    BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                    writeToStream(zipInputStream, fileOutputStream);
                    LOG.d("Unpack archive", file.getPath());

                    zipInputStream.close();
                    in.close();

                    return new UnZipRes(path, file.getPath(), nextEntry.getName());
                }
            }
            zipInputStream.close();
            in.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return new UnZipRes(path, path, null);
    }

    public static boolean extractArchive(File fromFile, File toDir) {
        try {
            LOG.d("extractArchive From:", fromFile);
            LOG.d("extractArchive To:  ", toDir);
            InputStream in = new FileInputStream(fromFile);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(in);

            ZipArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextZipEntry()) != null) {
                if (nextEntry.isDirectory()) {
                    continue;
                }
                String name = ExtUtils.getFileName(nextEntry.getName());
                File file = new File(toDir, name);
                LOG.d("extractArchive", file.getName());
                BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                writeToStream(zipInputStream, fileOutputStream);
            }
            zipInputStream.close();
            in.close();
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }

        return true;
    }

    public static byte[] getEntryAsByte(InputStream zipInputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
        return out.toByteArray();
    }

    public static void writeToStream(InputStream zipInputStream, OutputStream out) throws IOException {

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
    }

    static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);
        zip.setLevel(0);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    static private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();
        }
    }

    static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    public static void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static void copyFile(InputStream is, File dest) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            if (os != null) {
                os.close();
            }
        }
    }
}
