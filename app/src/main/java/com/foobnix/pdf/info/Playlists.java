package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.MyPath;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Playlists {
    final public static String L_PLAYLIST = ".playlist";
    final private static Map<String, Playlist> playlists = new HashMap<>();

    public static void createPlayList(String playlistName) {
        LOG.d("Playlists", "createPlayList", playlistName);

        if (playlists.containsKey(playlistName) || TxtUtils.isEmpty(playlistName)) {
            return;
        }

        File root = AppProfile.syncPlaylist;
        root.mkdirs();

        File child = new File(root, playlistName + L_PLAYLIST);
        if (!child.exists()) {
            try {
                child.createNewFile();
                playlists.put(child.getName(), new Playlist(child.getName()));
            } catch (IOException e) {
                LOG.e(e);
            }
        }
    }

    public static void deletePlaylist(String playlistName) {
        LOG.d("Playlists", "deletePlaylist", playlistName);
        if (TxtUtils.isEmpty(playlistName)) {
            return;
        }
        File child = new File(AppProfile.syncPlaylist, playlistName.endsWith(L_PLAYLIST) ? playlistName : playlistName + L_PLAYLIST);
        child.delete();
        playlists.remove(child.getName());
    }

    public static void addMetaToPlaylist(String playlistName, File file) {
        File child = new File(AppProfile.syncPlaylist, playlistName.endsWith(L_PLAYLIST) ? playlistName : playlistName + L_PLAYLIST);

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(child, true)));
            out.println(MyPath.toRelative(file.getPath()));
            out.close();
        } catch (IOException e) {
            LOG.e(e);
        }

    }

    public static void updatePlaylistAndCurrentItem(String playlistName, String current) {
        File playlistFile = getFile(playlistName);
        Objects.requireNonNull(playlists.get(playlistFile.getName())).updateCurrentFile(current);;
    }

    public static void updatePlaylist(String playlistName, List<String> items) {
        File playlist = getFile(playlistName);
        Objects.requireNonNull(playlists.get(playlist.getName())).update(items);
    }

    public static List<String> getPlaylistItems(String playlistName) {
        File playlist = getFile(playlistName);
        return Objects.requireNonNull(playlists.get(playlist.getName())).getItems();
    }

    public static Playlist getPlaylist(String playlistName) {
        File playlist = getFile(playlistName);
        return playlists.get(playlist.getName());
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

    public static String getFirstItem(String playlistName) {
        File playlist = getFile(playlistName);
        return Objects.requireNonNull(playlists.get(playlist.getName())).getFirstItem();
    }

    public static String formatPlaylistName(String name) {
        name = ExtUtils.getFileName(name);
        return TxtUtils.firstUppercase(name.replace(Playlists.L_PLAYLIST, "")) + " (" + getPlaylistItems(name).size() + ")";
    }

    public static List<FileMeta> getAllPlaylistsMeta() {
        List<FileMeta> res = new ArrayList<FileMeta>();

        for (String s : getAllPlaylists()) {
            playlists.put(s, new Playlist(s));
            FileMeta meta = new FileMeta(getFile(s).getPath());
            meta.setPathTxt(formatPlaylistName(s));
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
