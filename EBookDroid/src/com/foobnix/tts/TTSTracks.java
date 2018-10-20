package com.foobnix.tts;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FileMetaComparators;
import com.foobnix.pdf.info.wrapper.AppState;

public class TTSTracks {

    public static boolean isMultyTracks() {
        List<File> listFiles = getAllMp3InFolder();
        return listFiles != null && listFiles.size() >= 2;
    }

    public static String getNextTrack() {

        List<File> listFiles = getAllMp3InFolder();
        if (listFiles == null) {
            return null;
        }

        for (int i = 0; i < listFiles.size(); i++) {
            File file = listFiles.get(i);
            if (file.getPath().equals(AppState.get().mp3BookPath)) {
                return listFiles.size() > i + 1 ? listFiles.get(i + 1).getPath() : null;
            }
        }

        return null;
    }

    public static String getPrevTrack() {
        List<File> listFiles = getAllMp3InFolder();
        if (listFiles == null) {
            return null;
        }

        for (int i = 0; i < listFiles.size(); i++) {
            File file = listFiles.get(i);
            if (file.getPath().equals(AppState.get().mp3BookPath)) {
                return i > 0 ? listFiles.get(i - 1).getPath() : null;
            }
        }

        return null;
    }

    public static String getCurrentTrackName() {
        return ExtUtils.getFileName(AppState.get().mp3BookPath);
    }

    private static List<File> getAllMp3InFolder() {
        if (TxtUtils.isEmpty(AppState.get().mp3BookPath)) {
            return null;
        }
        File file = new File(AppState.get().mp3BookPath);
        File root = file.getParentFile();
        if (!file.isFile() || !root.isDirectory()) {
            return null;
        }

        File[] listFiles = root.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                for (String ext : ExtUtils.AUDIO) {
                    if (name.toLowerCase(Locale.US).endsWith(ext)) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (listFiles == null || listFiles.length == 0) {
            return Collections.emptyList();
        }

        List<File> items = new ArrayList<File>(Arrays.asList(listFiles));

        Collections.sort(items, FileMetaComparators.BY_PATH_FILE);

        return items;
    }

}
