package com.foobnix.sh;

import android.content.SharedPreferences;

import com.foobnix.android.utils.LOG;

import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MySharedPreferences implements SharedPreferences {

    LinkedJSONObject object = null;
    final File file;

    public MySharedPreferences(File root, String name) {
        LOG.d("MySharedPreferences begin", name);
        ;
        root.mkdirs();

        file = new File(root, name + ".json");
        load();
    }

    public void load() {
        try {
            object = new LinkedJSONObject(fileToString(file));
        } catch (Exception e) {
            LOG.w(e);
            object = new LinkedJSONObject();
        }
        LOG.d("MySharedPreferences loading");
    }


    @Override
    public Map<String, ?> getAll() {
        Iterator<String> keys = object.keys();
        Map<String, Object> res = new HashMap<>();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                res.put(key, object.get(key));
            } catch (JSONException e) {
                LOG.w(e);
            }

        }

        return res;
    }


    @Override
    public String getString(String key, String defValue) {
        try {
            return object.getString(key);
        } catch (JSONException e) {
            return defValue;
        }
    }


    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        //new RuntimeException("not implemented");
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        try {
            return object.getInt(key);
        } catch (JSONException e) {
            return defValue;
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        try {
            return object.getLong(key);
        } catch (JSONException e) {
            return defValue;
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        try {
            return (float) object.getDouble(key);
        } catch (JSONException e) {
            return defValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        try {
            return object.getBoolean(key);
        } catch (JSONException e) {
            return defValue;
        }
    }

    @Override
    public boolean contains(String key) {
        return object.has(key);
    }

    @Override
    public Editor edit() {
        return new MyEdit(object, file);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        new RuntimeException("not implemented");
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        new RuntimeException("not implemented");
    }

    public static class MyEdit implements Editor {
        LinkedJSONObject object;
        File file;

        public MyEdit(LinkedJSONObject object, File file) {
            this.object = object;
            this.file = file;
        }

        @Override
        public Editor putString(String key, String value) {
            try {
                object.put(key, value);
            } catch (JSONException e) {
                LOG.w(e);
            }
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            //new RuntimeException("not implemented");
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            try {
                object.put(key, value);
            } catch (JSONException e) {
                LOG.w(e);
            }
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            try {
                object.put(key, value);
            } catch (JSONException e) {
                LOG.w(e);
            }
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            try {
                object.put(key, value);
            } catch (JSONException e) {
                LOG.w(e);
            }
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            try {
                object.put(key, value);
            } catch (JSONException e) {
                LOG.w(e);
            }
            return this;
        }

        @Override
        public Editor remove(String key) {
            try {
                object.remove(key);
            } catch (Exception e) {
                LOG.w(e);
            }
            return this;
        }

        @Override
        public Editor clear() {
            object = new LinkedJSONObject();
            return this;
        }

        @Override
        public boolean commit() {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    LOG.d("MySharedPreferences", "write");
                    write(file, object.toString());
                }
            },"@T MySharedPreferences commit").start();

            return false;
        }

        @Override
        public void apply() {
            new RuntimeException("not implemented");
        }
    }

    public static void write(File file, String info) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(info.getBytes());
            out.close();
        } catch (IOException e) {
            LOG.w(e);
        }
    }


    public static String fileToString(File file) throws Exception {
        StringBuilder builder = new StringBuilder();
        String aux = "";
        BufferedReader reader = new BufferedReader(new FileReader(file));
        while ((aux = reader.readLine()) != null) {
            builder.append(aux);
        }
        reader.close();
        return builder.toString();
    }

}
