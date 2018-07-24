package com.foobnix.pdf.info.io;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.FileMetaAdapter;

public class SearchCore {
    public static boolean endWith(String name, List<String> exts) {
        for (String ext : exts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean findOnce = false;

    public static void searchSimple(List<FileMeta> items, File root, List<String> exts) {
        File[] listFiles = root.listFiles();

        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            if (file.isFile() && endWith(file.getName(), exts)) {
                items.add(new FileMeta(file.getPath()));
            }
        }
    }

    public static void search(List<FileMeta> items, File root, List<String> exts) {
        findOnce = false;
        search(root, exts, items);
    }

    private static void search(File root, List<String> exts, List<FileMeta> items) {
        if (root.isFile() && endWith(root.getName(), exts)) {
            items.add(new FileMeta(root.getPath()));
            return;
        } else if (root.isFile()) {
            return;
        }

        File[] listFiles = root.listFiles();

        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            if (file.isDirectory()) {
                if (!findOnce && file.getPath().endsWith("/Android/data")) {
                    LOG.d("Skip path", file.getPath());
                    findOnce = true;
                    continue;
                }

                search(file, exts, items);
            } else if (endWith(file.getName(), exts)) {
                items.add(new FileMeta(file.getPath()));
            }
        }
        return;

    }

    public static List<FileMeta> getFilesAndDirs(String path, boolean filterEmpty) {
        File file = new File(path);
        if (!file.isDirectory()) {
            return Collections.emptyList();
        }
        ArrayList<FileMeta> files = new ArrayList<FileMeta>();

        File[] listFiles = null;

        if (AppState.get().isDisplayAllFilesInFolder) {
            listFiles = file.listFiles();
        } else {
            listFiles = file.listFiles(SUPPORTED_EXT_AND_DIRS_FILTER);
        }

        if (listFiles == null || listFiles.length == 0) {
            return Collections.emptyList();
        }

        List<File> res = new ArrayList<File>(Arrays.asList(listFiles));
        // Collections.sort(res, FILES_AND_DIRS_COMPARATOR);

        for (File it : res) {
            if (it.getName().startsWith(".")) {
                continue;
            }
            if (!AppState.get().isDisplayAllFilesInFolder && filterEmpty && !isDirderctoryWithBook(it, 0)) {
                continue;
            }

            FileMeta meta = new FileMeta(it.getPath());

            if (it.isDirectory()) {
                FileMetaCore.get().upadteBasicMeta(meta, it);
                meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
            } else {
                FileMeta load = AppDB.get().load(it.getPath());
                if (load == null) {
                    FileMetaCore.get().upadteBasicMeta(meta, it);
                } else {
                    meta = load;
                }
            }
            files.add(meta);
        }

        return files;
    }

    public static boolean isDirderctoryWithBook(File dir, int dep) {
        if (dir.isFile() || dep == 2) {
            return true;
        }
        File[] list = dir.listFiles();
        if (list == null || list.length == 0) {
            return false;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                if (isDirderctoryWithBook(f, dep + 1)) {
                    return true;
                }
            } else {
                for (String s : ExtUtils.browseExts) {
                    if (f.getName().toLowerCase(Locale.US).endsWith(s)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static SupportedExtAndDirsFilter SUPPORTED_EXT_AND_DIRS_FILTER = new SupportedExtAndDirsFilter();

    public static class SupportedExtAndDirsFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return true;
            }
            for (String s : ExtUtils.browseExts) {
                if (pathname.getName().toLowerCase(Locale.US).endsWith(s)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static Comparator<File> FILES_AND_DIRS_COMPARATOR = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            if (o1.isDirectory() && o2.isFile())
                return -1;
            if (o1.isFile() && o2.isDirectory())
                return 1;
            return o1.getName().compareTo(o2.getName());
        }
    };

}
