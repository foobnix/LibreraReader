package com.foobnix.android.utils;

import com.foobnix.mobi.parser.IOUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        if (LOG.isEnable) {
            IO.writeString(new File(file.getPath()+".txt"), Objects.toJSONString(o));
        }
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
            //out.setLevel(9);
            ZipEntry e = new ZipEntry(file.getName());
            out.putNextEntry(e);
            out.write(Objects.toJSONString(o).getBytes());
            out.closeEntry();
            out.close();
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static void readObj(File file, Object o) {
        try {
            ZipInputStream in = new ZipInputStream(new FileInputStream(file));
            in.getNextEntry();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);
            Objects.loadFromJson(o, out.toString("UTF-8"));
            in.close();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static boolean writeString(File file, String string) {
        try {
            LOG.d("IO", "write to file");
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
}
