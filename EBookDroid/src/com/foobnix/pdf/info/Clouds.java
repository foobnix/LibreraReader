package com.foobnix.pdf.info;

import java.io.File;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.pdf.info.wrapper.AppState;

import android.content.Context;
import android.content.SharedPreferences;

public class Clouds {

    public static final String PREFIX_CLOUD = "cloud-";
    public static final String PREFIX_CLOUD_DROPBOX = PREFIX_CLOUD + "dropbox:";
    public static final String PREFIX_CLOUD_GDRIVE = PREFIX_CLOUD + "gdrive:";
    public static final String PREFIX_CLOUD_ONEDRIVE = PREFIX_CLOUD + "onedrive:";

    private static final Clouds instance = new Clouds();

    transient SharedPreferences sp;
    transient public CloudStorage dropbox;

    public String dropboxToken;

    public static boolean isCloud(String path) {
        return path.startsWith(PREFIX_CLOUD);
    }

    public static boolean isCacheFileExist(String path) {
        if (!path.startsWith(PREFIX_CLOUD)) {
            return false;
        }
        String displayName = ExtUtils.getFileName(path);
        final File file = new File(AppState.get().downlodsPath, displayName);
        return file.isFile() && file.length() > 0;
    }

    public static File getCacheFile(String path) {
        if (!path.startsWith(PREFIX_CLOUD)) {
            return null;
        }
        String displayName = ExtUtils.getFileName(path);
        File file = new File(AppState.get().downlodsPath, displayName);
        if (file.isFile() && file.length() > 0) {
            return file;
        }
        return null;
    }

    public static String getPath(String pathWithPrefix) {
        return pathWithPrefix.replace(PREFIX_CLOUD_DROPBOX, "").replace(PREFIX_CLOUD_GDRIVE, "").replace(PREFIX_CLOUD_ONEDRIVE, "");
    }

    public static String getPrefix(String path) {
        if (path.startsWith(PREFIX_CLOUD_DROPBOX)) {
            return PREFIX_CLOUD_DROPBOX;
        }
        if (path.startsWith(PREFIX_CLOUD_GDRIVE)) {
            return PREFIX_CLOUD_GDRIVE;
        }
        if (path.startsWith(PREFIX_CLOUD_ONEDRIVE)) {
            return PREFIX_CLOUD_ONEDRIVE;
        }
        return "";
    }

    public static String getPrefixName(String path) {
        if (path.startsWith(PREFIX_CLOUD_DROPBOX)) {
            return "Dropbox";
        }
        if (path.startsWith(PREFIX_CLOUD_GDRIVE)) {
            return "GDrive";
        }
        if (path.startsWith(PREFIX_CLOUD_ONEDRIVE)) {
            return "OneDrive";
        }
        return "File";
    }

    public void init(Context c) {
        CloudRail.setAppKey("5817abf0c40abf10ce9a04c5");

        sp = c.getSharedPreferences("Clouds", Context.MODE_PRIVATE);
        Objects.loadFromSp(this, sp);

        dropbox = new Dropbox(c, "wp5uvfelqbdnwkg", "e7hfer9dh5r18tz", "https://auth.cloudrail.com/Librera", "foobnix");
        ((Dropbox) dropbox).useAdvancedAuthentication();

        if (dropbox != null) {
            try {
                dropbox.loadAsString(dropboxToken);
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    public boolean isDropbox() {
        return dropboxToken != null;
    }

    public void save() {
        LOG.d("CloudRail save");
        Objects.saveToSP(this, sp);

    }

    public static Clouds get() {
        return instance;
    }

}
