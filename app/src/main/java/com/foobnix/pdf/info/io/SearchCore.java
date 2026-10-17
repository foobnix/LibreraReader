package com.foobnix.pdf.info.io;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
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

        if (root == null || exts == null) {
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

    /**
     * The same walk as {@link #search(List, File, List)}, over a folder the user added through the
     * system's folder picker. Such a folder is reached only through a document provider, so there
     * is nothing to list with {@link File}: every level is a query, and a book is named by the uri
     * of its document rather than by a path.
     * <p>
     * What a row shows is read from the same query, because a document has no file behind it for
     * {@link FileMetaCore#upadteBasicMeta} to measure.
     */
    public static void searchSaf(List<FileMeta> items, Context c, String treeUri, List<String> exts) {
        if (items == null || c == null || TxtUtils.isEmpty(treeUri) || exts == null) {
            return;
        }
        try {
            Uri uri = Uri.parse(treeUri);
            searchSaf(items, c, uri, uri, new ArrayList<>(exts), 0);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    // A document tree can be deep, and a provider is free to answer with whatever it likes, so the
    // walk is given a floor it cannot go below.
    private static final int SAF_MAX_DEPTH = 24;

    private static void searchSaf(List<FileMeta> items,
                                  Context c,
                                  Uri treeUri,
                                  Uri folderUri,
                                  List<String> exts,
                                  int depth) {
        if (depth > SAF_MAX_DEPTH) {
            LOG.d("find-saf-too-deep", folderUri);
            return;
        }
        Uri childrenUri = ExtUtils.getChildUri(c, folderUri);
        if (childrenUri == null) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = c.getContentResolver()
                      .query(childrenUri, new String[]{ //
                              Document.COLUMN_DISPLAY_NAME, //
                              Document.COLUMN_DOCUMENT_ID, //
                              Document.COLUMN_LAST_MODIFIED, //
                              Document.COLUMN_MIME_TYPE, //
                              Document.COLUMN_SIZE, //
                      }, null, null, null);
        } catch (Exception e) {
            LOG.e(e);
        }
        if (cursor == null) {
            LOG.d("find-saf-no-access", folderUri);
            return;
        }

        // The whole level is read before anything is kept from it: a .nomedia among the children
        // speaks for the folder, and it can come after the books in it.
        List<FileMeta> found = new ArrayList<FileMeta>();
        List<String> folders = new ArrayList<String>();
        boolean hasNoMedia = false;
        String parentName = safFolderName(c, folderUri);

        try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String documentId = cursor.getString(1);
                String lastModified = cursor.getString(2);
                String mimeType = cursor.getString(3);
                String size = cursor.getString(4);

                if (TxtUtils.isEmpty(name) || TxtUtils.isEmpty(documentId)) {
                    continue;
                }
                if (NOMEDIA.equals(name)) {
                    hasNoMedia = true;
                    continue;
                }
                if (name.startsWith(".")) {
                    LOG.d("find-saf-skip-hidden", name);
                    continue;
                }

                if (Document.MIME_TYPE_DIR.equals(mimeType)) {
                    folders.add(documentId);
                    continue;
                }
                if (!endWith(name, exts)) {
                    continue;
                }
                // As on the device, a file with nothing in it is not a book. A provider that
                // does not say how big a document is still gets the benefit of the doubt.
                if (size != null && parseLong(size) <= 0) {
                    continue;
                }

                FileMeta meta = new FileMeta(DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                                                              .toString());
                FileMetaCore.get()
                            .upadteBasicMetaSaf(meta, name, parseLong(size), parseLong(lastModified), parentName);
                found.add(meta);
            }
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            try {
                cursor.close();
            } catch (Exception ignored) {
            }
        }

        if (hasNoMedia && AppState.get().isSkipFolderWithNOMEDIA) {
            LOG.d("find-saf-skip NOMEDIA", folderUri);
            return;
        }

        items.addAll(found);

        for (String documentId : folders) {
            searchSaf(items, c, treeUri, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId), exts,
                    depth + 1);
        }
    }

    // What the library shows as the folder a book sits in. A document uri says nothing to a
    // reader, so the provider's own id for the folder is what is kept.
    private static String safFolderName(Context c, Uri folderUri) {
        try {
            return Uri.decode(ExtUtils.getExtSDDisplayName(c, folderUri.toString()));
        } catch (Exception e) {
            return folderUri.toString();
        }
    }

    private static long parseLong(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static List<FileMeta> getFilesAndDirs(String path, boolean filterEmpty, boolean isDisplayAllFilesInFolder) {
        LOG.d("getFilesAndDirs",path);
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


            FileMeta meta =  AppDB.get().getOrCreate(it.getPath());

            if (it.isDirectory()) {
                FileMetaCore.get().upadteBasicMeta(meta, it);
                meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
                meta.setIsStar(AppDB.get().isStarFolder(meta.getPath()));

            } else {
                if (meta.getState() != FileMetaCore.STATE_FULL) {
                    FileMetaCore.get().upadteBasicMeta(meta, it);
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
                String lowerCase = f.getName().toLowerCase(Locale.US);
                for (String s : ExtUtils.browseExts) {
                    if (lowerCase.endsWith(s)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static SupportedExtAndDirsFilter SUPPORTED_EXT_AND_DIRS_FILTER = new SupportedExtAndDirsFilter();

    public static SupportedExtFilesOnly SUPPORTED_EXT_FILES_ONLY = new SupportedExtFilesOnly();


    public static class SupportedExtAndDirsFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return true;
            }
            String lowerCase = pathname.getName().toLowerCase(Locale.US);
            for (String s : ExtUtils.browseExts) {
                if (lowerCase.endsWith(s)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class SupportedExtFilesOnly implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                return false;
            }
            String lowerCase = pathname.getName().toLowerCase(Locale.US);
            for (String s : ExtUtils.browseExts) {
                if (lowerCase.endsWith(s)) {
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
