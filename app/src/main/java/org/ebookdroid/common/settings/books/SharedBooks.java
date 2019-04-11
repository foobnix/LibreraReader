package org.ebookdroid.common.settings.books;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;

import org.json.JSONObject;

import java.util.List;

public class SharedBooks {


    public static synchronized void updateProgress(List<FileMeta> list) {

        JSONObject obj = IO.readJsonObject(AppProfile.syncProgress);
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
        return load(IO.readJsonObject(AppProfile.syncProgress), fileName);

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
            if (rootObj.has(AppProfile.DEVICE_MODEL)) {
                final JSONObject deviceObj = rootObj.getJSONObject(AppProfile.DEVICE_MODEL);
                Objects.loadFromJson(bs, deviceObj);
            }
            bs.p = (float) rootObj.optDouble("p", 0.0);
            bs.t = rootObj.optLong("t", 0L);
            bs.ln = rootObj.optString("ln", null);

        } catch (Exception e) {
            LOG.e(e);
        }
        return bs;
    }

    public static synchronized void save(AppBook bs) {
        JSONObject obj = IO.readJsonObject(AppProfile.syncProgress);
        try {
            final String fileName = ExtUtils.getFileName(bs.path);

            JSONObject rootObj = obj.has(fileName) ? obj.getJSONObject(fileName) : new JSONObject();
            rootObj.put("p", bs.p);
            rootObj.put("t", bs.t);
            rootObj.put("ln", bs.ln);

            final JSONObject device = Objects.toJSONObject(bs);
            device.remove("p");
            device.remove("t");
            device.remove("ln");

            rootObj.put(AppProfile.DEVICE_MODEL, device);

            obj.put(fileName, rootObj);

            IO.writeObjAsync(AppProfile.syncProgress, obj);
        } catch (Exception e) {
            LOG.e(e);
        }


    }
}
