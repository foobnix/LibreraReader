package com.foobnix.android.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;

public class IO {

    public static void writeObj(File file, Object o) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeObjAsync(file, o);
            }
        }).start();

    }

    public static void writeObjAsync(File file, Object o) {
        IO.writeString(file, Objects.toJSONString(o));
    }

    public static void readObj(File file, Object o) {
        Objects.loadFromJson(o, IO.readString(file));
    }

    public static boolean writeString(File file, String string) {
        try {
            LOG.d("IO","write to file");
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(string.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static String readString(File file) {
        try {
            LOG.d("IO","read from file");
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
}
