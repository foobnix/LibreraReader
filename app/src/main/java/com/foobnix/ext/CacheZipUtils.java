package com.foobnix.ext;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.mobi.parser.IOUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.sys.ArchiveEntry;
import com.foobnix.sys.ZipArchiveInputStream;

import net.lingala.zip4j.model.FileHeader;

import org.ebookdroid.BookType;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CacheZipUtils {
    public static final Lock cacheLock = new ReentrantLock();
    public static final Lock cacheLock2 = new ReentrantLock();
    private static final int BUFFER_SIZE = 16 * 1024;

    public static File CACHE_BOOK_DIR;
    public static File CACHE_WEB;
    public static File CACHE_RECENT;
    public static File CACHE_TEMP;
    public static File ATTACHMENTS_CACHE_DIR;
    public static File CACHE_SAF_BOOKS;
    static Pair<Boolean, String> cacheRes;
    static String cacheFile;

    public static void init(Context c) {
        File externalCacheDir = c.getExternalCacheDir();
        if (externalCacheDir == null) {
            externalCacheDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        }
        CacheDir.parent = externalCacheDir;

        CACHE_BOOK_DIR = new File(externalCacheDir, "Book");
        ATTACHMENTS_CACHE_DIR = new File(externalCacheDir, "Attachments");
        CACHE_WEB = new File(externalCacheDir, "WEB");
        CACHE_RECENT = new File(externalCacheDir, "Recent");
        CACHE_TEMP = new File(externalCacheDir, "Temp");
        CACHE_SAF_BOOKS = new File(externalCacheDir, "SafBooks");

        CacheZipUtils.createAllCacheDirs();
        CacheDir.createCacheDirs();
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
        if (!CACHE_TEMP.exists()) {
            CACHE_TEMP.mkdirs();
        }
        if (!CACHE_SAF_BOOKS.exists()) {
            CACHE_SAF_BOOKS.mkdirs();
        }
    }

    public static void emptyAllCacheDirs() {
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles());
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_TEMP.listFiles());
        CacheZipUtils.removeFiles(CacheZipUtils.ATTACHMENTS_CACHE_DIR.listFiles());
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_WEB.listFiles());
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_RECENT.listFiles());
    }

    // Where a cover is read out of a storage book that is not the one being read
    private static final String SAF_COVER_DIR = ".cover";

    /**
     * A book on added storage is reached through a document provider, and the readers want a file
     * to open it. So the book being read is copied here, and that copy is what is read.
     * <p>
     * The copy sits in a folder of its own named after the document and keeps the book's own name
     * inside it. Both matter. The readers pick themselves by the extension, so the name has to be
     * the book's; and the path has to be the same every time, because what the reader records
     * about a book - what you marked, the engines' own caches - is filed under the path it was
     * opened from. The position is filed under the book's name, so it outlives the copy.
     * <p>
     * Only the book opened last is kept: a library on added storage would otherwise end up stored
     * twice over. The copy outlives {@link #emptyAllCacheDirs()}, which runs as a book is opened,
     * so reopening the book being read costs nothing.
     */
    public static File getSafBookCopy(Context c, String uri, String displayName) {
        if (CACHE_SAF_BOOKS == null || TxtUtils.isEmpty(displayName)) {
            return null;
        }
        try {
            File dir = new File(CACHE_SAF_BOOKS, Integer.toHexString(uri.hashCode()));
            File dest = new File(dir, displayName);

            keepOnlySafBook(dir);

            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            // Which document the copy is of, so the history can name the book rather than the
            // copy: the copy goes when another book is opened, and with the app
            IO.writeString(new File(dir, SAF_URI_FILE), uri);

            if (dest.isFile() && dest.length() > 0) {
                return dest;
            }

            if (!copySafTo(c, uri, dest)) {
                return null;
            }
            LOG.d("saf-book-copy", dest.getPath(), dest.length());
            return dest;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * The file a cover of a storage book is read from: the copy of the book being read if it is
     * that book, otherwise a copy of its own to be thrown away with {@link #removeSafCoverCopy}
     * once the cover is out. Either way it is under the copies' folder, so
     * {@link #isSafBookCopy(String)} keeps it out of the library.
     */
    public static File getSafCoverCopy(Context c, String uri, String displayName) {
        if (CACHE_SAF_BOOKS == null || TxtUtils.isEmpty(displayName)) {
            return null;
        }
        try {
            File reading = new File(new File(CACHE_SAF_BOOKS, Integer.toHexString(uri.hashCode())), displayName);
            if (reading.isFile() && reading.length() > 0) {
                return reading;
            }
            File dir = new File(CACHE_SAF_BOOKS, SAF_COVER_DIR);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            File dest = new File(dir, Integer.toHexString(uri.hashCode()) + "-" + displayName);
            return copySafTo(c, uri, dest) ? dest : null;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    /** Throws away a copy made for a cover; the copy of the book being read is left alone. */
    public static void removeSafCoverCopy(File copy) {
        if (copy != null && copy.getParentFile() != null && SAF_COVER_DIR.equals(copy.getParentFile().getName())) {
            copy.delete();
        }
    }

    /**
     * A copy of a storage document meant to be read once and thrown away with
     * {@link #removeSafScanCopy(File)} - what the library scan reads a book's metadata out
     * of. Unlike {@link #getSafBookCopy} nothing is kept, so a scan of a thousand books costs one
     * book of disk rather than all of them.
     */
    public static File getSafScanCopy(Context c, String uri, String displayName) {
        if (CACHE_TEMP == null || TxtUtils.isEmpty(displayName)) {
            return null;
        }
        try {
            File dir = new File(CACHE_TEMP, "SafScan");
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            File dest = new File(dir, displayName);
            return copySafTo(c, uri, dest) ? dest : null;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public static void removeSafScanCopy(File copy) {
        try {
            if (copy != null) {
                copy.delete();
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    // Written beside the copy and moved into place, so a copy that was cut short is never taken
    // for a book.
    private static boolean copySafTo(Context c, String uri, File dest) throws Exception {
        InputStream in = c.getContentResolver().openInputStream(Uri.parse(uri));
        if (in == null) {
            return false;
        }
        File part = new File(dest.getParentFile(), dest.getName() + ".part");
        copyFile(in, part);
        if (part.renameTo(dest)) {
            return true;
        }
        part.delete();
        return false;
    }

    private static final String SAF_URI_FILE = ".uri";

    /** The document a copy made by {@link #getSafBookCopy} is of, or null when it is not known. */
    public static String getSafUriOfCopy(String path) {
        if (!isSafBookCopy(path)) {
            return null;
        }
        try {
            File file = new File(new File(path).getParentFile(), SAF_URI_FILE);
            if (!file.isFile()) {
                return null;
            }
            String uri = IO.readString(file).trim();
            return TxtUtils.isEmpty(uri) ? null : uri;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public static boolean isSafBookCopy(String path) {
        return path != null && CACHE_SAF_BOOKS != null && path.startsWith(CACHE_SAF_BOOKS.getPath());
    }

    public static void removeSafBookCopies() {
        if (CACHE_SAF_BOOKS == null) {
            return;
        }
        File[] dirs = CACHE_SAF_BOOKS.listFiles();
        if (dirs == null) {
            return;
        }
        for (File dir : dirs) {
            deleteDir(dir);
        }
    }

    // Every other book's copy goes; the covers' folder empties itself as each cover is read
    private static void keepOnlySafBook(File keep) {
        File[] dirs = CACHE_SAF_BOOKS.listFiles();
        if (dirs == null) {
            return;
        }
        for (File dir : dirs) {
            if (!dir.getName().equals(keep.getName()) && !dir.getName().equals(SAF_COVER_DIR)) {
                deleteDir(dir);
            }
        }
    }

    public static void savaJavaCache(Object object, File file) {
        try {
            LOG.d("JavaCache save", object, file.getName());
            FileOutputStream fos = new FileOutputStream(new File(CACHE_TEMP, makeKey(file)));
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.flush();
            oos.close();
            LOG.d("JavaCache save success");

            File[] files = CACHE_TEMP.listFiles();
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            if (files.length > 50) {
                files[0].delete();
                LOG.d("JavaCache save delete", files[0]);
            }

        } catch (Exception e) {
            LOG.d("JavaCache save error");
            LOG.e(e);
        }
    }

    public static Object loadJavaCache(File file1) {
        try {

            File file = new File(CACHE_TEMP, makeKey(file1));
            LOG.d("JavaCache load exist", file.isFile(), file);
            if (!file.isFile()) {
                return null;
            }
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Object res = ois.readObject();
            ois.close();
            LOG.d("JavaCache load success");
            return res;
        } catch (Exception e) {
            LOG.d("JavaCache load error");
            LOG.e(e);
        }
        return null;
    }

    @NonNull
    /**
     * Drops every cached table of contents. The outlines are serialized per book with their pages
     * already resolved, and those page numbers came from whichever engine laid the book out - so
     * they have to go when the engine changes, or the contents panel jumps to the wrong pages.
     */
    public static void removeOutlineCache() {
        final File[] files = CACHE_TEMP == null ? null : CACHE_TEMP.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file != null && file.getName().endsWith("-outline")) {
                LOG.d("JavaCache drop", file.getName());
                file.delete();
            }
        }
    }

    private static String makeKey(File file) {
        if (file == null) {
            return "no-file-outline";
        }
        return "" + MagicHelper.hash() + file.lastModified() + file.getName() + "-outline";
    }



    public static boolean removeFiles(File[] files) {
        try {
            if (files == null) {
                return false;
            }
            for (File file : files) {
                if (file != null) {
                    boolean result = file.delete();
                    LOG.d("removeFile", file,result);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }
        return true;
    }

    public static void removeFiles(File[] files, File exept) {
        try {
            if (files == null || exept == null) {
                return;
            }
            for (File file : files) {
                if (file.getName().equals("outline")) {
                    continue;
                }

                if (file != null && !file.getName().startsWith(exept.getName())) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void removeDirs(File[] files, File exept) {
        try {
            if (files == null || exept == null) {
                return;
            }
            for (File file : files) {
                if (file != null && !file.getName().startsWith(exept.getName())) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                        LOG.d("Delete-dir", file);
                    }
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static Pair<Boolean, String> isSingleAndSupportEntry(String file) {
        if (file.equals(cacheFile)) {
            LOG.d("isSingleAndSupportEntry-cache", file);
            return cacheRes;
        }
        final Pair<Boolean, String> res = isSingleAndSupportEntryInner(file);
        cacheFile = file;
        cacheRes = res;
        return res;
    }

    public static Pair<Boolean, String> isSingleAndSupportEntryInner(String file) {
        try {
            LOG.d("isSingleAndSupportEntry 1", file);
            if (!CbzCbrExtractor.isZip(file)) {
                return new Pair<Boolean, String>(false, "");
            }

            net.lingala.zip4j.ZipFile zp = new net.lingala.zip4j.ZipFile(file);
//            if(!zp.isValidZipFile()){
//                return new Pair<Boolean, String>(false, "");
//            }
            List<FileHeader> fileHeaders = zp.getFileHeaders();

            int count = 0;
            FileHeader last = null;
            boolean isOkular = BookType.OKULAR.is(file);
            for (FileHeader h : fileHeaders) {
                if (h.isDirectory()) {
                    continue;
                }
                String ends = h.getFileName();
                if (isOkular && ends.endsWith(".xml")) {
                    continue;
                }
                if (ends.endsWith(".opf") || ExtUtils.isImagePath(ends) || ends.endsWith(".fbd")) {
                    continue;
                }

                count++;
                last = h;
                LOG.d("isSingleAndSupportEntryInner finds", ends);
            }
            if (count == 1 && last != null) {
                String name = last.getFileName();
                LOG.d("isSingleAndSupportEntryInner name", name);

                return new Pair<Boolean, String>(BookType.isSupportedExtByPath(name), name);
            }
            return new Pair<Boolean, String>(false, "");
        } catch (Exception e) {
            return new Pair<Boolean, String>(false, "");
        }
    }

    public static UnZipRes extracIfNeed(String path, CacheDir folder) {
        return extracIfNeed(path, folder, -1);

    }

    public static UnZipRes extracIfNeed(String path, CacheDir folder, long salt) {
        if (!ExtUtils.isZip(path)) {
            return new UnZipRes(path, path, null);
        }

        folder.removeCacheContent();

        try {
            if (!isSingleAndSupportEntry(path).first) {
                return new UnZipRes(path, path, null);
            }
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(path);

            ArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (BookType.isSupportedExtByPath(nextEntry.getName())) {
                    String name = nextEntry.getName();
                    if (salt != -1) {
                        name = salt + name;
                    }
                    File file = new File(TxtUtils.fixFilePath(folder.getDir().getPath()), TxtUtils.fixFileName(name));
                    file.getParentFile().mkdirs();

                    BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                    LOG.d("Unpack archive", file.getPath());
                    IOUtils.copyClose(zipInputStream, fileOutputStream);
                    return new UnZipRes(path, file.getPath(), nextEntry.getName());
                }
            }
            zipInputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return new UnZipRes(path, path, null);
    }

    public static boolean extractArchive(File fromFile, File toDir) {
        try {
            LOG.d("extractArchive From:", fromFile, fromFile.isFile());
            LOG.d("extractArchive To:  ", toDir);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(fromFile.getPath());

            ArchiveEntry nextEntry = null;
            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (nextEntry.isDirectory()) {
                    continue;
                }
                String name = ExtUtils.getFileName(nextEntry.getName());
                File file = new File(toDir, name);
                LOG.d("extractArchive", file.getName());
                BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
                IOUtils.copyClose(zipInputStream, fileOutputStream);
            }
            zipInputStream.close();
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

    @Deprecated
    Pair<Boolean, String> isSingleAndSupportEntry(ZipArchiveInputStream zipInputStream) {
        String name = "";
        try {

            boolean find = false;
            ArchiveEntry nextEntry = null;

            while ((nextEntry = zipInputStream.getNextEntry()) != null) {
                if (nextEntry.isDirectory()) {
                    continue;
                }

                name = nextEntry.getName();
                if (find) {
                    zipInputStream.close();
                    return new Pair<Boolean, String>(false, "");
                }
                find = true;
            }
            zipInputStream.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return new Pair<Boolean, String>(BookType.isSupportedExtByPath(name), name);
    }

    public enum CacheDir {
        ZipApp("ZipApp"), //
        ZipService("ZipService"); //

        public static File parent;
        private final String type;

        private CacheDir(String type) {
            this.type = type;
        }

        public static void createCacheDirs() {
            for (CacheDir folder : values()) {
                File root = new File(parent, folder.getType());
                if (!root.exists()) {
                    root.mkdirs();
                }
            }
        }

        public String getType() {
            return type;
        }

        public void removeCacheContent() {
            try {
                removeFiles(getDir().listFiles());
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        public File getDir() {
            File file = new File(parent, type);
            return file;
        }

    }

    public static class FilerByDate implements Comparator<File> {
        @Override
        public int compare(final File lhs, final File rhs) {
            return Long.compare(lhs.lastModified(), rhs.lastModified());
        }
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
}
