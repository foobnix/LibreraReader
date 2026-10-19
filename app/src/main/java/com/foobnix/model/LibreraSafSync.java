package com.foobnix.model;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.widget.Toast;

import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.MainTabs2;
import com.foobnix.work.SearchAllBooksWorker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The profile - settings, reading progress, bookmarks - kept in the shared Librera folder for a
 * user who handed the app a folder of books but not the permission to read the whole storage.
 *
 * Without that permission the profile lives in the app's own storage, which nothing else sees and
 * an uninstall wipes. The user can hand over the Librera folder through the system's picker as
 * well, and this keeps the two in step: every file of every {@code profile.*} folder is copied to
 * whichever side has the older one. The profile code itself goes on reading and writing plain
 * files, as it does everywhere else; the Google Drive sync works the same way, see
 * {@link com.foobnix.drive.GFile}.
 *
 * A document uri's file cannot be given a modification time, so each copy is remembered: a file
 * that has changed since its last copy on one side only is copied over the other, and one changed
 * on both sides goes the way of the newer. A file deleted on one side is not deleted on the other.
 */
public class LibreraSafSync {

    public static final int REQUEST_CODE_LIBRERA_FOLDER = 125;

    private static final String SP_NAME = "LibreraSafSync";
    private static final String KEY_TREE = "tree";
    private static final String STATE_PREFIX = "f:";
    private static final String EXTERNAL_STORAGE = "com.android.externalstorage.documents";
    private static final String MIME_FILE = "application/octet-stream";
    private static final String LIBRERA = "Librera";

    /** The profile that only ever exists without the permission; it is not the user's to share. */
    private static final String LOCAL_ONLY_PROFILE = AppProfile.PROFILE_PREFIX + "Demo";

    private static final AtomicBoolean pushQueued = new AtomicBoolean();

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    private static Uri tree(Context c) {
        String tree = sp(c).getString(KEY_TREE, "");
        return TxtUtils.isEmpty(tree) ? null : Uri.parse(tree);
    }

    /**
     * Whether the profile is kept in a Librera folder handed over through the picker: one was,
     * the app may still write it, and it does not have the permission that makes it pointless.
     */
    public static boolean isActive(Context c) {
        if (c == null || Android6.canWrite(c)) {
            return false;
        }
        Uri tree = tree(c);
        if (tree == null) {
            return false;
        }
        try {
            for (UriPermission p : c.getContentResolver().getPersistedUriPermissions()) {
                if (p.getUri().equals(tree) && p.isWritePermission()) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    /** The system's folder picker, opened on the Librera folder when the system allows it. */
    public static void startPick(Activity a) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                        | Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The picker opens on Download/Librera, made here if need be - the app may make folders
            // in Download, and does keep the settings there - so the user has only to take it.
            // A folder that is not there is not opened on (the picker falls back to wherever it
            // was last), and the app cannot make one at the top of the storage without the
            // permission, so failing that it opens on the storage itself, which offers to create
            // one. The picker's own new folder dialog cannot be given a name.
            File download = downloadLibrera();
            download.mkdirs();
            String id = download.isDirectory() ? "primary:" + Environment.DIRECTORY_DOWNLOADS + "/" + LIBRERA : "primary:";
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                    DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE, id));
        }
        try {
            a.startActivityForResult(intent, REQUEST_CODE_LIBRERA_FOLDER);
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, R.string.fail, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Takes in the Librera folder the user picked: the profile there is brought in, the app moves
     * onto it from its own local one, and what is only here is written out.
     *
     * @return whether this was the picker's answer for the Librera folder
     */
    public static boolean onActivityResult(final Activity a, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_LIBRERA_FOLDER) {
            return false;
        }
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return true;
        }
        final Uri uri = data.getData();
        try {
            a.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            LOG.e(e);
            Toast.makeText(a, R.string.fail, Toast.LENGTH_SHORT).show();
            return true;
        }
        // A different folder starts from nothing: what was remembered of copies to the old one
        // says nothing about this one
        sp(a).edit().clear().putString(KEY_TREE, uri.toString()).apply();
        LOG.d("LibreraSafSync picked", uri);

        final Context app = a.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        AppProfile.save(a);
        AppsConfig.executorServiceSingle.execute(() -> {
            pull(app);
            main.post(() -> {
                useSharedProfile(app);
                push(app);
                SearchAllBooksWorker.run(app);
                if (!a.isFinishing()) {
                    a.finish();
                    MainTabs2.startActivity(a, TempHolder.get().currentTab);
                }
            });
        });
        return true;
    }

    /**
     * Moves the app off its local profile onto the one the Librera folder is read with when the
     * permission is there, so both ways of running write the same files. A profile the folder does
     * not have yet starts from the local one, so nothing set so far is lost.
     */
    private static void useSharedProfile(Context c) {
        String shared = sharedProfile();
        if (shared.equals(AppProfile.getCurrent())) {
            return;
        }
        File root = new File(AppSP.get().getRootPath(c));
        File target = new File(root, AppProfile.PROFILE_PREFIX + shared);
        File local = new File(root, AppProfile.PROFILE_PREFIX + AppProfile.getCurrent());
        if (!target.isDirectory() && local.isDirectory()) {
            copyFolder(local, target);
        }
        // The folders just handed over are the only ones that can be read. The shared profile may
        // have been written with the permission, its folders paths this app cannot read now, so
        // the picked ones are carried over rather than lost under them.
        final List<String> library = new ArrayList<>();
        for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (ExtUtils.isExteralSD(path)) {
                library.add(path);
            }
        }
        final List<String> storages = StringDB.asList(BookCSS.get().pathSAF);
        final String lastDir = BookCSS.get().dirLastPath;

        // Chosen again from scratch, as on a permission change: getRootPath picks the shared
        // profile now that the folder is there
        AppSP.get().currentProfile = "";
        AppSP.get().save();
        AppProfile.clear();
        AppProfile.init(c);

        // Without the permission only folders handed over through the picker can be read. Those
        // of them this install holds no grant for - after a reinstall, all of them - stay, so
        // the library can ask for each again (MainTabs2.askForLostLibraryStorage).
        final List<String> readable = new ArrayList<>();
        for (String path : JsonDB.get(BookCSS.get().searchPathsJson)) {
            if (ExtUtils.isExteralSD(path) && !readable.contains(path)) {
                readable.add(path);
            }
        }
        for (String path : library) {
            if (!readable.contains(path)) {
                readable.add(path);
            }
        }
        BookCSS.get().searchPathsJson = JsonDB.set(readable);
        for (String saf : storages) {
            if (!StringDB.asList(BookCSS.get().pathSAF).contains(saf)) {
                StringDB.add(BookCSS.get().pathSAF, saf, (db) -> BookCSS.get().pathSAF = db);
            }
        }
        if (ExtUtils.isExteralSD(lastDir)) {
            BookCSS.get().dirLastPath = lastDir;
        }
        AppProfile.save(c);
        LOG.d("LibreraSafSync profile", AppProfile.getCurrent(), BookCSS.get().searchPathsJson);
    }

    /** The profile the Librera folder is read with when the app has the permission. */
    public static String sharedProfile() {
        return AppsConfig.IS_LOG ? "BETA" : "Librera";
    }

    /** The files a device's profile folder holds. */
    private static final String[] PROFILE_FILES = {AppProfile.APP_STATE_JSON, AppProfile.APP_CSS_JSON,
            AppProfile.APP_RECENT_JSON, AppProfile.APP_EXCLUDE_JSON, AppProfile.APP_FAVORITE_JSON,
            AppProfile.APP_BOOKMARKS_JSON, AppProfile.APP_ANNOTATIONS_JSON, AppProfile.APP_PROGRESS_JSON,
            AppProfile.APP_TAGS_JSON, AppProfile.APP_TAGS_JSON2};

    /** Download/Librera: where the settings are kept without the permission, in plain sight. */
    public static File downloadLibrera() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LIBRERA);
    }

    /**
     * Whether the settings can be kept in Download/Librera by this install itself, as plain files.
     *
     * Without the permission an app may make folders and files in Download and read back what it
     * made - and only that. Files an earlier install made, before the app was reinstalled, are
     * still there but cannot be read, written, replaced or deleted (EACCES): the settings would
     * silently never save. Those are told by being there yet unreadable. The folder has then to
     * be handed over through the picker, and is kept in step with as any other Librera folder.
     */
    public static boolean isDownloadLibreraUsable(Context c) {
        try {
            File device = new File(downloadLibrera(),
                    AppProfile.PROFILE_PREFIX + sharedProfile() + "/" + AppProfile.DEVICE_MODEL);
            device.mkdirs();
            if (!device.isDirectory()) {
                return false;
            }
            for (String name : PROFILE_FILES) {
                File file = new File(device, name);
                if (file.exists() && !file.canRead()) {
                    LOG.d("LibreraSafSync Download/Librera is an earlier install's", file);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOG.e(e);
            return false;
        }
    }

    /**
     * Where the settings are kept, as the user knows it: the Librera folder handed over when the
     * app's own copy is kept in step with one, otherwise the folder they are written to.
     */
    public static String settingsFolder(Context c) {
        if (isActive(c)) {
            Uri tree = tree(c);
            String path = ExtUtils.getSafTreePath(tree.toString());
            if (path != null) {
                return path;
            }
            try {
                return Uri.decode(DocumentsContract.getTreeDocumentId(tree));
            } catch (Exception e) {
                LOG.e(e);
            }
        }
        return AppSP.get().getRootPath(c);
    }

    /** Whether the settings are in the app's own storage, where nothing else sees them. */
    public static boolean isProfileHidden(Context c) {
        return AppSP.get().getRootPath(c).equals(AppSP.get().getTempDir(c));
    }

    /**
     * With the permission the settings are in /storage/emulated/0/Librera. What was kept in
     * Download/Librera without it, and is newer, is brought over, so granting the permission
     * later loses nothing. The files there are left as they are.
     */
    static void mergeDownloadLibrera(Context c) {
        if (c == null || !Android6.canWrite(c)) {
            return;
        }
        File from = downloadLibrera();
        File to = new File(AppSP.get().getRootPath(c));
        if (!from.isDirectory() || from.getAbsolutePath().equals(to.getAbsolutePath())) {
            return;
        }
        File[] profiles = from.listFiles();
        if (profiles == null) {
            return;
        }
        for (File profile : profiles) {
            if (profile.isDirectory() && isSynced(profile.getName())) {
                mergeNewer(profile, new File(to, profile.getName()));
            }
        }
    }

    private static void mergeNewer(File from, File to) {
        File[] children = from.listFiles();
        if (children == null) {
            return;
        }
        to.mkdirs();
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) {
                mergeNewer(child, target);
            } else if (!target.isFile() || child.lastModified() > target.lastModified()) {
                try (InputStream in = new java.io.FileInputStream(child);
                     OutputStream out = new FileOutputStream(target)) {
                    copy(in, out);
                    target.setLastModified(child.lastModified());
                    LOG.d("LibreraSafSync merged", target);
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        }
    }

    /** Brings in what the Librera folder has newer. Blocks; it is run as the profile is loaded. */
    static void pull(Context c) {
        if (!isActive(c)) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            sync(c, true);
        } catch (Exception e) {
            LOG.e(e);
        }
        LOG.d("LibreraSafSync pull", System.currentTimeMillis() - start, "ms");
    }

    /**
     * Writes out what changed here, once the profile writes already queued are done: they run on
     * the same single thread, so this comes after them. Saves come in bursts; one push covers a
     * burst.
     */
    static void pushLater(Context c) {
        if (c == null || !isActive(c) || !pushQueued.compareAndSet(false, true)) {
            return;
        }
        final Context app = c.getApplicationContext();
        AppsConfig.executorServiceSingle.execute(() -> {
            pushQueued.set(false);
            push(app);
        });
    }

    private static void push(Context c) {
        if (!isActive(c)) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            sync(c, false);
        } catch (Exception e) {
            LOG.e(e);
        }
        LOG.d("LibreraSafSync push", System.currentTimeMillis() - start, "ms");
    }

    private static class Remote {
        String id;
        long modified;
    }

    /**
     * One pass over both sides.
     *
     * @param pull true to only bring files in, false to only write them out - a file that should
     *             go the other way waits for the pass that does
     */
    private static synchronized void sync(Context c, boolean pull) throws Exception {
        final ContentResolver resolver = c.getContentResolver();
        final Uri tree = tree(c);
        final File root = new File(AppSP.get().getRootPath(c));

        // The folder the settings are written to directly: copying it onto itself through a
        // second door would truncate each file as it is read
        final String treeFolder = ExtUtils.getSafTreePath(tree.toString());
        if (treeFolder != null && new File(treeFolder).getAbsolutePath().equals(root.getAbsolutePath())) {
            LOG.d("LibreraSafSync same folder, nothing to keep in step", treeFolder);
            return;
        }

        // The Librera folder: every file under a profile folder, and every folder by its path
        final Map<String, Remote> remoteFiles = new HashMap<>();
        final Map<String, String> remoteDirs = new HashMap<>();
        final String rootId = DocumentsContract.getTreeDocumentId(tree);
        remoteDirs.put("", rootId);
        listRemote(resolver, tree, rootId, "", remoteFiles, remoteDirs);

        // The app's own copy
        final Map<String, File> localFiles = new HashMap<>();
        File[] profiles = root.listFiles();
        if (profiles != null) {
            for (File profile : profiles) {
                if (isSynced(profile.getName()) && profile.isDirectory()) {
                    listLocal(profile, profile.getName(), localFiles);
                }
            }
        }

        final SharedPreferences sp = sp(c);
        final SharedPreferences.Editor state = sp.edit();
        final Set<String> all = new HashSet<>(remoteFiles.keySet());
        all.addAll(localFiles.keySet());

        for (String path : all) {
            Remote remote = remoteFiles.get(path);
            File local = localFiles.get(path);
            String[] last = TxtUtils.isEmpty(sp.getString(STATE_PREFIX + path, ""))
                            ? null
                            : sp.getString(STATE_PREFIX + path, "").split("\\|");

            boolean toLocal;
            if (remote == null) {
                toLocal = false;
            } else if (local == null) {
                toLocal = true;
            } else {
                boolean localChanged = last == null || !String.valueOf(local.lastModified()).equals(last[0]);
                boolean remoteChanged = last == null || !String.valueOf(remote.modified).equals(last[1]);
                if (!localChanged && !remoteChanged) {
                    continue;
                }
                if (localChanged && remoteChanged) {
                    toLocal = remote.modified > local.lastModified();
                } else {
                    toLocal = remoteChanged;
                }
            }
            if (toLocal != pull) {
                continue;
            }

            if (toLocal) {
                File file = local != null ? local : new File(root, path);
                if (download(resolver, tree, remote.id, file, remote.modified)) {
                    state.putString(STATE_PREFIX + path, file.lastModified() + "|" + remote.modified);
                    LOG.d("LibreraSafSync in", path);
                }
            } else {
                Remote written = upload(resolver, tree, remoteDirs, path, remote, local);
                if (written != null) {
                    state.putString(STATE_PREFIX + path, local.lastModified() + "|" + written.modified);
                    LOG.d("LibreraSafSync out", path);
                }
            }
        }
        state.apply();
    }

    private static boolean isSynced(String topName) {
        return topName.startsWith(AppProfile.PROFILE_PREFIX) && !topName.equals(LOCAL_ONLY_PROFILE);
    }

    private static void listRemote(ContentResolver resolver, Uri tree, String dirId, String dirPath,
            Map<String, Remote> files, Map<String, String> dirs) {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, dirId);
        String[] columns = {Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED};
        try (Cursor cursor = resolver.query(children, columns, null, null, null)) {
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);
                String path = dirPath.isEmpty() ? name : dirPath + "/" + name;
                if (Document.MIME_TYPE_DIR.equals(mime)) {
                    if (dirPath.isEmpty() && !isSynced(name)) {
                        continue;
                    }
                    dirs.put(path, id);
                    listRemote(resolver, tree, id, path, files, dirs);
                } else if (!dirPath.isEmpty()) {
                    Remote remote = new Remote();
                    remote.id = id;
                    remote.modified = cursor.getLong(3);
                    files.put(path, remote);
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private static void listLocal(File dir, String dirPath, Map<String, File> files) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String path = dirPath + "/" + child.getName();
            if (child.isDirectory()) {
                listLocal(child, path, files);
            } else {
                files.put(path, child);
            }
        }
    }

    private static boolean download(ContentResolver resolver, Uri tree, String id, File file, long modified) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temp = new File(parent, file.getName() + ".saf-tmp");
        try (InputStream in = resolver.openInputStream(DocumentsContract.buildDocumentUriUsingTree(tree, id));
             OutputStream out = new FileOutputStream(temp)) {
            if (in == null) {
                return false;
            }
            copy(in, out);
        } catch (Exception e) {
            LOG.e(e);
            temp.delete();
            return false;
        }
        if (!temp.renameTo(file)) {
            temp.delete();
            return false;
        }
        file.setLastModified(modified);
        return true;
    }

    /** @return the written file as the Librera folder now has it, or null if it could not be */
    private static Remote upload(ContentResolver resolver, Uri tree, Map<String, String> dirs, String path,
            Remote remote, File local) {
        try {
            String id = remote != null ? remote.id : null;
            if (id == null) {
                int slash = path.lastIndexOf('/');
                String parentId = ensureDir(resolver, tree, dirs, path.substring(0, slash));
                if (parentId == null) {
                    return null;
                }
                Uri created = DocumentsContract.createDocument(resolver,
                        DocumentsContract.buildDocumentUriUsingTree(tree, parentId), MIME_FILE,
                        path.substring(slash + 1));
                if (created == null) {
                    return null;
                }
                id = DocumentsContract.getDocumentId(created);
            }
            Uri doc = DocumentsContract.buildDocumentUriUsingTree(tree, id);
            try (InputStream in = new java.io.FileInputStream(local);
                 OutputStream out = resolver.openOutputStream(doc, "wt")) {
                if (out == null) {
                    return null;
                }
                copy(in, out);
            }
            Remote written = new Remote();
            written.id = id;
            written.modified = modifiedOf(resolver, doc);
            return written;
        } catch (Exception e) {
            LOG.e(e, path);
            return null;
        }
    }

    private static String ensureDir(ContentResolver resolver, Uri tree, Map<String, String> dirs, String path)
            throws Exception {
        String id = dirs.get(path);
        if (id != null) {
            return id;
        }
        int slash = path.lastIndexOf('/');
        String parentId = ensureDir(resolver, tree, dirs, slash < 0 ? "" : path.substring(0, slash));
        if (parentId == null) {
            return null;
        }
        Uri created = DocumentsContract.createDocument(resolver,
                DocumentsContract.buildDocumentUriUsingTree(tree, parentId), Document.MIME_TYPE_DIR,
                path.substring(slash + 1));
        if (created == null) {
            return null;
        }
        id = DocumentsContract.getDocumentId(created);
        dirs.put(path, id);
        return id;
    }

    private static long modifiedOf(ContentResolver resolver, Uri doc) {
        try (Cursor cursor = resolver.query(doc, new String[]{Document.COLUMN_LAST_MODIFIED}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return 0;
    }

    private static void copyFolder(File from, File to) {
        to.mkdirs();
        File[] children = from.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) {
                copyFolder(child, target);
            } else {
                try (InputStream in = new java.io.FileInputStream(child);
                     OutputStream out = new FileOutputStream(target)) {
                    copy(in, out);
                } catch (Exception e) {
                    LOG.e(e);
                }
            }
        }
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
