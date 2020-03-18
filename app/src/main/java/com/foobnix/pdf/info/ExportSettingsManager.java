package com.foobnix.pdf.info;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import androidx.core.util.Pair;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.ResultResponse;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.ui2.AppDB;

import org.librera.JSONArray;
import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExportSettingsManager {


    public static boolean exportAll(Context c, File toFile) {
        if (toFile == null) {
            return false;
        }
        LOG.d("TEST", "Export all to" + toFile.getPath());

        SharedPreferences pdf = c.getSharedPreferences("pdf", Context.MODE_PRIVATE);
        SharedPreferences BOOKS = c.getSharedPreferences("BOOKS", Context.MODE_PRIVATE);
        SharedPreferences ViewerPreferences = c.getSharedPreferences("ViewerPreferences", Context.MODE_PRIVATE);
        SharedPreferences BookCSS = c.getSharedPreferences("BookCSS", Context.MODE_PRIVATE);


        try {
            AppProfile.save(c);

            LinkedJSONObject root = new LinkedJSONObject();
            root.put("pdf", exportToJSon("pdf", pdf, null, null));
            root.put("BOOKS", exportToJSon("BOOKS", BOOKS, null, null));
            root.put("ViewerPreferences", exportToJSon("ViewerPreferences", ViewerPreferences, null, null));
            root.put("BookCSS", exportToJSon("BookCSS", BookCSS, null, null));

            root.put("Recent", fileMetaToJSON(AppDB.get().getRecentDeprecated()));
            root.put("StarsBook", fileMetaToJSON(AppDB.get().getStarsFilesDeprecated()));
            root.put("StarsFolder", fileMetaToJSON(AppDB.get().getStarsFoldersDeprecated()));

            root.put("TAGS", fileMetaTagToJSON(AppDB.get().getAllWithTag()));


            FileWriter file = new FileWriter(toFile);
            LOG.d("TEXT", "exoprt to " + toFile.getPath());
            String string = root.toString(5);
            file.write(string);
            file.flush();
            file.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(c, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public static String getSampleJsonConfigName(Context a, String ext) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String time = format.format(new Date());
            String name = String.format("%s%s", time, ext);
            return name;
        } catch (Exception e) {
            LOG.e(e);
            return "pdf_reader" + ext;
        }
    }


    public static JSONArray fileMetaToJSON(List<FileMeta> list) {
        JSONArray jsonObject = new JSONArray();
        if (list == null) {
            return jsonObject;
        }
        for (FileMeta value : list) {
            jsonObject.put(value.getPath());
        }
        return jsonObject;
    }

    public static JSONArray fileMetaTagToJSON(List<FileMeta> list) {
        JSONArray jsonObject = new JSONArray();
        if (list == null) {
            return jsonObject;
        }
        for (FileMeta value : list) {
            LinkedJSONObject obj = new LinkedJSONObject();
            try {
                obj.put("path", value.getPath());
                obj.put("tag", value.getTag());
            } catch (Exception e) {
                LOG.e(e);
            }
            jsonObject.put(obj);
        }
        return jsonObject;
    }

    public static void jsonToMeta(JSONArray jsonObject, ResultResponse<String> action) {
        if (jsonObject == null) {
            return;
        }
        try {
            for (int i = jsonObject.length() - 1; i >= 0; i--) {
                String path = jsonObject.getString(i);
                if (path != null) {
                    action.onResultRecive(path);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void jsonTagsToMeta(JSONArray jsonObject, ResultResponse<Pair<String, String>> action) {
        if (jsonObject == null) {
            return;
        }
        try {
            for (int i = jsonObject.length() - 1; i >= 0; i--) {
                LinkedJSONObject object = jsonObject.getJSONObject(i);
                if (object != null) {
                    action.onResultRecive(new Pair<String, String>(object.getString("path"), object.getString("tag")));
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static LinkedJSONObject exportToJSon(String name, SharedPreferences sp, String excludekey, String includeValue) throws JSONException {
        LinkedJSONObject linkedJsonObject = new LinkedJSONObject();
        Map<String, ?> all = sp.getAll();
        for (String key : all.keySet()) {
            if (excludekey != null && key.startsWith(excludekey)) {
                LOG.d("excludekey", key);
                continue;
            }
            Object value = all.get(key);
            if (includeValue != null && value != null && !value.toString().contains(includeValue)) {
                LOG.d("excludeValue", value);
                continue;
            }

            linkedJsonObject.put(key, value);
            LOG.d("export", key, value);
        }
        return linkedJsonObject;
    }

    public static void importFromJSon(LinkedJSONObject linkedJsonObject, SharedPreferences sp) throws JSONException {
        if (linkedJsonObject == null) {
            LOG.d("TEST", "import null");
            return;
        }
        LOG.d("importFromJSon", linkedJsonObject);

        Iterator<String> keys = linkedJsonObject.keys();
        Editor edit = sp.edit();
        while (keys.hasNext()) {
            String name = keys.next();
            Object res = linkedJsonObject.get(name);
            LOG.d("import", "name", name, "type", res);
            if (res instanceof String) {
                edit.putString(name, (String) res);
            } else if (res instanceof Float) {
                edit.putFloat(name, (Float) res);
            } else if (res instanceof Double) {
                edit.putFloat(name, ((Double) res).floatValue());
            } else if (res instanceof Integer) {
                edit.putInt(name, (Integer) res);
            } else if (res instanceof Boolean) {
                edit.putBoolean(name, (Boolean) res);
            }
        }
        edit.commit();
    }

}
