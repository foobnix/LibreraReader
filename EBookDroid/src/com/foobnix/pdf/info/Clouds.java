package com.foobnix.pdf.info;

import java.io.File;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.foobnix.android.utils.Apps;
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
    transient public CloudStorage googleDrive;
    transient public CloudStorage oneDrive;

    public String dropboxToken;
    public String googleDriveToken;
    public String oneDriveToken;

    public String dropboxInfo;
    public String googleDriveInfo;
    public String oneDriveInfo;

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

    public void logout(String path) {
        CloudStorage cloud = cloud(path);
        cloud.logout();
    }
    public CloudStorage cloud(String path) {
        if (path.startsWith(PREFIX_CLOUD_DROPBOX)) {
            return dropbox;
        }
        if (path.startsWith(PREFIX_CLOUD_GDRIVE)) {
            return googleDrive;
        }

        if (path.startsWith(PREFIX_CLOUD_ONEDRIVE)) {
            return oneDrive;
        }
        return null;
    }

    public String getUserLogin(String path) {
        if (path.startsWith(PREFIX_CLOUD_DROPBOX)) {
            return dropboxInfo;
        }
        if (path.startsWith(PREFIX_CLOUD_GDRIVE)) {
            return googleDriveInfo;
        }
        if (path.startsWith(PREFIX_CLOUD_ONEDRIVE)) {
            return oneDriveInfo;
        }
        return "";
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

        // https://www.dropbox.com/developers/apps
        dropbox = new Dropbox(c, "wp5uvfelqbdnwkg", "e7hfer9dh5r18tz", "https://auth.cloudrail.com/Librera", "");
        ((Dropbox) dropbox).useAdvancedAuthentication();

        // https://console.cloud.google.com/apis/credentials?project=librera-release
        googleDrive = new GoogleDrive(c, AppsConfig.GOOGLE_DRIVE_KEY, "", Apps.getPackageName(c) + ":/auth", "");
        ((GoogleDrive) googleDrive).useAdvancedAuthentication();

        // https://apps.dev.microsoft.com/#/application
        oneDrive = new OneDrive(c, "e5017cc6-0a84-4007-92ae-cfb9509d40db", "imhVPQO635[{xqdrPUN26[%", "https://auth.cloudrail.com/Librera", "");
        ((OneDrive) oneDrive).useAdvancedAuthentication();

        try {
            dropbox.loadAsString(dropboxToken);
        } catch (Exception e) {
            LOG.e(e);
        }

        try {
            googleDrive.loadAsString(googleDriveToken);
        } catch (Exception e) {
            LOG.e(e);
        }

        try {
            oneDrive.loadAsString(oneDriveToken);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public boolean isDropbox() {
        return dropboxToken != null;
    }

    public boolean isGoogleDrive() {
        return googleDriveToken != null;
    }

    public boolean isOneDrive() {
        return oneDriveToken != null;
    }

    public void save() {
        LOG.d("CloudRail save");
        Objects.saveToSP(this, sp);

    }

    public static Clouds get() {
        return instance;
    }

}
