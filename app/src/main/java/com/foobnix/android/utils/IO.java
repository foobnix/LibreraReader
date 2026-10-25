package com.foobnix.android.utils;

import com.foobnix.mobi.parser.IOUtils;

import com.foobnix.LibreraApp;

import org.librera.JSONArray;
import org.librera.JSONException;
import org.librera.LinkedJSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IO {

    static HashMap<Integer, Object> locks = new HashMap<>();

    /**
     * The thread every background write goes through, in the order the writes were asked for.
     *
     * It used to be the app's one shared worker, the same thread that renders book pages and
     * reads outlines - so a page turned and the phone locked left the reading position queued
     * behind work that could take seconds, and a process killed in the background in the
     * meantime never wrote it at all.
     */
    private static final ExecutorService writer = Executors.newSingleThreadExecutor(r -> new Thread(r, "@T IO writer"));

    // Two threads asking for the same file at once could each make a lock of their own.
    public static synchronized Object getLock(File file) {
        Object l = locks.get(file.hashCode());
        if (l == null) {
            l = new Object();
            locks.put(file.hashCode(), l);
        } else {
            LOG.d("mylock", file.getPath(), l);
        }
        return l;
    }


    public static void writeObj(File file, Object o) {
        //new Thread(() -> writeObjAsync(file, o), "@T writeObj").start();
        writer.execute(() -> writeObjSync(file, o));
    }

    public static void writeObjSync(File file, Object o) {
        LOG.d("writeObjAsync", file.getPath());
        if (o instanceof LinkedJSONObject || o instanceof JSONArray) {
            LOG.d("writeObjAsync", "LinkedJSONObject");
            IO.writeString(file, o.toString());
        } else if (o instanceof String) {
            LOG.d("writeObjAsync", "String");
            IO.writeString(file, (String) o);
        } else {
            //LOG.d("writeObjAsync", "Class", o.getClass().getName());
            IO.writeString(file, Objects.toJSONString(o));
        }
    }

    public static void readObj(File file, Object o) {

        try {
            if (!file.exists()) {
                LOG.d("readObj not exsits", file.getPath());
                return;
            }

            Objects.loadFromJson(o, readString(file));

        } catch (Exception e) {
            LOG.e(e);
        }

    }


    public static LinkedJSONObject readJsonObject(File file) {

        final String s = readString(file);
        if (TxtUtils.isEmpty(s)) {
            return new LinkedJSONObject();
        }

        try {
            return new LinkedJSONObject(s);
        } catch (JSONException e) {
            return new LinkedJSONObject();
        }

    }

    /**
     * The last file read or written, held as one value so its name and its content can never
     * come from two different files.
     *
     * They used to be two separate fields. A thread that had set the name but not yet the
     * content left a moment where another thread asking for that file was handed the previous
     * file's text - and a save reading the progress file that way wrote it back as some other
     * file plus one book, losing every other book's position. Nor was the content ever checked
     * against the disk, so a file replaced by sync went on being read as it was before.
     */
    private static final class Cached {
        final String path;
        final long modified;
        final long length;
        final String content;

        Cached(File file, String content) {
            this.path = file.getPath();
            this.modified = file.lastModified();
            this.length = file.length();
            this.content = content;
        }

        boolean isFor(File file) {
            return path.equals(file.getPath()) && modified == file.lastModified() && length == file.length();
        }
    }

    private static volatile Cached cached;

    public static String readString(File file) {
        return readString(file, false);
    }

    public static String readStringFromAsset(String assetName) throws IOException {
        InputStream open = LibreraApp.context.getAssets().open(assetName);
        return readString(open);
    }

    public static String readString(InputStream open) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(open, out);
        return out.toString().trim();
    }


    public static String readString(File file, boolean withSeparator) {
        final Cached hit = cached;
        if (!withSeparator && hit != null && hit.isFor(file)) {
            LOG.d("lib-IO", "read cache", file);
            return hit.content;
        }
        synchronized (getLock(file)) {

            try {
                if (!file.exists()) {
                    return "";
                }
                LOG.d("lib-IO", "read file", file);
                StringBuilder builder = new StringBuilder();
                String aux = "";
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String separator = System.getProperty("line.separator");

                while ((aux = reader.readLine()) != null) {
                    builder.append(aux);
                    if (withSeparator) {
                        builder.append(separator);
                    }
                }
                reader.close();
                final String content = builder.toString();
                if (!withSeparator) {
                    cached = new Cached(file, content);
                }
                return content;
            } catch (Exception e) {
                LOG.e(e);
            }
            return "";
        }
    }

    public static boolean writeString(File file, String string) {

        synchronized (getLock(file)) {

            try {
                if (string == null) {
                    string = "";
                }
                LOG.d("lib-IO", "write file", file);
                new File(file.getParent()).mkdirs();

                // Written beside the file and then put in its place, so the file is always
                // either the old content or the new one. Written in place, a process killed
                // in the background part way through - a locked phone is where that happens -
                // left it empty or cut short, and an unreadable progress file reads as no
                // progress at all: every book back to its first page.
                final File tmp = new File(file.getPath() + ".tmp");
                OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp));
                try {
                    out.write(string.getBytes());
                    out.flush();
                } finally {
                    out.close();
                }
                if (!tmp.renameTo(file)) {
                    LOG.d("lib-IO", "rename failed, writing in place", file);
                    tmp.delete();
                    OutputStream direct = new BufferedOutputStream(new FileOutputStream(file));
                    try {
                        direct.write(string.getBytes());
                        direct.flush();
                    } finally {
                        direct.close();
                    }
                }

                cached = new Cached(file, string);

            } catch (Exception e) {
                LOG.e(e);
                return false;
            }
            return true;
        }
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
