package com.foobnix.pdf.info;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.cloudrail.si.types.CloudMetaData;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.ui2.FileMetaCore;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class Clouds {

    public static final String LIBRERA_SYNC_ONLINE_FOLDER = "/Librera.Cloud";
    public static final String PREFIX_CLOUD = "cloud-";
    public static final String PREFIX_CLOUD_DROPBOX = PREFIX_CLOUD + "dropbox:";
    public static final String PREFIX_CLOUD_GDRIVE = PREFIX_CLOUD + "gdrive:";
    public static final String PREFIX_CLOUD_ONEDRIVE = PREFIX_CLOUD + "onedrive:";

    private static final Clouds instance = new Clouds();

    transient SharedPreferences sp;

    transient volatile public CloudStorage dropbox;
    transient volatile public CloudStorage googleDrive;
    transient volatile public CloudStorage oneDrive;

    public volatile String dropboxToken;
    public volatile String googleDriveToken;
    public volatile String oneDriveToken;

    public volatile String dropboxInfo;
    public volatile String googleDriveInfo;
    public volatile String oneDriveInfo;

    public static boolean isCloud(String path) {
        return path.startsWith(PREFIX_CLOUD);
    }

    public static boolean isCloudSyncFile(String path) {
        String parentName = new File(path).getParentFile().getName();
        String syncName = new File(AppState.get().syncDropboxPath).getName();

        LOG.d("isCloudSyncFile", parentName, syncName);

        if (parentName.equals(syncName)) {
            LOG.d("Cloud sync file");
            return true;
        }
        return false;
    }

    public static File getCacheFile(String path) {
        if (!path.startsWith(PREFIX_CLOUD)) {
            return null;
        }
        String displayName = ExtUtils.getFileName(path);

        File download = new File(AppState.get().downlodsPath, displayName);
        if (download.isFile() && download.length() > 0) {
            return download;
        }

        final File sync = new File(AppState.get().syncDropboxPath, displayName);
        if (sync.isFile() && sync.length() > 0) {
            return sync;
        }

        return null;
    }

    public static boolean isCacheFileExist(String path) {
        return getCacheFile(path) != null;

    }

    public static String getPath(String pathWithPrefix) {
        return pathWithPrefix.replace(PREFIX_CLOUD_DROPBOX, "").replace(PREFIX_CLOUD_GDRIVE, "").replace(PREFIX_CLOUD_ONEDRIVE, "");
    }

    public void logout(String path) {
        LOG.d("Logout", path);

        try {
            CloudStorage cloud = cloud(path);
            cloud.logout();
        } catch (Exception e) {
            LOG.e(e);
        }

        if (path.startsWith(PREFIX_CLOUD_DROPBOX)) {
            dropboxInfo = null;
            dropboxToken = null;
        }
        if (path.startsWith(PREFIX_CLOUD_GDRIVE)) {
            googleDriveInfo = null;
            googleDriveToken = null;
        }

        if (path.startsWith(PREFIX_CLOUD_ONEDRIVE)) {
            oneDriveInfo = null;
            oneDriveToken = null;
        }

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
            if (dropboxToken != null)
                dropbox.loadAsString(dropboxToken);
        } catch (Exception e) {
            LOG.e(e);
        }

        try {
            if (googleDriveToken != null)
                googleDrive.loadAsString(googleDriveToken);
        } catch (Exception e) {
            LOG.e(e);
        }

        try {
            if (oneDriveToken != null)
                oneDrive.loadAsString(oneDriveToken);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void syncronizeGet() {
        syncronizeGet(dropbox, AppState.get().syncDropboxPath);
        syncronizeGet(googleDrive, AppState.get().syncGdrivePath);
        syncronizeGet(oneDrive, AppState.get().syncOneDrivePath);
    }

    private void syncronizeGet(CloudStorage storage, String syncPath) {

        LOG.d("syncronizeGet begin", storage);

        if (storage instanceof Dropbox && !isDropbox()) {
            LOG.d("Dropbox is not connected");
            return;
        }

        if (storage instanceof GoogleDrive && !isGoogleDrive()) {
            LOG.d("GoogleDrive is not connected");
            return;
        }

        if (storage instanceof OneDrive && !isOneDrive()) {
            LOG.d("OneDrive is not connected");
            return;
        }

        try {

            if (!storage.exists(LIBRERA_SYNC_ONLINE_FOLDER)) {
                LOG.d("syncronize No sync folder");
                return;
            }

            File root = new File(syncPath);
            root.mkdirs();

            List<CloudMetaData> childs = storage.getChildren(LIBRERA_SYNC_ONLINE_FOLDER);
            for (CloudMetaData ch : childs) {

                LOG.d("get-cloud", ch.getName(), ch.getModifiedAt(), ch.getSize());

                if (ch.getFolder()) {
                    LOG.d("syncronize Skip folder", ch.getPath());
                    continue;
                }

                String name = ch.getName();
                File dest = new File(root, name);

                if (!dest.exists() || (ch.getSize() != 0 && ch.getSize() != dest.length())) {
                    String path = ch.getPath();
                    LOG.d("get-syncronize copy begin", path, "to", dest.getPath());
                    try {
                        InputStream download = storage.download(path);
                        CacheZipUtils.copyFile(download, dest);
                        download.close();

                        FileMetaCore.createMetaIfNeed(dest.getPath(), true);

                    } catch (Exception e) {
                        LOG.d("Download error", path, dest.getPath());
                        LOG.e(e);
                    }
                    LOG.d("syncronize copy sync", path);
                }
                LOG.d("get-file-", dest.getName(), dest.lastModified(), dest.length());

            }

            if (root.listFiles() != null) {
                for (File file : root.listFiles()) {
                    if (file.isDirectory()) {
                        continue;
                    }
                    boolean isFound = false;
                    for (CloudMetaData ch : childs) {
                        if (ch.getFolder()) {
                            continue;
                        }
                        if (file.getName().equals(ch.getName())) {
                            isFound = true;
                            break;
                        }
                    }
                    // file not found in dropbox
                    if (!isFound) {
                        file.delete();
                        LOG.d("get-delete", file);
                    }
                }
            }

        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void syncronizeAdd(final Activity a, final File file, final CloudStorage cloud) {

        new AsyncProgressTask<Boolean>() {

            @Override
            public Context getContext() {
                return a;
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                try {
                    String syncPath = "";
                    if (cloud instanceof Dropbox) {
                        syncPath = AppState.get().syncDropboxPath;
                    } else if (cloud instanceof GoogleDrive) {
                        syncPath = AppState.get().syncGdrivePath;
                    } else if (cloud instanceof OneDrive) {
                        syncPath = AppState.get().syncOneDrivePath;
                    } else {
                        new IllegalArgumentException("Invalid provider");

                    }

                    File root = new File(syncPath);
                    root.mkdirs();
                    File dest = new File(root, file.getName());
                    CacheZipUtils.copyFile(file, dest);

                    String extSyncFolder = LIBRERA_SYNC_ONLINE_FOLDER;
                    String extSyncFile = LIBRERA_SYNC_ONLINE_FOLDER + "/" + file.getName();

                    if (!cloud.exists(extSyncFolder)) {
                        cloud.createFolder(extSyncFolder);
                        LOG.d("Create folder" + extSyncFolder);
                    }
                    if (!cloud.exists(extSyncFile)) {
                        FileInputStream outStream = new FileInputStream(file);
                        cloud.upload(extSyncFile, outStream, file.length(), true);
                        outStream.close();
                        LOG.d("upload File" + extSyncFile);
                    }

                    return true;
                } catch (Exception e) {
                    LOG.e(e);
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result == false) {
                    Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(a, R.string.success, Toast.LENGTH_SHORT).show();
            }
        }.execute();

    }

    public static boolean isSyncFileExist(File file) {
        File sync = new File(AppState.get().syncDropboxPath, file.getName());
        return sync.exists() && sync.length() > 0;
    }

    public void loginToDropbox(final Activity a, final Runnable success) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (!Clouds.get().isDropbox()) {
                        Clouds.get().dropbox.login();
                        Clouds.get().dropboxToken = Clouds.get().dropbox.saveAsString();
                        Clouds.get().dropboxInfo = Clouds.get().dropbox.getUserLogin();
                        Clouds.get().save();
                    }

                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (success != null) {
                                success.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    LOG.d(e);
                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };
        }.start();
    }

    public void loginToOneDrive(final Activity a, final Runnable success) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (!Clouds.get().isOneDrive()) {
                        Clouds.get().oneDrive.login();

                        Clouds.get().oneDriveToken = Clouds.get().oneDrive.saveAsString();
                        Clouds.get().oneDriveInfo = Clouds.get().oneDrive.getUserLogin();
                        Clouds.get().save();
                    }

                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (success != null) {
                                success.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    LOG.d(e);
                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };
        }.start();
    }

    public void loginToGoogleDrive(final Activity a, final Runnable success) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (!Clouds.get().isGoogleDrive()) {
                        Clouds.get().googleDrive.login();

                        Clouds.get().googleDriveToken = Clouds.get().googleDrive.saveAsString();
                        Clouds.get().googleDriveInfo = Clouds.get().googleDrive.getUserLogin();
                        Clouds.get().save();
                    }

                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (success != null) {
                                success.run();
                            }
                        }
                    });
                } catch (Exception e) {
                    LOG.d(e);
                    a.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            };
        }.start();
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
