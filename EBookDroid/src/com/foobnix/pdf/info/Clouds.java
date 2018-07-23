package com.foobnix.pdf.info;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.ebookdroid.common.settings.books.BookSettings;
import org.greenrobot.eventbus.EventBus;

import com.cloudrail.si.CloudRail;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.Dropbox;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.OneDrive;
import com.cloudrail.si.types.CloudMetaData;
import com.cloudrail.si.types.SpaceAllocation;
import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.search.activity.msg.MessageSyncUpdateList;
import com.foobnix.pdf.search.view.AsyncProgressTask;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.FileMetaCore;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Clouds {

    private static final String TOKEN_EMPTY = "[{}]";
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

    public volatile String dropboxSpace;
    public volatile String googleSpace;
    public volatile String oneDriveSpace;
    private Context context;

    public static void saveProgress(BookSettings bs) {
        LOG.d("Save progress", bs);
        if (!isCloudImage(bs.fileName)) {
            return;
        }
        File bookFile = new File(bs.fileName);
        File folder = new File(bookFile.getParentFile(), ".data");
        folder.mkdirs();

        File settings = new File(folder, bookFile.getName());
        try {
            CacheZipUtils.copyFile(new ByteArrayInputStream(bs.toJSON().toString().getBytes()), settings);
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static boolean isCloud(String path) {
        return path.startsWith(PREFIX_CLOUD);
    }

    public static boolean isCloudImage(String path) {
        return path.contains("Librera.Cloud");
    }

    public static void runSync(Activity a) {
        a.startService(new Intent(a, BooksService.class).setAction(BooksService.ACTION_SYNC_DROPBOX));
    }

    public static File getCacheFile(String path) {

        if (!path.startsWith(PREFIX_CLOUD)) {
            return null;
        }

        File cacheDir = new File(AppState.get().cachePath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        String displayName = path.hashCode() + "_" + ExtUtils.getFileName(path);

        if (!path.contains(".Cloud/")) {
            File cacheFile = new File(AppState.get().cachePath, displayName);
            LOG.d("cacheFile-1", cacheFile);
            return cacheFile;
        }

        displayName = ExtUtils.getFileName(path);

        File cacheFile2 = null;
        if (path.startsWith(PREFIX_CLOUD_DROPBOX)) {
            cacheFile2 = new File(AppState.get().syncDropboxPath, displayName);
        } else if (path.startsWith(PREFIX_CLOUD_GDRIVE)) {
            cacheFile2 = new File(AppState.get().syncGdrivePath, displayName);
        } else if (path.startsWith(PREFIX_CLOUD_ONEDRIVE)) {
            cacheFile2 = new File(AppState.get().syncOneDrivePath, displayName);
        } else {
            cacheFile2 = new File(AppState.get().cachePath, displayName);
        }
        LOG.d("cacheFile-2", cacheFile2);

        return cacheFile2;

    }

    public static boolean isCacheFileExist(String path) {
        File cacheFile = getCacheFile(path);
        return cacheFile != null && cacheFile.isFile();

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
        save();

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
        this.context = c;
        try {

            CloudRail.setAppKey("5817abf0c40abf10ce9a04c5");

            sp = c.getSharedPreferences("Clouds", Context.MODE_PRIVATE);
            Objects.loadFromSp(this, sp);

            // https://www.dropbox.com/developers/apps
            dropbox = new Dropbox(c, "wp5uvfelqbdnwkg", "e7hfer9dh5r18tz", "https://auth.cloudrail.com/Librera", "");
            ((Dropbox) dropbox).useAdvancedAuthentication();

            googleDrive = new GoogleDrive(c, AppsConfig.GOOGLE_DRIVE_KEY, "", Apps.getPackageName(c) + ":/auth", "");
            ((GoogleDrive) googleDrive).useAdvancedAuthentication();

            // https://apps.dev.microsoft.com/#/application
            oneDrive = new OneDrive(c, "e5017cc6-0a84-4007-92ae-cfb9509d40db", "imhVPQO635[{xqdrPUN26[%", "https://auth.cloudrail.com/Librera", "");
            ((OneDrive) oneDrive).useAdvancedAuthentication();

            try {
                if (dropboxToken != null) {
                    dropbox.loadAsString(dropboxToken);
                }
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

        } catch (Throwable e) {
            LOG.e(e);
        }

    }

    public void syncronizeGet() {
        updateSpace();

        // IMG.clearMemoryCache();
        // IMG.clearDiscCache();

        syncronizeGet(dropbox, AppState.get().syncDropboxPath);
        syncronizeGet(googleDrive, AppState.get().syncGdrivePath);
        syncronizeGet(oneDrive, AppState.get().syncOneDrivePath);

    }

    private void syncronizeGet(CloudStorage storage, String syncPath) {

        LOG.d("syncronizeGet begin", storage);

        if (storage == null) {
            LOG.d("CloudStorage NULL");
            return;
        }

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
                    EventBus.getDefault().post(new MessageSyncUpdateList());

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

        if (cloud == null) {
            LOG.d("CloudStorage NULL");
            return;
        }

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
                    if (!file.getPath().equals(dest.getPath())) {
                        CacheZipUtils.copyFile(file, dest);
                    }

                    String extSyncFile = LIBRERA_SYNC_ONLINE_FOLDER + "/" + file.getName();

                    createLibreraCloudFolder(cloud);

                    FileInputStream outStream = new FileInputStream(file);
                    cloud.upload(extSyncFile, outStream, file.length(), true);
                    outStream.close();
                    LOG.d("upload File" + extSyncFile);

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
                runSync(a);
            }
        }.execute();

    }

    private void createLibreraCloudFolder(final CloudStorage cloud) {
        if (cloud == null) {
            LOG.d("CloudStorage NULL");
            return;
        }

        String extSyncFolder = LIBRERA_SYNC_ONLINE_FOLDER;
        if (!cloud.exists(extSyncFolder)) {
            cloud.createFolder(extSyncFolder);
            LOG.d("Create folder" + extSyncFolder);
        }
    }

    public static boolean isSyncFileExist(File file) {
        File sync = new File(AppState.get().syncDropboxPath, file.getName());
        return sync.exists() && sync.length() > 0;
    }

    public static void updateSpace() {
        try {
            if (Clouds.get().isDropbox()) {
                SpaceAllocation allocation = Clouds.get().dropbox.getAllocation();
                Clouds.get().dropboxSpace = ExtUtils.readableFileSize(allocation.getUsed()) + "/" + ExtUtils.readableFileSize(allocation.getTotal());
            }
            if (Clouds.get().isGoogleDrive()) {
                SpaceAllocation allocation = Clouds.get().googleDrive.getAllocation();
                Clouds.get().googleSpace = ExtUtils.readableFileSize(allocation.getUsed()) + "/" + ExtUtils.readableFileSize(allocation.getTotal());
            }
            if (Clouds.get().isOneDrive()) {
                SpaceAllocation allocation = Clouds.get().oneDrive.getAllocation();
                Clouds.get().oneDriveSpace = ExtUtils.readableFileSize(allocation.getUsed()) + "/" + ExtUtils.readableFileSize(allocation.getTotal());
            }
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void loginToDropbox(final Activity a, final Runnable success) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (!Clouds.get().isDropbox()) {
                        LOG.d("Begin login to dropbox");
                        Clouds.get().dropbox.loadAsString(TOKEN_EMPTY);
                        Clouds.get().dropbox.login();
                        LOG.d("End login to dropbox");
                        Clouds.get().dropboxToken = Clouds.get().dropbox.saveAsString();
                        LOG.d("token", Clouds.get().dropboxToken);
                        Clouds.get().dropboxInfo = Clouds.get().dropbox.getUserLogin();

                        Clouds.get().save();
                        createLibreraCloudFolder(Clouds.get().dropbox);
                        Clouds.runSync(a);
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
                        Clouds.get().oneDrive.loadAsString(TOKEN_EMPTY);
                        Clouds.get().oneDrive.login();

                        Clouds.get().oneDriveToken = Clouds.get().oneDrive.saveAsString();
                        Clouds.get().oneDriveInfo = Clouds.get().oneDrive.getUserLogin();
                        Clouds.get().save();
                        createLibreraCloudFolder(Clouds.get().oneDrive);
                        Clouds.runSync(a);
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
                        Clouds.get().googleDrive.loadAsString(TOKEN_EMPTY);
                        Clouds.get().googleDrive.login();

                        Clouds.get().googleDriveToken = Clouds.get().googleDrive.saveAsString();
                        Clouds.get().googleDriveInfo = Clouds.get().googleDrive.getUserLogin();
                        Clouds.get().save();
                        createLibreraCloudFolder(Clouds.get().googleDrive);
                        Clouds.runSync(a);
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

    public static boolean showHideCloudImage(ImageView img, String path) {
        if (!AppsConfig.isCloudsEnable) {
            img.setVisibility(View.GONE);
            return false;
        }

        if (path.contains(Clouds.LIBRERA_SYNC_ONLINE_FOLDER)) {
            img.setVisibility(View.VISIBLE);
            if (path.contains(AppState.LIBRERA_CLOUD_DROPBOX) || path.startsWith(Clouds.PREFIX_CLOUD_DROPBOX)) {
                img.setImageResource(R.drawable.dropbox);
            } else if (path.contains(AppState.LIBRERA_CLOUD_GOOGLEDRIVE) || path.startsWith(Clouds.PREFIX_CLOUD_GDRIVE)) {
                img.setImageResource(R.drawable.gdrive);
            } else if (path.contains(AppState.LIBRERA_CLOUD_ONEDRIVE) || path.startsWith(Clouds.PREFIX_CLOUD_ONEDRIVE)) {
                img.setImageResource(R.drawable.onedrive);
            } else {
                // img.setImageResource(R.drawable.star_1);
                img.setVisibility(View.GONE);
            }
            return true;
        } else {
            img.setVisibility(View.GONE);
            return false;
        }
    }

    public boolean isDropbox() {
        return dropbox != null && dropboxToken != null;
    }

    public boolean isGoogleDrive() {
        return googleDrive != null && googleDriveToken != null;
    }

    public boolean isOneDrive() {
        return oneDrive != null && oneDriveToken != null;
    }

    public void save() {
        LOG.d("CloudRail save");
        Objects.saveToSP(this, sp);

    }

    public static Clouds get() {
        return instance;
    }

}
