package com.foobnix.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.io.SearchCore;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class CheckDeletedBooksWorker extends MessageWorker {


    public CheckDeletedBooksWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public void doWorkInner() {

        List<FileMeta> all = AppDB.get().getAll();

        for (FileMeta meta : all) {
            if (meta == null) {
                continue;
            }

            if (Clouds.isCloud(meta.getPath())) {
                continue;
            }

            File bookFile = new File(meta.getPath());
            if (ExtUtils.isMounted(bookFile)) {
                if (!bookFile.exists()) {
                    AppDB.get().delete(meta);
                    LOG.d("BooksService", "Delete-setIsSearchBook", meta.getPath());
                }
            }

        }

        List<FileMeta> localMeta = new LinkedList<FileMeta>();
        if (JsonDB.isEmpty(BookCSS.get().searchPathsJson)) {
            sendFinishMessage();
            return;
        }

        for (final String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (path != null && path.trim().length() > 0) {
                final File root = new File(path);
                if (root.isDirectory()) {
                    LOG.d("Worker", "Search in " + root.getPath());
                    SearchCore.search(localMeta, root, ExtUtils.seachExts);
                }
            }
        }


        for (FileMeta meta : localMeta) {
            if (!all.contains(meta)) {
                FileMetaCore.createMetaIfNeedSafe(meta.getPath(), true);
                LOG.d("BooksService", "Add book", meta.getPath());
            }
        }


        List<FileMeta> allNone = AppDB.get().getAllByState(FileMetaCore.STATE_NONE);
        for (FileMeta m : allNone) {
            LOG.d("BooksService", "STATE_NONE", m.getTitle(), m.getPath(), m.getTitle());
            FileMetaCore.createMetaIfNeedSafe(m.getPath(), false);
        }

        Clouds.get().syncronizeGet();
    }
}
