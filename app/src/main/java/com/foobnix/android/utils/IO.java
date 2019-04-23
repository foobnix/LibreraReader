package com.foobnix.android.utils;

import com.foobnix.mobi.parser.IOUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IO {

    public static void writeObj(String file, Object o) {
        writeObj(new File(file), o);
    }


    private static void writeObj(File file, Object o) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeObjAsync(file, o);
            }
        }).start();

    }

    public static void writeObjAsync(String file, Object o) {
        writeObjAsync(new File(file), o);
    }

    public static void writeObjAsync(File file, Object o) {
        if (o instanceof JSONObject || o instanceof JSONArray) {
            LOG.d("writeObjAsync", "JSONObject");
            IO.writeString(file, o.toString());
        } else if (o instanceof String) {
            LOG.d("writeObjAsync", "String");
            IO.writeString(file, (String) o);
        } else {
            //LOG.d("writeObjAsync", "Class", o.getClass().getName());
            IO.writeString(file, Objects.toJSONString(o));
        }

//        try {
//            LOG.d("writeObjAsync", file.getPath());
//            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
//            //out.setLevel(9);
//            ZipEntry e = new ZipEntry("file.json");
//            out.putNextEntry(e);
//            out.write(Objects.toJSONString(o).getBytes());
//            out.closeEntry();
//            out.close();
//        } catch (Exception e) {
//            LOG.e(e);
//        }

    }

    public static void readObj(String file, Object o) {
        readObj(new File(file), o);
    }

    public static void readObj(File file, Object o) {
        try {
            if (!file.exists()) {
                LOG.d("readObj not exsits", file.getPath());
                return;
            }

            Objects.loadFromJson(o, readString(file));

//            ZipInputStream in = new ZipInputStream(new FileInputStream(file));
//            in.getNextEntry();
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            IOUtils.copy(in, out);
//            Objects.loadFromJson(o, out.toString("UTF-8"));
//            in.close();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static boolean writeString(File file, String string) {
        try {
            if (string == null) {
                string = "";
            }
            LOG.d("IO", "write to file", file);
            new File(file.getParent()).mkdirs();

            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(string.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static JSONObject readJsonObject(File file) {
        final String s = readString(file);
        if(TxtUtils.isEmpty(s)){
            return new JSONObject();
        }

        try {
            return new JSONObject(s);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public static String readString(File file) {
        try {
            if(!file.exists()){
                return "";
            }
            LOG.d("IO", "read from file");
            StringBuilder builder = new StringBuilder();
            String aux = "";
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((aux = reader.readLine()) != null) {
                builder.append(aux);
            }
            reader.close();
            return builder.toString();
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";

    }

    public static boolean copyFile(File from, File to) {
        try {
            new File(to.getParent()).mkdirs();

            InputStream input = new BufferedInputStream(new FileInputStream(from));
            OutputStream output = new BufferedOutputStream(new FileOutputStream(to));

            IOUtils.copyClose(input, output);

            LOG.d("Copy file form to", from, to);
        } catch (IOException e) {
            LOG.e(e);
            return false;
        }
        return true;
    }

    public static boolean copyFile(InputStream from, File to) {
        try {
            new File(to.getParent()).mkdirs();

            InputStream input = new BufferedInputStream(from);
            OutputStream output = new BufferedOutputStream(new FileOutputStream(to));

            IOUtils.copyClose(input, output);

            LOG.d("Copy file form to", from, to);
        } catch (IOException e) {
            LOG.e(e);
            return false;
        }
        return true;
    }
}
