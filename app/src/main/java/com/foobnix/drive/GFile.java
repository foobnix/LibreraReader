package com.foobnix.drive;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.IO;
import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.TagData;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.BooksService;
import com.foobnix.ui2.FileMetaCore;
import com.foobnix.work.SynctornizatoinWorker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.StartPageToken;

import org.ebookdroid.common.settings.books.SharedBooks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class GFile {
    public static final int REQUEST_CODE_SIGN_IN = 1110;


    public static final String MIME_FOLDER = "application/vnd.google-apps.folder";

    public static final String TAG = "GFile";
    public static final int PAGE_SIZE = 1000;
    /** Exactly the metadata the sync reads; keeps the listing response small. */
    private static final String LIST_FIELDS =
            "nextPageToken, files(id,name,mimeType,parents,size,trashed,createdTime,modifiedTime)";
    public static final String SKIP = "skip";
    /**
     * Suffix of the scratch file downloadContent() writes next to its target. It is deleted in a
     * finally block, but a process that dies mid-download (or a sync that is killed) leaves one
     * behind - and because it sits inside the synced folder, the next sync uploaded it to Drive
     * and every other device then downloaded it. These are never synced in either direction.
     */
    public static final String TEMP_SUFFIX = ".temp";
    public static final String MY_SCOPE = DriveScopes.DRIVE_FILE;
    public static final String LASTMODIFIED = "lastmodified2";

    public static com.google.api.services.drive.Drive googleDriveService;

    /**
     * Human-readable sync log shown in the debug dialog. A StringBuilder rather than repeated
     * String concatenation: several of the append sites sit inside per-file loops, which made
     * this quadratic on large libraries, and nothing ever trimmed it between runs.
     */
    public static final StringBuilder debugOut = new StringBuilder();
    private static final int DEBUG_OUT_MAX = 200_000;

    static void debug(String text) {
        synchronized (debugOut) {
            debugOut.append(text);
            if (debugOut.length() > DEBUG_OUT_MAX) {
                debugOut.delete(0, debugOut.length() - DEBUG_OUT_MAX);
            }
        }
    }

    public static void clearDebug() {
        synchronized (debugOut) {
            debugOut.setLength(0);
        }
    }


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
                            new NetHttpTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName(Apps.getApplicationName(c))
                            .build();
        }
        sp = c.getSharedPreferences(LASTMODIFIED, Context.MODE_PRIVATE);
        invalidateLastModifiedIndex();

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
                        new NetHttpTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(Apps.getApplicationName(c))
                        .build();

        LOG.d(TAG, "googleDriveService", " build");
        sp = c.getSharedPreferences(LASTMODIFIED, Context.MODE_PRIVATE);
        invalidateLastModifiedIndex();

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
        LOG.d(TAG, "exeQ", q);
        String nextPageToken = "";
        List<File> res = new ArrayList<File>();
        do {
            //debug("\n:" + q);

            // Only the fields below are ever read (see getName/getId/getModifiedTime/...).
            // files(*) returns the full metadata for every file, which is by far the largest
            // part of the sync payload. Ordering is dropped too - the results go into maps.
            final FileList list = (FileList) googleDriveService.files()
                    .list()
                    .setSpaces("drive")
                    .setQ(q)
                    .setPageToken(nextPageToken)
                    .setFields(LIST_FIELDS)
                    .setPageSize(PAGE_SIZE)
                    .execute();
            nextPageToken = list.getNextPageToken();
            res.addAll(list.getFiles());
            debug("\nGet remote files info: " + list.getFiles().size());
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
            debug("\nCreate lock: " + new DateTime(modifiedTime).toStringRfc3339());
            file = googleDriveService.files().create(metadata).execute();
        }
        return file;
    }

    public static void updateLock(String roodId, long modifiedTime) throws IOException {
        File file = getOrCreateLock(roodId, modifiedTime);
        File metadata = new File().setModifiedTime(new DateTime(modifiedTime));

        debug("\nUpdate lock: " + new DateTime(modifiedTime).toStringRfc3339());
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
        debug("\nUpload: " + inFile.getParentFile().getParentFile().getName() + "/" + inFile.getParentFile().getName() + "/" + inFile.getName());

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
        if (downloadContent(fileId, file, lastModified)) {
            afterDownload(file);
        }
    }

    /**
     * Transfers one file. Safe to run on several threads at once: it only touches the network,
     * a temp path unique to this file, and the synchronized timestamp store.
     *
     * @return true when the file was written and still needs registering via afterDownload.
     */
    private static boolean downloadContent(String fileId, java.io.File file, long lastModified) throws IOException {
        LOG.d(TAG, "Download: " + file.getParentFile().getParentFile().getName() + "/" + file.getName());
        debug("\nDownload: " + file.getParentFile().getParentFile().getName() + "/" + file.getParentFile().getName() + "/" + file.getName());
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
                setLastModifiedTime(file, lastModified);
                return true;
            }
            return false;
        } finally {
            temp.delete();
        }
    }

    /**
     * Post-download bookkeeping. Deliberately kept off the download threads - the Glide cache,
     * FileMeta and the greenDAO session are shared state that is not written for concurrent use.
     */
    private static void afterDownload(java.io.File file) {
        if (!Clouds.isLibreraSyncFile(file.getPath())) {
            return;
        }
        try {
            IMG.clearCache(file.getPath());
            AppDB.get().getOrCreate(file.getPath());
            FileMetaCore.createMetaIfNeed(file.getPath(), true);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void downloadTemp(String fileId, java.io.File file) throws IOException {
        LOG.d(TAG, "Download: " + file.getParentFile().getName() + "/" + file.getName());
        debug("\nDownload: " + file.getParentFile().getName() + "/" + file.getName());
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

    /*
     * Timestamp bookkeeping.
     *
     * Keys are "<path><local mtime>", so answering "is there an entry for this path?" is a
     * prefix query. The original code answered it by calling sp.getAll() - which clones the
     * entire preference map - once per remote file, and wrote with a blocking commit() per
     * removed key. On a large library that is O(n^2) map copies plus an fsync per file, and it
     * was the slowest part of the sync.
     *
     * The keys are mirrored into a sorted set instead: ceiling()/tailSet() answer the same
     * prefix query in O(log n) with identical semantics, and writes are batched into one
     * asynchronous apply() per file.
     */
    private static TreeSet<String> keyIndex;

    /**
     * Private monitor, deliberately NOT the class monitor.
     *
     * sycnronizeAll() is static synchronized, so it holds GFile.class for the whole sync. If
     * these methods were static synchronized too, the parallel download threads would block on
     * a lock the sync thread owns while it waits for them to finish - a deadlock that stalls the
     * sync right after the first batch of downloads starts.
     */
    private static final Object TIMESTAMP_LOCK = new Object();

    private static TreeSet<String> keyIndex() {
        synchronized (TIMESTAMP_LOCK) {
            if (keyIndex == null) {
                keyIndex = new TreeSet<>(sp.getAll().keySet());
            }
            return keyIndex;
        }
    }

    /** Drop the cached index; call when the backing preferences are cleared wholesale. */
    public static void invalidateLastModifiedIndex() {
        synchronized (TIMESTAMP_LOCK) {
            keyIndex = null;
        }
    }

    public static void setLastModifiedTime(java.io.File file, long lastModified) {
        synchronized (TIMESTAMP_LOCK) {
        final TreeSet<String> index = keyIndex();
        final String path = file.getPath();
        final SharedPreferences.Editor editor = sp.edit();

        if (file.isFile()) {
            // Same prefix sweep as before, but over the sorted index and in a single batch.
            final List<String> stale = new ArrayList<>();
            for (String key : index.tailSet(path)) {
                if (!key.startsWith(path)) {
                    break;
                }
                stale.add(key);
            }
            for (String key : stale) {
                editor.remove(key);
                index.remove(key);
                LOG.d("hasLastModified remove", key);
            }
        }

        final String key = path + file.lastModified();
        editor.putLong(key, lastModified);
        index.add(key);
        // apply() instead of commit(): the timestamps are a cache, and a blocking disk write
        // per file is what made this dominate the sync.
        editor.apply();
        LOG.d("hasLastModified put", key, lastModified);
        }
    }

    public static boolean hasLastModified(java.io.File file) {
        synchronized (TIMESTAMP_LOCK) {
            final String path = file.getPath();
            final String candidate = keyIndex().ceiling(path);
            return candidate != null && candidate.startsWith(path);
        }
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
        debug("\nDelete: " + file.getName());
        googleDriveService.files().update(file.getId(), metadata).execute();

    }


    public static File createFolder(String roodId, String name) throws IOException {
        File folder = getFileById(roodId, name);
        if (folder != null) {
            return folder;
        }
        LOG.d(TAG, "Create folder", roodId, name);
        debug("\nCreate remote folder: " + name);
        File metadata = new File()
                .setParents(Collections.singletonList(roodId))
                //.setModifiedTime(new DateTime(lastModified))
                .setMimeType(MIME_FOLDER)
                .setName(name);

        return googleDriveService.files().create(metadata).execute();

    }


    public static volatile boolean isNeedUpdate = false;


    /*
     * changes.list probe.
     *
     * Deliberately used only to answer "did anything change at all?", not to drive an
     * incremental tree cache. The token lives in app-private preferences and never on Drive:
     * getFilesAll() pulls everything and syncUpload() uploads everything it sees, so a cache
     * file inside the Librera folder would be downloaded by older builds, shown in the library
     * and synced between devices.
     *
     * When anything has changed the code falls through to exactly the same full listing as
     * before, which keeps behaviour identical to older versions and avoids two traps: the feed
     * lags files.list (so a cached tree can be stale and lose a just-uploaded file), and
     * modifiedTime is written from the uploading device's clock, so feed order does not imply
     * modifiedTime order.
     */
    private static final String SYNC_STATE_PREFS = "gfile_sync_state";
    private static final String KEY_CHANGES_TOKEN = "changesToken";
    private static final String KEY_LOCAL_SIGNATURE = "localSignature";

    private static SharedPreferences syncState(Context c) {
        return c.getSharedPreferences(SYNC_STATE_PREFS, Context.MODE_PRIVATE);
    }

    /** @return true if Drive reports changes since the stored token, or if we cannot tell. */
    private static boolean hasRemoteChanges(Context c) {
        try {
            final String token = syncState(c).getString(KEY_CHANGES_TOKEN, null);
            if (token == null) {
                return true;
            }
            String page = token;
            String newToken = null;
            boolean changed = false;
            while (page != null) {
                final ChangeList list = (ChangeList) googleDriveService.changes()
                        .list(page)
                        .setSpaces("drive")
                        .setIncludeRemoved(true)
                        .setPageSize(PAGE_SIZE)
                        .setFields("newStartPageToken,nextPageToken,changes(fileId)")
                        .execute();
                if (list.getChanges() != null && !list.getChanges().isEmpty()) {
                    changed = true;
                }
                if (list.getNewStartPageToken() != null) {
                    newToken = list.getNewStartPageToken();
                }
                page = list.getNextPageToken();
            }
            if (!changed && newToken != null) {
                // Only advance when nothing changed. If something did, the token is refreshed
                // after the full sync succeeds - never before, or a failed sync would lose the
                // signal and the work would be skipped next time.
                syncState(c).edit().putString(KEY_CHANGES_TOKEN, newToken).apply();
            }
            return changed;
        } catch (Exception e) {
            LOG.e(e);
            return true;
        }
    }

    private static void rememberSyncedState(Context c, java.io.File root) {
        try {
            final StartPageToken startPage = (StartPageToken) googleDriveService.changes()
                    .getStartPageToken()
                    .execute();
            final String token = startPage.getStartPageToken();
            syncState(c).edit()
                    .putString(KEY_CHANGES_TOKEN, token)
                    .putLong(KEY_LOCAL_SIGNATURE, localSignature(root))
                    .apply();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    /** Cheap local fingerprint (paths, sizes, mtimes) used to notice local edits. */
    private static long localSignature(java.io.File root) {
        long hash = 1125899906842597L;
        final Deque<java.io.File> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            final java.io.File[] children = stack.pop().listFiles();
            if (children == null) {
                continue;
            }
            for (java.io.File child : children) {
                hash = 31 * hash + child.getPath().hashCode();
                if (child.isDirectory()) {
                    stack.push(child);
                } else {
                    hash = 31 * hash + child.lastModified();
                    hash = 31 * hash + child.length();
                }
            }
        }
        return hash;
    }

    /** True when nothing changed on either side and there is genuinely no work to do. */
    private static boolean canSkipSync(Context c) {
        if (c == null || !AppProfile.SYNC_FOLDER_ROOT.exists()) {
            return false;
        }
        if (!pendingDeletions(c).isEmpty()) {
            return false;
        }
        final SharedPreferences state = syncState(c);
        if (!state.contains(KEY_CHANGES_TOKEN) || !state.contains(KEY_LOCAL_SIGNATURE)) {
            return false;
        }
        if (state.getLong(KEY_LOCAL_SIGNATURE, 0) != localSignature(AppProfile.SYNC_FOLDER_ROOT)) {
            return false;
        }
        return !hasRemoteChanges(c);
    }

    public static synchronized void sycnronizeAll(final Context c) throws Exception {


        try {
            isNeedUpdate = false;
            debug("\n ----------------------------------");
            debug("\nBegin: " + DateFormat.getTimeInstance().format(new Date()));
            buildDriveService(c);
            LOG.d(TAG, "sycnronizeAll", "begin");

            if (canSkipSync(c)) {
                // One changes.list call and a local stat walk, instead of paging the whole
                // drive and re-walking every file.
                LOG.d(TAG, "sycnronizeAll", "nothing changed, skipping");
                debug("\nNothing changed - skipped");
                return;
            }
            if (TxtUtils.isEmpty(AppSP.get().syncRootID)) {
                File syncRoot = GFile.findLibreraSync();
                LOG.d(TAG, "findLibreraSync finded", syncRoot);
                if (syncRoot == null || syncRoot.getTrashed() == true) {
                    syncRoot = GFile.createFolder("root", "Librera");
                    debug("\n Create remote [Librera]");
                }
                AppSP.get().syncRootID = syncRoot.getId();
                AppProfile.save(c);
            } else {
//                try {
//                    final File execute = GFile.googleDriveService.files().get(AppSP.get().syncRootID).execute();
//                    if (execute.getTrashed() == true) {
//                        File syncRoot = GFile.createFolder("root", "Librera");
//                        debug("\n Create remote [Librera]");
//                        AppSP.get().syncRootID = syncRoot.getId();
//                        AppProfile.save(c);
//                    }
//                } catch (GoogleJsonResponseException e) {
//                    LOG.e(e);
//                    if (e.getDetails().getCode() == 404) {
//                        File syncRoot = GFile.createFolder("root", "Librera");
//                        debug("\n Create remote [Librera]");
//                        AppSP.get().syncRootID = syncRoot.getId();
//                        AppProfile.save(c);
//                    }
//                }


            }


            //googleDriveService.files().update( AppSP.get().syncRootID, metadata).execute();


            if (!AppProfile.SYNC_FOLDER_ROOT.exists()) {
                sp.edit().clear().commit();
                invalidateLastModifiedIndex();
                AppProfile.SYNC_FOLDER_ROOT.mkdirs();
                debug("\n Create local [Librera]");
            }


            LOG.d("Begin");
            SharedBooks.cache.clear();

            sync(AppSP.get().syncRootID, AppProfile.SYNC_FOLDER_ROOT);

            //updateLock(AppState.get().syncRootID, beginTime);

            LOG.d(TAG, "sycnronizeAll", "finished");
            debug("\nEnd: " + DateFormat.getTimeInstance().format(new Date()));


            rememberSyncedState(c, AppProfile.SYNC_FOLDER_ROOT);

            TagData.restoreTags();


        } catch (IOException e) {
            debug("\nException: " + e.getMessage());
            LOG.e(e);
            throw e;
        }
    }

    /*
     * Local deletions are recorded persistently.
     *
     * map2 is only populated while sync() runs, so resolving the remote copy through it worked
     * solely when a sync had already happened in this process. After an app restart, or before
     * the first sync, or with no network at delete time, the lookup found nothing, the remote
     * copy stayed alive - and the next sync saw "remote exists, local missing" and downloaded
     * the book straight back. Callers ignore the return value, so the failure was silent.
     *
     * Recording the path means the next sync can still trash the remote copy, whenever it runs.
     */
    private static final String DELETED_PREFS = "gfile_deleted";
    private static final String DELETED_KEY = "paths";

    private static SharedPreferences deletedPrefs(Context c) {
        return c.getSharedPreferences(DELETED_PREFS, Context.MODE_PRIVATE);
    }

    private static final Object DELETION_LOCK = new Object();

    private static Set<String> pendingDeletions(Context c) {
      synchronized (DELETION_LOCK) {
        // getStringSet may hand back the live instance; copy before touching it.
        return new HashSet<>(deletedPrefs(c).getStringSet(DELETED_KEY, Collections.<String>emptySet()));
      }
    }

    private static void savePendingDeletions(Context c, Set<String> paths) {
        synchronized (DELETION_LOCK) {
            deletedPrefs(c).edit().putStringSet(DELETED_KEY, paths).apply();
        }
    }

    public static void markDeletedLocally(Context c, java.io.File ioFile) {
        if (c == null || ioFile == null) {
            return;
        }
        final Set<String> paths = pendingDeletions(c);
        if (paths.add(ioFile.getPath())) {
            savePendingDeletions(c, paths);
            LOG.d(TAG, "deletion recorded", ioFile.getPath());
        }
    }

    private static void clearDeletion(Context c, String path) {
        final Set<String> paths = pendingDeletions(c);
        if (paths.remove(path)) {
            savePendingDeletions(c, paths);
        }
    }

    /**
     * Trashes the remote copies of files deleted locally. Runs before the download pass so a
     * pending deletion cannot be undone by re-downloading the file first.
     */
    private static void applyPendingDeletions(Context c, Map<java.io.File, File> byLocalPath) {
        if (c == null) {
            return;
        }
        for (String path : pendingDeletions(c)) {
            final java.io.File local = new java.io.File(path);
            if (local.exists()) {
                // Came back locally - nothing to propagate.
                clearDeletion(c, path);
                continue;
            }
            final File remote = byLocalPath.get(local);
            try {
                if (remote != null && !Boolean.TRUE.equals(remote.getTrashed())) {
                    deleteFile(remote, System.currentTimeMillis());
                    // Mark it here too: driveFiles holds the same instances, so the later
                    // passes will skip it instead of downloading it again.
                    remote.setTrashed(true);
                }
                clearDeletion(c, path);
            } catch (Exception e) {
                // Leave it pending so the next sync retries.
                LOG.e(e);
            }
        }
    }

    public static boolean deleteRemoteFile(final java.io.File ioFile) {
        // Record first: if anything below fails, the next sync still removes the remote copy.
        markDeletedLocally(LibreraApp.context, ioFile);
        try {
            final File file = map2.get(ioFile);
            if (file != null) {
                deleteFile(file, System.currentTimeMillis());
                file.setTrashed(true);
                clearDeletion(LibreraApp.context, ioFile.getPath());
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
//            debug("\n 10 sec time-out");
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
        for (File file : driveFiles) {
            map.put(file.getId(), file);
        }

        map2.clear();
        map2.putAll(collapse(driveFiles, ioRoot, map));

        // Propagate deletions made on this device first, so a file that is pending removal is
        // not downloaded again by the pass below.
        applyPendingDeletions(LibreraApp.context, map2);

        for (java.io.File local : map2.keySet()) {
            File remote = map2.get(local);
            if (remote.getTrashed() && local.exists()) {

                LOG.d("CHECK-to-REMOVE", local.getPath(), remote.getModifiedTime().getValue(), getLastModified(local));

                if (remote.getModifiedTime().getValue() - getLastModified(local) > 0) {
                    debug("\nDelete local: " + local.getPath());
                    LOG.d(TAG, "Delete locale", local.getPath());
                    ExtUtils.deleteRecursive(local);
                    isNeedUpdate = true;
                }
            }

        }


        //upload second files
        final List<Download> pending = new ArrayList<>();
        for (File remote : driveFiles) {
            if (remote.getTrashed()) {
                LOG.d(TAG, "Skip trashed", remote.getName());
                continue;
            }
            boolean skip = false;
            if (!MIME_FOLDER.equals(remote.getMimeType())) {
                if (remote.getName() != null && remote.getName().endsWith(TEMP_SUFFIX)) {
                    LOG.d(TAG, "Skip temp", remote.getName());
                    continue;
                }
                String filePath = findFile(remote, map);
                if (filePath.startsWith(SKIP)) {
                    LOG.d(TAG, "Skip", filePath);
                    continue;
                }

                //filePath = TxtUtils.fixFilePath(filePath);

                java.io.File local = new java.io.File(ioRoot, filePath);

                final boolean sameSize = remote.getSize() != null
                        && !local.getName().endsWith(".json")
                        && local.length() == remote.getSize().longValue();
                if (!hasLastModified(local) || sameSize) {
                    setLastModifiedTime(local, remote.getModifiedTime().getValue());
                    skip = true;
                    //debug("\n skip: " + local.getName());
                    LOG.d(TAG, "Skip", local.getName());
                }


                if (!skip && compareBySizeModifiedTime(remote, local) > 0) {
                    final java.io.File parentFile = local.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    // Decide serially, transfer in parallel below: the decision reads and writes
                    // the shared timestamp store, and keeping it on one thread avoids ordering
                    // surprises there.
                    pending.add(new Download(remote.getId(), local, remote.getModifiedTime().getValue()));
                }
            }
        }

        downloadAll(pending);

        syncUpload(syncId, ioRoot, map2);
    }

    /** How many downloads run at once. */
    public static final int DOWNLOAD_THREADS = 4;
    private static final int DOWNLOAD_TIMEOUT_MINUTES = 30;

    private static final class Download {
        final String fileId;
        final java.io.File local;
        final long modifiedTime;

        Download(String fileId, java.io.File local, long modifiedTime) {
            this.fileId = fileId;
            this.local = local;
            this.modifiedTime = modifiedTime;
        }
    }

    /**
     * Fetches the queued files on a small thread pool.
     *
     * Only the transfer is parallel. The bookkeeping that follows a download - Glide cache,
     * FileMeta, the greenDAO session - runs afterwards on this thread, because it is shared
     * mutable state that was never written with concurrency in mind. Downloads are pure network
     * plus a write to a path unique per task, so they parallelise safely.
     */
    private static void downloadAll(List<Download> pending) throws IOException {
        if (pending.isEmpty()) {
            return;
        }
        LOG.d(TAG, "downloadAll", pending.size(), "threads", DOWNLOAD_THREADS);

        final ExecutorService pool = Executors.newFixedThreadPool(Math.min(DOWNLOAD_THREADS, pending.size()));
        final List<java.io.File> fetched = Collections.synchronizedList(new ArrayList<java.io.File>());
        final AtomicReference<Exception> failure = new AtomicReference<>();

        for (final Download task : pending) {
            pool.submit(new Runnable() {
                @Override public void run() {
                    if (failure.get() != null) {
                        return;
                    }
                    try {
                        downloadContent(task.fileId, task.local, task.modifiedTime);
                        fetched.add(task.local);
                    } catch (Exception e) {
                        LOG.e(e);
                        failure.compareAndSet(null, e);
                    }
                }
            });
        }

        pool.shutdown();
        try {
            // Bounded wait: a stalled connection must not leave the sync blocked forever.
            if (!pool.awaitTermination(DOWNLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                pool.shutdownNow();
                failure.compareAndSet(null, new IOException("Download timed out"));
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
            failure.compareAndSet(null, e);
        }

        // Serial tail: register everything that arrived.
        for (java.io.File file : fetched) {
            afterDownload(file);
            isNeedUpdate = true;
        }

        final Exception e = failure.get();
        if (e != null) {
            throw e instanceof IOException ? (IOException) e : new IOException(e);
        }
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
            if (remote != null && Boolean.TRUE.equals(remote.getTrashed())) {
                // The remote copy is in the trash, i.e. deleted on some device. Skip it:
                // uploading into a trashed entry is a silent no-op, and recreating it would
                // resurrect the book on every other device. Deleted stays deleted; the delete
                // pass above is what removes the leftover local copy.
                LOG.d(TAG, "Skip trashed remote", local.getPath());
                continue;
            }
            if (!local.isDirectory() && local.getName().endsWith(TEMP_SUFFIX)) {
                // Leftover scratch file from an interrupted download: delete it rather than
                // uploading it.
                LOG.d(TAG, "Delete leftover temp", local.getPath());
                local.delete();
                continue;
            }
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
                    if (isStale(remote)) {
                        // Another device changed this file after our listing was taken. Leave it
                        // alone; the next sync sees the fresh state and resolves it properly.
                        // files.get costs a fraction of an upload, and there are usually only a
                        // handful of uploads per sync, so this check is cheap insurance.
                        LOG.d(TAG, "Skip upload, remote moved on", local.getPath());
                        debug("\nSkip upload (remote changed): " + local.getName());
                    } else {
                        uploadFile(syncId, remote, local);
                    }
                }


            }
        }
    }


    /**
     * Resolves remote files to local paths, collapsing duplicates.
     *
     * Drive happily holds several files with the same path - createFirstTime does not check for
     * an existing entry, so deleting a book on one device and re-adding it on another produces
     * two ids for the same path. The tie-break is "newest modifiedTime wins", and it has to stay
     * in one place: if two code paths ever picked different winners, the devices would upload
     * over each other in a loop. Anything keyed by path must therefore be derived through here,
     * never cached as a path-to-id mapping.
     */
    public static Map<java.io.File, File> collapse(Collection<File> all, java.io.File ioRoot,
            Map<String, File> byId) {
        final Map<java.io.File, File> res = new HashMap<>();
        for (File file : all) {
            final String filePath = findFile(file, byId);
            if (filePath.startsWith(SKIP)) {
                continue;
            }
            final java.io.File local = new java.io.File(ioRoot, filePath);
            final File other = res.get(local);
            if (other == null) {
                res.put(local, file);
                LOG.d(TAG, "map2-put-1", file.getName(), file.getId(), file.getTrashed());
            } else if (file.getModifiedTime().getValue() > other.getModifiedTime().getValue()) {
                res.put(local, file);
                LOG.d(TAG, "map2-put-2", file.getName(), file.getId(), file.getModifiedTime(), file.getTrashed());
            }
        }
        return res;
    }

    /**
     * @return true when the remote file changed since the listing this sync is working from.
     *         Drive v3 has no conditional updates (ETags went away with v2), so this read is the
     *         only guard against clobbering a newer version.
     */
    private static boolean isStale(File cached) {
        try {
            final File fresh = googleDriveService.files().get(cached.getId())
                    .setFields("id,modifiedTime,size,trashed")
                    .execute();
            if (fresh.getModifiedTime() == null || cached.getModifiedTime() == null) {
                return false;
            }
            return fresh.getModifiedTime().getValue() != cached.getModifiedTime().getValue()
                    || Boolean.TRUE.equals(fresh.getTrashed());
        } catch (Exception e) {
            // Cannot tell - do not block the upload on a failed probe.
            LOG.e(e);
            return false;
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
                    //BooksService.startForeground(a, BooksService.ACTION_RUN_SYNCRONICATION);
                    OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SynctornizatoinWorker.class).build();
//                    WorkManager.getInstance(a).enqueue(workRequest);
                    WorkManager.getInstance(a).enqueueUniqueWork(AppsConfig.SYNC_DRIVE_WORKER_NAME, ExistingWorkPolicy.KEEP, workRequest);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }


    }


}

