package org.ebookdroid.common.settings.books;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class SharedBooks {

    public static final File syncProgress = new File(AppsConfig.SYNC_FOLDER, "app-Progress.json");


    public static synchronized void updateProgress(List<FileMeta> list) {

        JSONObject obj = IO.readJsonObject(syncProgress);
        for (FileMeta meta : list) {
            try {
                final String fileName = ExtUtils.getFileName(meta.getPath());
                if (obj.has(fileName)) {
                    final JSONObject bookObj = obj.getJSONObject(fileName);

                    if (bookObj != null) {
                        AppBook book = load(obj, meta.getPath());
                        meta.setIsRecentProgress(book.p);
                        LOG.d("SharedBooks-updateProgress", meta.getPath(), book.p);
                    }
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        AppDB.get().updateAll(list);

    }

    public static synchronized AppBook load(String fileName) {
        LOG.d("SharedBooks-load", fileName);
        return load(IO.readJsonObject(syncProgress), fileName);

    }

    public static synchronized AppBook load(JSONObject obj, String fileName) {
        AppBook bs = new AppBook(fileName);
        try {
            LOG.d("SharedBooks-load", bs.path);
            final JSONObject jsonObject = obj.getJSONObject(ExtUtils.getFileName(fileName));
            Objects.loadFromJson(bs, jsonObject);
        } catch (Exception e) {
            LOG.e(e);
        }
        return bs;
    }

    public static synchronized void save(AppBook bs) {
        final int hash = Objects.hashCode(bs);
        bs.h = hash;
        final AppBook load = load(bs.path);
        if (load.h != hash) {
            load.h = hash;

            LOG.d("SharedBooks-save", bs.path, hash);

            try {
                JSONObject obj = IO.readJsonObject(syncProgress);
                final String fileName = ExtUtils.getFileName(bs.path);
                obj.put(fileName, Objects.toJSONObject(bs));
                IO.writeObjAsync(syncProgress, obj);
            } catch (Exception e) {
                LOG.e(e);
            }

        }
    }
}
