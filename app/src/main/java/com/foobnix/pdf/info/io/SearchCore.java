package com.foobnix.pdf.info.io;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.Playlists;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchCore {

    public static String NOMEDIA = ".nomedia";

    public static boolean endWith(String name, List<String> exts) {
        if (exts == null) {
            return true;
        }


        for (String ext : exts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public static boolean findOnce = false;
    public static boolean findOnce2 = false;

    public static void searchSimple(List<FileMeta> items, File root, List<String> exts) {

        if (root ==null || exts == null) {
            return;
        }

        File[] listFiles = root.listFiles();

        if (listFiles == null) {
            return;
        }

        exts = new ArrayList<>(exts);

        for (File file : listFiles) {
            if (file.isFile() && endWith(file.getName(), exts)) {
                items.add(new FileMeta(file.getPath()));
            }
        }
    }

    public static void search(List<FileMeta> items, File root, List<String> exts) {

        if (root == null || root.getPath() == null || exts == null) {
            return;
        }
        if (root.getPath().equals("/")) {
            LOG.d("Skip search in root");
            return;
        }
        findOnce = false;
        exts = new ArrayList<>(exts);
        search(root, exts, items);
    }

    private static void search(File root, final List<String> exts, List<FileMeta> items) {

        if (root.isFile() && endWith(root.getName(), exts)) {
            final FileMeta e = new FileMeta(root.getPath());
            e.setTitle(root.getName());
            items.add(e);
            LOG.d("find-root", root.getPath());
            return;
        } else if (root.isFile()) {
            return;
        }

        File[] listFiles = root.listFiles();

        if (listFiles == null) {
            return;
        }
        for (File file : listFiles) {
            if (file.isHidden()) {
                LOG.d("find-skip-hidden", file.getPath());
                continue;
            }

            if (file.isDirectory()) {
                LOG.d("find-add-folder", file.getPath());

                if (AppState.get().isSkipFolderWithNOMEDIA && new File(file, NOMEDIA).isFile()) {
                    LOG.d("find-skip NOMEDIA", file.getPath());
                    continue;
                }

                if (!findOnce && file.getPath().endsWith("/Android/data")) {
                    LOG.d("find-skip-folder", file.getPath());
                    findOnce = true;
                    continue;
                }
                search(file, exts, items);
            } else if (file.length() > 0 && endWith(file.getName(), exts)) {
                LOG.d("find-add-file", file.getPath());
                final FileMeta e = new FileMeta(file.getPath());
                e.setTitle(file.getName());
                items.add(e);

            }
        }
        return;

    }

    public static List<FileMeta> getFilesAndDirs(String path, boolean filterEmpty, boolean isDisplayAllFilesInFolder) {
        File file = new File(path);
        if (!file.isDirectory()) {
            return Collections.emptyList();
        }
        ArrayList<FileMeta> files = new ArrayList<FileMeta>();

        File[] listFiles = null;

        if (isDisplayAllFilesInFolder) {
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
            if (!isDisplayAllFilesInFolder && it.getName().startsWith(".")) {
                continue;
            }
            if (!isDisplayAllFilesInFolder && filterEmpty && !isDirderctoryWithBook(it, 0)) {
                continue;
            }

            FileMeta meta = new FileMeta(it.getPath());

            if (it.isDirectory()) {
                FileMetaCore.get().upadteBasicMeta(meta, it);
                meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
                meta.setIsStar(AppDB.get().isStarFolder(meta.getPath()));
            } else {
                FileMeta load = AppDB.get().load(it.getPath());
                if (load == null) {
                    FileMetaCore.get().upadteBasicMeta(meta, it);
                } else {
                    meta = load;
                }
                if (it.getName().endsWith(Playlists.L_PLAYLIST)) {
                    meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_PLAYLIST);
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
