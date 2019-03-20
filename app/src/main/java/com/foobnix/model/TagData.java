package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;

import java.io.File;
import java.io.FileFilter;

public class TagData {

    public static class Tag implements MyPath.RelativePath {
        public String path;
        public String tags;

        public Tag() {
        }

        public Tag(String path, String tags) {
            this.path = MyPath.toRelative(path);
            this.tags = tags;
        }

        public String getPath() {
            return MyPath.toAbsolute(path);
        }

        public void setPath(String path) {
            this.path = MyPath.toRelative(path);
        }
    }

    public static File TAGS_ROOT = new File(AppsConfig.SYNC_FOLDER, "tags");

    private static File getCacheFile(File path) {
        return getCacheFile(path.getPath());
    }

    private static File getCacheFile(String path) {
        TAGS_ROOT.mkdirs();

        if (path.startsWith(TAGS_ROOT.getPath())) {
            return new File(path);
        }

        return new File(TAGS_ROOT, ExtUtils.getFileName(path) + ".json");
    }

    public static void saveTags(FileMeta meta) {
        saveTags(meta.getPath(), meta.getTag());
    }

    public static void saveTags(String path, String tags) {
        IO.writeObjAsync(getCacheFile(path), new Tag(path, tags));
    }

    public static void restoreTags() {
        LOG.d("restoreTags");
        File[] files = TAGS_ROOT.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".json");
            }
        });
        if (files == null) {
            return;
        }

        for (File file : files) {
            Tag tag = new Tag();
            IO.readObj(file, tag);
            FileMeta load = AppDB.get().load(tag.getPath());
            if (load != null) {
                load.setTag(tag.tags);
                LOG.d("restoreTags", tag.getPath(), tag.tags);
                AppDB.get().update(load);
            }

        }


    }

}
