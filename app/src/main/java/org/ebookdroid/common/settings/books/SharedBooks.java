package org.ebookdroid.common.settings.books;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

public class SharedBooks {


    public static synchronized void updateProgress(List<FileMeta> list) {

        for (FileMeta meta : list) {
            try {
                AppBook book = SharedBooks.load(meta.getPath());
                meta.setIsRecentProgress(book.p);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        AppDB.get().updateAll(list);
    }

    public static synchronized AppBook load(String fileName) {
        LOG.d("SharedBooks-load", fileName);

        AppBook res = new AppBook(fileName);
        for (File file : AppProfile.getAllFiles(AppProfile.APP_PROGRESS_JSON)) {
            final AppBook load = load(IO.readJsonObject(file), fileName);
            if (load.t > res.t) {
                res.path = fileName;
                res = load;
            }
        }
        return res;

    }

    public static synchronized AppBook load(JSONObject obj, String fileName) {
        AppBook bs = new AppBook(fileName);
        try {

            LOG.d("SharedBooks-load", bs.path);
            final String key = ExtUtils.getFileName(fileName);
            if (!obj.has(key)) {
                return bs;
            }
            final JSONObject rootObj = obj.getJSONObject(key);
            Objects.loadFromJson(bs, rootObj);
        } catch (Exception e) {
            LOG.e(e);
        }
        return bs;
    }

    public static synchronized void save(AppBook bs) {
        if (bs == null || TxtUtils.isEmpty(bs.path)) {
            LOG.d("Can't save AppBook");
            return;
        }
        JSONObject obj = IO.readJsonObject(AppProfile.syncProgress);
        try {
            final String fileName = ExtUtils.getFileName(bs.path);
            obj.put(fileName, Objects.toJSONObject(bs));
            IO.writeObjAsync(AppProfile.syncProgress, obj);
        } catch (Exception e) {
            LOG.e(e);
        }


    }
}
