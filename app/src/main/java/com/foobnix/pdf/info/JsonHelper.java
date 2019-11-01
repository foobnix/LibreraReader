package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;

import org.librera.LinkedJSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JsonHelper {

    public static Map<String, String> fileToMap(File jsonFile) {
        String json = JsonHelper.fileToString(jsonFile);
        return JsonHelper.jsonToMap(json);
    }

    public static void mapToFile(File jsonFile, Map<String, String> notes) {
        try {
            FileWriter fw = new FileWriter(jsonFile);
            fw.write(JsonHelper.mapToJson(notes));
            fw.flush();
            fw.close();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static String fileToString(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder total = new StringBuilder();
            String line;

            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            inputStream.close();
            return total.toString();
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    public static String mapToJson(Map<String, String> map) {
        try {
            if (map == null) {
                return "";
            }
            LinkedJSONObject obj = new LinkedJSONObject();
            for (String key : map.keySet()) {
                String value = map.get(key);
                if (key != null && value != null) {
                    obj.put(key, value);
                }
            }
            return obj.toString();
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    public static Map<String, String> jsonToMap(String json) {
        Map<String, String> map = new HashMap<String, String>();
        try {

            LinkedJSONObject linkedJsonObject = new LinkedJSONObject(json);
            Iterator<String> keys = linkedJsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, linkedJsonObject.getString(key));
            }

            return map;
        } catch (Exception e) {
            LOG.e(e);
        }
        return map;
    }

}
