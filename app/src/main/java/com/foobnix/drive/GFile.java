package com.foobnix.drive;

import android.app.Activity;
import android.content.Context;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

public class GFile {
    public static final int REQUEST_CODE_SIGN_IN = 1110;

    public static final String MIME_FOLDER = "application/vnd.google-apps.folder";

    public static final String TAG = "GFile";

    public static com.google.api.services.drive.Drive googleDriveService;

    public static void init(Activity c) {

        if (googleDriveService != null) {
            return;
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(c);

        if (account == null) {


            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                            .build();
            GoogleSignInClient client = GoogleSignIn.getClient(c, signInOptions);

            // The result of the sign-in Intent is handled in onActivityResult.
            c.startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        } else {

            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            c, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());
            googleDriveService =
                    new com.google.api.services.drive.Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName(Apps.getApplicationName(c))
                            .build();
        }
    }

    public static List<File> getFiles(String rootId) {
        try {
            LOG.d("getFiles by", rootId);
            FileList drive = googleDriveService.files().list().setQ("'" + rootId + "' in parents and trashed = false").setFields("nextPageToken, files(*)").setSpaces("drive").setPageSize(1000).execute();
            return drive.getFiles();
        } catch (Exception e) {
            LOG.e(e);
            return Collections.emptyList();
        }
    }

    public static File findLibreraSync() {
        try {

            FileList drive = googleDriveService.files().list().setQ("name = 'Librera' and 'root' in parents and mimeType = '" + MIME_FOLDER + "' and trashed = false").setFields("nextPageToken, files(*)").setSpaces("drive").setPageSize(1000).execute();
            debugPrint(drive.getFiles());
            for (File it : drive.getFiles()) {
                if (it.getParents() == null) {
                    return it;
                }
            }
        } catch (Exception e) {
            LOG.e(e);

        }
        return null;
    }

    public static void debugPrint(List<File> list) {

        LOG.d(TAG, list.size());
        for (File f : list) {
            LOG.d(TAG, f.getId(), f.getName(), f.getMimeType(), f.getParents(), f.getCreatedTime(), f.getModifiedTime(), "trashed", f.getTrashed());
            LOG.d(f);
        }
    }

    public static File getFileById(String roodId, String name) {
        try {
            LOG.d(TAG, "Get file", roodId, name);
            FileList drive = googleDriveService.files().list().setQ(String.format("'%s' in parents and name='%s' and trashed = false", roodId, name)).setFields("nextPageToken, files(*)").setSpaces("drive").setPageSize(10).execute();
            if (drive.getFiles() != null && drive.getFiles().size() >= 1) {
                final File file = drive.getFiles().get(0);
                if (file.getModifiedTime() != null) {
                    LOG.d(TAG, "Get file getModifiedTime", file.getModifiedTime().getValue());
                }
                return file;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;
    }


    public static File createFile(String roodId, String name, String content) {
        try {
            File file = getFileById(roodId, name);
            if (file == null) {
                File metadata = new File()
                        .setParents(Collections.singletonList(roodId))
                        .setModifiedTime(new DateTime(System.currentTimeMillis()))
                        .setMimeType("text/plain")
                        .setName(name);

                LOG.d(TAG, "Create file", roodId, name);
                file = googleDriveService.files().create(metadata).execute();
            }

            File metadata = new File().setName(name).setModifiedTime(new DateTime(System.currentTimeMillis()));
            ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);
            LOG.d(TAG, "Create file with content", roodId, name);
            GFile.googleDriveService.files().update(file.getId(), metadata, contentStream).execute();

            return file;
        } catch (Exception e) {
            LOG.e(e);
        }
        return null;

    }

    public static File getFileInfo(String roodId, final java.io.File inFile) throws IOException {
        File file = getFileById(roodId, inFile.getName());
        if (file == null) {
            File metadata = new File()
                    .setParents(Collections.singletonList(roodId))
                    .setMimeType(ExtUtils.getMimeType(inFile))
                    .setModifiedTime(new DateTime(inFile.lastModified()))
                    .setName(inFile.getName());

            LOG.d(TAG, "Create file", roodId, inFile.getName());
            file = googleDriveService.files().create(metadata).execute();
        }
        return file;

    }


    public static void uploadFile(String roodId, String fileId, final java.io.File inFile) {
        try {
            File metadata = new File().setName(inFile.getName()).setModifiedTime(new DateTime(inFile.lastModified()));

            FileContent contentStream = new FileContent("text/plain", inFile);
            LOG.d(TAG, "upload file", roodId, inFile);
            googleDriveService.files().update(fileId, metadata, contentStream).execute();

        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public static String readFileAsString(String fileId) {
        try {
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
        } catch (Exception e) {
            LOG.e(e);
        }
        return "";
    }

    public static void downloadFile(String fileId, java.io.File file) throws IOException {
        LOG.d(TAG, "download file", fileId, file.getPath());
        InputStream is = googleDriveService.files().get(fileId).executeAsInputStream();
        IO.copyFile(is, file);
    }

    public static File createFolder(String roodId, String name) throws IOException {
        File folder = getFileById(roodId, name);
        if (folder != null) {
            return folder;
        }
        LOG.d(TAG, "Create folder", roodId, name);
        File metadata = new File()
                .setParents(Collections.singletonList(roodId))
                .setMimeType(MIME_FOLDER)
                .setName(name);

        return googleDriveService.files().create(metadata).execute();

    }

    public static void sycnronizeAll(final Context c) throws IOException {
        LOG.d(TAG, "sycnronizeAll", "begin");
        if (TxtUtils.isEmpty(AppState.get().syncRootID)) {
            LOG.d(TAG, "syncRootID", AppState.get().syncRootID);
            File syncRoot = GFile.findLibreraSync();
            LOG.d(TAG, "findLibreraSync", syncRoot);
            if (syncRoot == null) {
                syncRoot = GFile.createFolder("root", "Librera");
            }
            AppState.get().syncRootID = syncRoot.getId();
            AppState.get().save(c);
        }
        sync(AppState.get().syncRootID, AppsConfig.SYNC_FOLDER_ROOT);
        LOG.d(TAG, "sycnronizeAll", "finished");
    }

    private static void sync(String syncId, final java.io.File ioRoot) throws IOException {
        LOG.d(TAG, "sync", syncId, ioRoot.getPath());
        final java.io.File[] files = ioRoot.listFiles();

        if (files == null) {
            return;
        }

        for (java.io.File file : files) {
            if (file.isDirectory()) {
                File folder = createFolder(syncId, file.getName());
                sync(folder.getId(), file);
            } else {
                final File syncFile = getFileInfo(syncId, file);
                if (syncFile.getModifiedTime() == null || syncFile.getModifiedTime().getValue() < file.lastModified()) {
                    uploadFile(syncId, syncFile.getId(), file);
                } else if (syncFile.getModifiedTime().getValue() > file.lastModified()) {
                    downloadFile(syncId, file);
                }
            }
        }
        final List<File> rFiles = getFiles(syncId);
        for (File remote : rFiles) {
            if (!MIME_FOLDER.equals(remote.getMimeType())) {
                java.io.File lFile = new java.io.File(ioRoot, remote.getName());
                if (!lFile.exists()) {
                    downloadFile(remote.getId(), lFile);
                }
            }
        }

    }
}

