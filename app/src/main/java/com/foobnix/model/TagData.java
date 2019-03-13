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

    public static class Tag {
        public String path;
        public String tags;

        public Tag() {
        }

        public Tag(String path, String tags) {
            this.path = path;
            this.tags = tags;
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
        IO.writeObjAsync(getCacheFile(meta.getPath()), new Tag(meta.getPath(), meta.getTag()));
    }

    public static void restoreTags() {
        LOG.d("restoreTags");
        File[] files = TAGS_ROOT.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".json");
            }
        });
        for (File file : files) {
            Tag tag = new Tag();
            IO.readObj(file, tag);
            FileMeta load = AppDB.get().load(tag.path);
            if (load != null) {
                load.setTag(tag.tags);
                LOG.d("restoreTags", tag.path, tag.tags);
                AppDB.get().update(load);
            }

        }


    }

}
