package com.foobnix.drive;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.FileMetaCore;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import org.ebookdroid.common.settings.books.SharedBooks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GFile {
    public static final int REQUEST_CODE_SIGN_IN = 1110;


    public static final String MIME_FOLDER = "application/vnd.google-apps.folder";

    public static final String TAG = "GFile";
    public static final int PAGE_SIZE = 1000;
    public static final String SKIP = "skip";
    public static final String MY_SCOPE = DriveScopes.DRIVE_FILE;
    public static final String LASTMODIFIED = "lastmodified2";

    public static com.google.api.services.drive.Drive googleDriveService;

    public static String debugOut = new String();


    public static String getDisplayInfo(Context c) {
        final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(c);
        if (account == null) {
            return "";
        }
        return TxtUtils.nullToEmpty(account.getDisplayName()) + " (" + account.getEmail() + ")";

    }

    public static void logout(Context c) {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(MY_SCOPE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(c, signInOptions);
        client.signOut();
        googleDriveService = null;
        AppSP.get().syncRootID = "";
        AppSP.get().syncTime = 0;

    }

    public static void init(Activity c) {

        logout(c);

        if (googleDriveService != null) {
            return;
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(c);

        if (account == null) {


            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(MY_SCOPE))
                            .build();
            GoogleSignInClient client = GoogleSignIn.getClient(c, signInOptions);

            // The result of the sign-in Intent is handled in onActivityResult.
            c.startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        } else {

            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            c, Collections.singleton(MY_SCOPE));
            credential.setSelectedAccount(account.getAccount());
            googleDriveService =
                    new com.google.api.services.drive.Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName(Apps.getApplicationName(c))
                            .build();
        }
        sp = c.getSharedPreferences(LASTMODIFIED, Context.MODE_PRIVATE);

    }

    static SharedPreferences sp;

    public static void buildDriveService(Context c) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(c);
        if (account == null) {
            LOG.d(TAG, "buildDriveService", " account is null");
            return;
        }

        if (googleDriveService != null) {
            LOG.d(TAG, "googleDriveService", " has already inited");
            return;
        }


        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        c, Collections.singleton(MY_SCOPE));
        credential.setSelectedAccount(account.getAccount());
        googleDriveService =
                new com.google.api.services.drive.Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(Apps.getApplicationName(c))
                        .build();

        LOG.d(TAG, "googleDriveService", " build");
        sp = c.getSharedPreferences(LASTMODIFIED, Context.MODE_PRIVATE);

    }

    public static List<File> exeQF(String q, String arg1) throws IOException {
        return exeQ(String.format(q, arg1));
    }

    public static List<File> exeQF(String q, String arg1, String arg2) throws IOException {
        return exeQ(String.format(q, arg1, arg2));
    }

    public static List<File> exeQF(String q, String arg1, String arg2, String arg3, String arg4) throws IOException {
        return exeQ(String.format(q, arg1, arg2, arg3, arg4));
    }

    public static List<File> exeQF(String q, String arg1, String arg2, String arg3) throws IOException {
        return exeQ(String.format(q, arg1, arg2, arg3));
    }

    public static List<File> exeQ(String q) throws IOException {
        //LOG.d(TAG, "exeQ", q);
        String nextPageToken = "";
        List<File> res = new ArrayList<File>();
        do {
            //debugOut += "\n:" + q;

            final FileList list = (FileList) googleDriveService.files().list().setSpaces("drive").setQ(q).setPageToken(nextPageToken).setFields("nextPageToken, files(*)").setPageSize(PAGE_SIZE).setOrderBy("modifiedTime").execute();
            nextPageToken = list.getNextPageToken();
            res.addAll(list.getFiles());
            debugOut += "\nGet remote files info: " + list.getFiles().size();
            //debugPrint(list.getFiles());
        } while (nextPageToken != null);
        return res;
    }

    public static List<File> getFiles(String rootId) throws Exception {

        //String time = new DateTime(lastModifiedTime).toString();
        LOG.d("getFiles-by", rootId);
        final String txt = "('%s' in parents and trashed = false) or ('%s' in parents and trashed = false and mimeType = '%s')";
        return exeQF(txt, rootId, rootId, MIME_FOLDER);
    }

    public static List<File> getFilesAll(boolean withTrashed) throws Exception {
        return withTrashed ? exeQ("") : exeQ("trashed = false");
    }

    public static File findLibreraSync() throws Exception {

        final List<File> files = exeQF("name = 'Librera' and 'root' in parents and mimeType = '%s' and trashed = false", MIME_FOLDER);
        debugPrint(files);
        if (files.size() > 0) {
            return files.get(0);
        } else {
            return null;
        }
    }

    public static void debugPrint(List<File> list) {

        LOG.d(TAG, list.size());
        for (File f : list) {
            LOG.d(TAG, f.getId(), f.getName(), f.getMimeType(), f.getParents(), f.getCreatedTime(), f.getModifiedTime(), "trashed", f.getTrashed());
            LOG.d(f);
        }
    }

    public static File getFileById(String roodId, String name) throws IOException {
        LOG.d(TAG, "Get file", roodId, name);
        name = name.replace("'", "\\'");
        final List<File> files = exeQF("'%s' in parents and name='%s' and trashed = false", roodId, name);
        if (files != null && files.size() >= 1) {
            final File file = files.get(0);
            return file;
        }

        return null;
    }

    public static File getOrCreateLock(String roodId, long modifiedTime) throws IOException {
        File file = getFileById(roodId, "lock");
        if (file == null) {
            File metadata = new File()
                    .setParents(Collections.singletonList(roodId))
                    .setModifiedTime(new DateTime(modifiedTime))
                    .setMimeType("text/plain")
                    .setName("lock");

            LOG.d(TAG, "Create lock", roodId, "lock");
            debugOut += "\nCreate lock: " + new DateTime(modifiedTime).toStringRfc3339();
            file = googleDriveService.files().create(metadata).execute();
        }
        return file;
    }

    public static void updateLock(String roodId, long modifiedTime) throws IOException {
        File file = getOrCreateLock(roodId, modifiedTime);
        File metadata = new File().setModifiedTime(new DateTime(modifiedTime));

        debugOut += "\nUpdate lock: " + new DateTime(modifiedTime).toStringRfc3339();
        GFile.googleDriveService.files().update(file.getId(), metadata).execute();
    }

    public static File createFile(String roodId, String name, String content, long lastModifiedtime) throws IOException {
        File file = getFileById(roodId, name);
        if (file == null) {
            File metadata = new File()
                    .setParents(Collections.singletonList(roodId))
                    .setModifiedTime(new DateTime(lastModifiedtime))
                    .setMimeType("text/plain")
                    .setName(name);

            LOG.d(TAG, "Create file", roodId, name);
            file = googleDriveService.files().create(metadata).execute();
        }

        File metadata = new File().setName(name).setModifiedTime(new DateTime(lastModifiedtime));
        ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);
        LOG.d(TAG, "Create file with content", roodId, name);
        GFile.googleDriveService.files().update(file.getId(), metadata, contentStream).execute();

        return file;
    }


    public static File getFileInfo(String roodId, final java.io.File inFile) throws IOException {
        File file = getFileById(roodId, inFile.getName());
        if (file == null) {
            File metadata = new File()
                    .setParents(Collections.singletonList(roodId))
                    .setMimeType(ExtUtils.getMimeType(inFile))
                    .setModifiedTime(new DateTime(getLastModified(inFile)))
                    .setName(inFile.getName());

            LOG.d(TAG, "Create file", roodId, inFile.getName());
            file = googleDriveService.files().create(metadata).execute();
        }
        return file;

    }

    public static File createFirstTime(String roodId, final java.io.File inFile) throws IOException {
        File metadata = new File()
                .setParents(Collections.singletonList(roodId))
                .setMimeType(ExtUtils.getMimeType(inFile))
                .setModifiedTime(new DateTime(getLastModified(inFile)))
                .setName(inFile.getName());

        LOG.d(TAG, "Create file", roodId, inFile.getName());
        return googleDriveService.files().create(metadata).execute();
    }


    public static void uploadFile(String roodId, File file, final java.io.File inFile) throws IOException {
        debugOut += "\nUpload: " + inFile.getParentFile().getParentFile().getName() + "/" + inFile.getParentFile().getName() + "/" + inFile.getName();

        setLastModifiedTime(inFile, inFile.lastModified());
        File metadata = new File().setName(inFile.getName()).setModifiedTime(new DateTime(inFile.lastModified()));
        FileContent contentStream = new FileContent(ExtUtils.getMimeType(inFile), inFile);


        file.setModifiedTime(new DateTime(inFile.lastModified()));
        googleDriveService.files().update(file.getId(), metadata, contentStream).execute();

        LOG.d(TAG, "Upload: " + inFile.getParentFile().getParentFile().getName() + "/" + inFile.getParentFile().getName() + "/" + inFile.getName());


    }


    public static String readFileAsString(String fileId) throws IOException {

        LOG.d(TAG, "read file as string", fileId);
        //File metadata = googleDriveService.files().get(fileId).execute();
        //String name = metadata.getName();

        // Stream the file contents to a String.
        try (InputStream is = googleDriveService.files().get(fileId).executeMediaAsInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            String contents = stringBuilder.toString();

            return contents;
        }


    }


    public static void downloadFile(String fileId, java.io.File file, long lastModified) throws IOException {

        //file = new java.io.File(TxtUtils.fixFilePath(file.getPath()));

        LOG.d(TAG, "Download: " + file.getParentFile().getParentFile().getName() + "/" + file.getName());
        debugOut += "\nDownload: " + file.getParentFile().getParentFile().getName() + "/" + file.getParentFile().getName() + "/" + file.getName();
        InputStream is = null;
        //if (!file.getPath().endsWith("json")) {
        //    is = googleDriveService.files().get(fileId).executeMediaAsInputStream();
        //} else {
        // }
        java.io.File temp = new java.io.File(file.getPath() + ".temp");
        try {
            try {
                is = googleDriveService.files().get(fileId).executeMediaAsInputStream();
            } catch (IOException e) {
                is = googleDriveService.files().get(fileId).executeAsInputStream();
            }


            final boolean result = IO.copyFile(is, temp);
            if (result) {
                IO.copyFile(temp, file);
                setLastModifiedTime(file, lastModified);

                if (Clouds.isLibreraSyncFile(file.getPath())) {
                    IMG.clearCache(file.getPath());
                    FileMeta meta = AppDB.get().getOrCreate(file.getPath());
                    FileMetaCore.createMetaIfNeed(file.getPath(), true);
                    //IMG.loadCoverPageWithEffect(meta.getPath(), IMG.getImageSize());

                }


            }
        } finally {
            temp.delete();
        }

        //LOG.d(TAG, "downloadFile-lastModified after", file.lastModified(), lastModified, file.getName());

    }

    public static void downloadTemp(String fileId, java.io.File file) throws IOException {
        LOG.d(TAG, "Download: " + file.getParentFile().getName() + "/" + file.getName());
        debugOut += "\nDownload: " + file.getParentFile().getName() + "/" + file.getName();
        InputStream is = null;
        java.io.File temp = new java.io.File(file.getPath() + ".temp");
        try {
            try {
                is = googleDriveService.files().get(fileId).executeMediaAsInputStream();
            } catch (IOException e) {
                is = googleDriveService.files().get(fileId).executeAsInputStream();
            }

            final boolean result = IO.copyFile(is, temp);
            if (result) {
                IO.copyFile(temp, file);
            }
        } finally {
            temp.delete();
        }
    }

    public static void setLastModifiedTime(java.io.File file, long lastModified) {
        if (file.isFile()) {
            for (String key : sp.getAll().keySet()) {
                if (key.startsWith(file.getPath())) {
                    sp.edit().remove(key).commit();
                    LOG.d("hasLastModified remove", key);
                }
            }
        }
        sp.edit().putLong(file.getPath() + file.lastModified(), lastModified).commit();
        LOG.d("hasLastModified put", file.getPath() + file.lastModified(), lastModified);

    }

    public static boolean hasLastModified(java.io.File file) {
        for (String key : sp.getAll().keySet()) {
            if (key.startsWith(file.getPath())) {
                return true;
            }
        }
        return false;
    }

    public static long getLastModified(java.io.File file) {
        if (file.lastModified() == 0) {
            return 0;
        }
        return sp.getLong(file.getPath() + file.lastModified(), file.lastModified());
    }


    private static void deleteFile(File file, long lastModified) throws IOException {
        File metadata = new File().setTrashedTime(new DateTime(lastModified)).setModifiedTime(new DateTime(lastModified)).setTrashed(true);
        LOG.d("Delete", file.getName());
        debugOut += "\nDelete: " + file.getName();
        googleDriveService.files().update(file.getId(), metadata).execute();

    }


    public static File createFolder(String roodId, String name) throws IOException {
        File folder = getFileById(roodId, name);
        if (folder != null) {
            return folder;
        }
        LOG.d(TAG, "Create folder", roodId, name);
        debugOut += "\nCreate remote folder: " + name;
        File metadata = new File()
                .setParents(Collections.singletonList(roodId))
                //.setModifiedTime(new DateTime(lastModified))
                .setMimeType(MIME_FOLDER)
                .setName(name);

        return googleDriveService.files().create(metadata).execute();

    }


    public static volatile boolean isNeedUpdate = false;


    public static synchronized void sycnronizeAll(final Context c) throws Exception {


        try {
            isNeedUpdate = false;
            debugOut += "\n ----------------------------------";
            debugOut += "\nBegin: " + DateFormat.getTimeInstance().format(new Date());
            buildDriveService(c);
            LOG.d(TAG, "sycnronizeAll", "begin");
            if (TxtUtils.isEmpty(AppSP.get().syncRootID)) {
                File syncRoot = GFile.findLibreraSync();
                LOG.d(TAG, "findLibreraSync finded", syncRoot);
                if (syncRoot == null || syncRoot.getTrashed() == true) {
                    syncRoot = GFile.createFolder("root", "Librera");
                    debugOut += "\n Create remote [Librera]";
                }
                AppSP.get().syncRootID = syncRoot.getId();
                AppProfile.save(c);
            } else {
//                try {
//                    final File execute = GFile.googleDriveService.files().get(AppSP.get().syncRootID).execute();
//                    if (execute.getTrashed() == true) {
//                        File syncRoot = GFile.createFolder("root", "Librera");
//                        debugOut += "\n Create remote [Librera]";
//                        AppSP.get().syncRootID = syncRoot.getId();
//                        AppProfile.save(c);
//                    }
//                } catch (GoogleJsonResponseException e) {
//                    LOG.e(e);
//                    if (e.getDetails().getCode() == 404) {
//                        File syncRoot = GFile.createFolder("root", "Librera");
//                        debugOut += "\n Create remote [Librera]";
//                        AppSP.get().syncRootID = syncRoot.getId();
//                        AppProfile.save(c);
//                    }
//                }


            }


            //googleDriveService.files().update( AppSP.get().syncRootID, metadata).execute();


            if (!AppProfile.SYNC_FOLDER_ROOT.exists()) {
                sp.edit().clear().commit();
                AppProfile.SYNC_FOLDER_ROOT.mkdirs();
                debugOut += "\n Create local [Librera]";
            }


            LOG.d("Begin");
            SharedBooks.cache.clear();

            sync(AppSP.get().syncRootID, AppProfile.SYNC_FOLDER_ROOT);

            //updateLock(AppState.get().syncRootID, beginTime);

            LOG.d(TAG, "sycnronizeAll", "finished");
            debugOut += "\nEnd: " + DateFormat.getTimeInstance().format(new Date());


            TagData.restoreTags();



        } catch (IOException e) {
            debugOut += "\nException: " + e.getMessage();
            LOG.e(e);
            throw e;
        }
    }

    public static boolean deleteRemoteFile(final java.io.File ioFile) {
        try {
            final File file = map2.get(ioFile);
            if (file != null) {
                deleteFile(file, System.currentTimeMillis());
                //Thread.sleep(5000);
                return true;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    static Map<java.io.File, File> map2 = new HashMap<>();

    public static long timeout = 0;

    private static void sync(final String syncId, final java.io.File ioRoot) throws Exception {

//        if (System.currentTimeMillis() - timeout < 10 * 1000) {
//            debugOut += "\n 10 sec time-out";
//            return;
//        }
//        timeout = System.currentTimeMillis();

        final List<File> driveFiles = getFilesAll(true);
        LOG.d(TAG, "getFilesAll", "end");
//        if (LOG.isEnable) {
//            FileWriter out = new FileWriter(new java.io.File(BookCSS.get().downlodsPath, "dump-sync.txt"));
//            for (File file : driveFiles) {
//                out.write(file.toString() + "\n");
//            }
//            out.flush();
//            out.close();
//        }


        Map<String, File> map = new HashMap<>();
        map2.clear();


        for (File file : driveFiles) {
            map.put(file.getId(), file);
        }

        for (File file : driveFiles) {
            String filePath = findFile(file, map);

            if (filePath.startsWith(SKIP)) {
                continue;
            }
            //filePath = TxtUtils.fixFilePath(filePath);

            java.io.File local = new java.io.File(ioRoot, filePath);

            final File other = map2.get(local);
            if (other == null) {
                map2.put(local, file);
                LOG.d(TAG, "map2-put-1", file.getName(), file.getId(), file.getTrashed());
            } else if (file.getModifiedTime().getValue() > other.getModifiedTime().getValue()) {
                map2.put(local, file);
                LOG.d(TAG, "map2-put-2", file.getName(), file.getId(), file.getModifiedTime(), file.getTrashed());
            }
        }

        for (java.io.File local : map2.keySet()) {
            File remote = map2.get(local);
            if (remote.getTrashed() && local.exists()) {

                LOG.d("CHECK-to-REMOVE", local.getPath(), remote.getModifiedTime().getValue(), getLastModified(local));

                if (remote.getModifiedTime().getValue() - getLastModified(local) > 0) {
                    debugOut += "\nDelete local: " + local.getPath();
                    LOG.d(TAG, "Delete locale", local.getPath());
                    ExtUtils.deleteRecursive(local);
                    isNeedUpdate = true;
                }
            }

        }


        //upload second files
        for (File remote : driveFiles) {
            if (remote.getTrashed()) {
                LOG.d(TAG, "Skip trashed", remote.getName());
                continue;
            }
            boolean skip = false;
            if (!MIME_FOLDER.equals(remote.getMimeType())) {
                String filePath = findFile(remote, map);
                if (filePath.startsWith(SKIP)) {
                    LOG.d(TAG, "Skip", filePath);
                    continue;
                }

                //filePath = TxtUtils.fixFilePath(filePath);

                java.io.File local = new java.io.File(ioRoot, filePath);

                if (!hasLastModified(local) || (!local.getName().endsWith(".json") && local.length() == remote.getSize().longValue())) {
                    setLastModifiedTime(local, remote.getModifiedTime().getValue());
                    skip = true;
                    //debugOut += "\n skip: " + local.getName();
                    LOG.d(TAG, "Skip", local.getName());
                }


                if (!skip && compareBySizeModifiedTime(remote, local) > 0) {
                    final java.io.File parentFile = local.getParentFile();
                    if (parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    downloadFile(remote.getId(), local, remote.getModifiedTime().getValue());
                    isNeedUpdate = true;
                }
            }
        }
        syncUpload(syncId, ioRoot, map2);
    }

    public static long compareBySizeModifiedTime(File remote, java.io.File local) {
        if (!(remote.getName().endsWith("json") || remote.getName().endsWith("playlist"))) {
            if (remote.getSize() != null && remote.getSize().longValue() == local.length()) {
                LOG.d("compareBySizeModifiedTime-1: 0", remote.getName(), local.getPath());
                return 0;
            }
        }

        final long res = remote.getModifiedTime().getValue() - getLastModified(local);
        LOG.d("compareBySizeModifiedTime-2: " + res, remote.getName(), local.getPath());
        return res;
    }

    private static void syncUpload(String syncId, java.io.File ioRoot, Map<java.io.File, File> map2) throws IOException {
        java.io.File[] files = ioRoot.listFiles();
        if (files == null) {
            return;
        }
        for (java.io.File local : files) {
            File remote = map2.get(local);
//            if (remote != null && remote.getTrashed() == true) {
//                remote = null;
//            }
            if (local.isDirectory()) {
                if (remote == null) {
                    remote = createFolder(syncId, local.getName());
                }
                syncUpload(remote.getId(), local, map2);
            } else {
                if (remote == null) {
                    File add = createFirstTime(syncId, local);
                    uploadFile(syncId, add, local);
                } else if (compareBySizeModifiedTime(remote, local) < 0) {
                    uploadFile(syncId, remote, local);
                }


            }
        }
    }


    private static String findFile(File file, Map<String, File> map) {
        if (file == null) {
            return SKIP;
        }
        if (file.getParents() == null) {
            return SKIP;
        }

        if (file.getId().equals(AppSP.get().syncRootID)) {
            return "";
        }

        return findFile(map.get(file.getParents().get(0)), map) + "/" + file.getName();
    }


    public static void runSyncService(Activity a) {
        runSyncService(a, false);

    }


    public static void runSyncService(Activity a, boolean force) {

        try {
            if (AppSP.get().isEnableSync && !BooksService.isRunning) {
//                if (!force && BookCSS.get().isSyncPullToRefresh) {
//                    LOG.d("runSyncService", "manual sync only");
//                    return;
//                }
                if (BookCSS.get().isSyncWifiOnly && !Apps.isWifiEnabled(a)) {
                    LOG.d("runSyncService", "wifi not available");
                    return;
                }

                GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(a);
                if (account != null) {
                    GFile.buildDriveService(a);
                    BooksService.startForeground(a, BooksService.ACTION_RUN_SYNCRONICATION);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }


    }


}

