package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;

import org.librera.LinkedJSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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

    public static boolean isValidMapFile(File file) {
        if (!file.isFile()) return false;
        try {
            new LinkedJSONObject(fileToString(file));
            return true;
        } catch (Exception invalid) {
            return false;
        }
    }

    public static boolean mapToFile(File jsonFile, Map<String, String> notes) {
        return writeMap(jsonFile, notes, false, false);
    }

    private static boolean writeMap(File jsonFile, Map<String, String> notes, boolean cacheEntry, boolean repairInvalid) {
        String json = JsonHelper.mapToJson(notes);
        if (json.isEmpty()) return false;
        File temporary = null;
        try {
            AutoCloseable lease;
            synchronized (BookCacheLeases.class) {
                temporary = BookCacheLeases.temporary(jsonFile.getParentFile(), "notes-");
                lease = BookCacheLeases.acquire(temporary);
            }
            try (AutoCloseable writing = lease) {
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(temporary);
                     java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
                             output, java.nio.charset.StandardCharsets.UTF_8)) {
                    writer.write(json);
                    writer.flush();
                    output.getFD().sync();
                }
                synchronized (BookCacheLeases.class) {
                    if (cacheEntry && (repairInvalid ? isValidMapFile(jsonFile) : jsonFile.isFile()))
                        return false;
                    if ((jsonFile.exists() && !jsonFile.isFile()) ||
                            BookCacheLeases.isProtected(jsonFile) || !temporary.renameTo(jsonFile))
                        throw new java.io.IOException("Cannot publish notes cache: " + jsonFile);
                }
            }
            return true;
        } catch (Exception e) {
            LOG.e(e);
            return false;
        } finally {
            if (temporary != null) temporary.delete();
        }
    }

    /** A conversion-keyed notes entry is immutable once valid; corrupt entries can be repaired. */
    public static boolean mapToCacheFile(File jsonFile, Map<String, String> notes) {
        return !isValidMapFile(jsonFile) && writeMap(jsonFile, notes, true, true);
    }

    /** Publishes a complete map only if another writer has not already created the file. */
    public static boolean mapToFileIfAbsent(File jsonFile, Map<String, String> notes) {
        if (jsonFile.isFile()) return false;
        return writeMap(jsonFile, notes, true, false);
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
