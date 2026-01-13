package com.foobnix.pdf.info;

import android.app.Activity;
import android.content.Context;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.model.MyPath;
import com.foobnix.ui2.AdsFragmentActivity;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Playlists {
    final public static String L_PLAYLIST = ".playlist";
    final public static String L_PLAYLIST_RECENT = ".recent";
    final public static String L_PLAYLIST_FAVORITES = ".favorites";
    final public static String L_PLAYLIST_FOLDER = ".folder";
    final public static String L_PLAYLIST_TAGS = ".tags";
    final public static String L_PLAYLIST_CURRENT_FOLDER = ".currentFolder";

    public static void createPlayList(String name) {
        LOG.d("Playlists", "createPlayList", name);
        if (TxtUtils.isEmpty(name)) {
            return;
        }

        File root = AppProfile.syncPlaylist;
        root.mkdirs();

        File child = new File(root, name + L_PLAYLIST);
        if (!child.exists()) {
            try {
                child.createNewFile();
            } catch (IOException e) {
                LOG.e(e);
            }
        }
    }

    public static void deletePlaylist(String name) {
        LOG.d("Playlists", "deletePlaylist", name);
        if (TxtUtils.isEmpty(name)) {
            return;
        }
        File child = new File(AppProfile.syncPlaylist, name.endsWith(L_PLAYLIST) ? name : name + L_PLAYLIST);
        child.delete();
    }

    public static void addMetaToPlaylist(String name, File file) {
        File child = new File(AppProfile.syncPlaylist, name.endsWith(L_PLAYLIST) ? name : name + L_PLAYLIST);

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(child, true)));
            out.println(MyPath.toRelative(file.getPath()));
            out.close();
        } catch (IOException e) {
            LOG.e(e);
        }

    }

    public static void updatePlaylist(String name, List<String> items) {
        File child = getFile(name);
        LOG.d("Playlists", "updatePlaylist", child);

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(child)));
            for (String path : items) {
                out.println(MyPath.toRelative(path));
            }
            out.close();
        } catch (IOException e) {
            LOG.e(e);
        }

    }

    public static List<String> getPlaylistItems(String name) {
        List<String> res = new ArrayList<String>();

        try {
            if (TxtUtils.isEmpty(name)) {
                return res;
            }

            File child = getFile(name);

            BufferedReader reader = new BufferedReader(new FileReader(child));

            String line;

            while ((line = reader.readLine()) != null) {
                if (TxtUtils.isNotEmpty(line)) {
                    line = MyPath.toAbsolute(line);
                    res.add(line.replace(L_PLAYLIST, ""));
                }
            }
            reader.close();
        } catch (Exception e) {
            LOG.e(e);
        }
        return res;

    }

    public static File getFile(String name) {
        File child = null;
        if (name.startsWith("/")) {
            child = new File(name);
        } else {
            child = new File(AppProfile.syncPlaylist, name.endsWith(L_PLAYLIST) ? name : name + L_PLAYLIST);
        }
        return child;
    }

    public static String getFirstItem(String path) {
        try {
            return getPlaylistItems(path).get(0);
        } catch (Exception e) {
            LOG.e(e);
            return path;
        }
    }

    public static String formatPlaylistName(android.content.Context a, String name) {
        if (name.startsWith(L_PLAYLIST_RECENT)) {
            return ((Activity) a).getString(R.string.recent);
        }
        if (name.startsWith(L_PLAYLIST_FAVORITES)) {
            return ((Activity) a).getString(R.string.favorites);
        }
        if (name.startsWith(L_PLAYLIST_FOLDER)) {
            String folderName = " [" + ExtUtils.getFileName(name) + "]";
            return ((Activity) a).getString(R.string.folder) + folderName;
        }
        if (name.startsWith(L_PLAYLIST_TAGS)) {
            String tag = name.replace(L_PLAYLIST_TAGS,"") ;
            return tag;
        }
        if (name.startsWith(L_PLAYLIST_CURRENT_FOLDER)) {
            String folderName = " [" + ExtUtils.getFileName(AppState.get().displayPath)+"]";
            return ((Activity) a).getString(R.string.current_folder) + folderName;
        }
        name = ExtUtils.getFileName(name);
        return TxtUtils.firstUppercase(name.replace(Playlists.L_PLAYLIST, "")) + " (" + getPlaylistItems(name).size() + ")";
    }

    public static List<FileMeta> getAllPlaylistsMeta(Context a) {
        List<FileMeta> res = new ArrayList<FileMeta>();

        for (String s : getAllPlaylists()) {
            FileMeta meta = new FileMeta(getFile(s).getPath());
            meta.setPathTxt(formatPlaylistName(a, s));
            meta.setCusType(FileMetaAdapter.DISPLAY_TYPE_PLAYLIST);
            res.add(meta);
        }

        return res;

    }

    public static List<String> getAllPlaylists() {
        List<String> res = new ArrayList<String>();

        File root = AppProfile.syncPlaylist;

        String[] list = root.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(L_PLAYLIST);
            }
        });
        if (list == null) {
            return res;
        }
        res.addAll(Arrays.asList(list));
        Collections.sort(res, String.CASE_INSENSITIVE_ORDER);
        return res;
    }

}
