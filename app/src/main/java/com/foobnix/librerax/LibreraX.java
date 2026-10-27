package com.foobnix.librerax;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.IO;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppProfile;
import com.foobnix.model.MyPath;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.ui2.AppDB;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.view.MyProgressDialog;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The extras below are a contract with the LibreraX app: com.librerax.reader.ReaderActivity
 * reads them and returns the place it closed the book at, see LibreraXActivity.
 */
public class LibreraX {

    public static final String PACKAGE = "com.librerax";
    // The reader LibreraX keeps for itself is singleTask and answers at once with RESULT_CANCELED.
    public static final String READER_FOR_RESULT = "com.librerax.reader.ReaderForResultActivity";
    public static final String DOWNLOAD_URL_PLAY = "https://play.google.com/store/apps/details?id=" + PACKAGE;
    public static final String DOWNLOAD_URL_APP_STORE = "https://apps.apple.com/us/app/librerax/id6815773197";
    public static final String DOWNLOAD_URL_GITHUB = "https://github.com/foobnix/LibreraReader/releases";

    public static final String EXTRA_PERCENT = "librera.percent";
    public static final String EXTRA_PAGE_TEXT = "librera.pageText";
    // the foliate engine's CFI of the place, of Librera's own copy of the book: right to the paragraph
    public static final String EXTRA_CFI = "librera.cfi";
    // a JSON array of {text, p, pt, t}
    public static final String EXTRA_BOOKMARKS = "librera.bookmarks";
    // a JSON array of the book's highlights as Librera keeps them: {t, type, color, text, page, p, rects, cfi}
    public static final String EXTRA_ANNOTATIONS = "librera.annotations";
    // "scroll" for the vertical view, "paged" for the book view
    public static final String EXTRA_VIEW = "librera.view";
    public static final String VIEW_SCROLL = "scroll";
    public static final String VIEW_PAGED = "paged";
    // true when the reader is in full screen here, with or without the cutout
    public static final String EXTRA_FULLSCREEN = "librera.fullscreen";

    // Export to LibreraX: the current profile's files below, zipped as profile.X/device.Y/app-*.json
    public static final String IMPORT_ACTIVITY = "com.librerax.LibreraImportActivity";
    public static final String EXTRA_PROFILE = "librera.profile";
    private static final int REQUEST_EXPORT = 7301;
    // The choices in the export dialog, in its order: book progress, favorites, bookmarks, tags.
    private static final String[][] EXPORT_PARTS = {
            {AppProfile.APP_PROGRESS_JSON, AppProfile.APP_RECENT_JSON},
            {AppProfile.APP_FAVORITE_JSON},
            {AppProfile.APP_BOOKMARKS_JSON, AppProfile.APP_ANNOTATIONS_JSON},
            {AppProfile.APP_TAGS_JSON, AppProfile.APP_TAGS_JSON2}};
    // Always sent: the library folders, so LibreraX can find the books the rest is about.
    private static final String EXPORT_ALWAYS = AppProfile.APP_CSS_JSON;
    // In the zip beside the profile: [{path, uri}], a content uri of this app's for every book the
    // export names, granted to LibreraX with the intent so it can read books it has no access to
    public static final String EXPORT_BOOKS = "librera-books.json";

    private static boolean isView(int mode) {
        return mode == AppState.READING_MODE_SCROLL || mode == AppState.READING_MODE_BOOK
                || mode == AppState.READING_MODE_MUSICIAN;
    }

    // Called before readingMode becomes READING_MODE_LIBRERAX, which would lose the view in use.
    public static void rememberView() {
        final int mode = AppSP.get().readingMode;
        if (isView(mode)) {
            AppSP.get().libreraXView = mode;
        }
    }

    public static String view() {
        int mode = AppSP.get().readingMode;
        if (!isView(mode)) {
            mode = AppSP.get().libreraXView;
        }
        return mode == AppState.READING_MODE_BOOK ? VIEW_PAGED : VIEW_SCROLL;
    }

    public static Class<?> readerClass() {
        switch (AppSP.get().readingMode) {
            case AppState.READING_MODE_LIBRERAX:
                return LibreraXActivity.class;
            case AppState.READING_MODE_BOOK:
                return HorizontalViewActivity.class;
            default:
                return VerticalViewActivity.class;
        }
    }

    public static boolean isInstalled(Context c) {
        return Apps.isPackageInstalled(PACKAGE, c);
    }

    // percent > 0 opens the book at that place, else where it was left
    public static void open(Context c, Uri uri, float percent, String pageText) {
        final Intent intent = new Intent(c, LibreraXActivity.class);
        intent.setData(uri);
        if (percent > 0f) {
            intent.putExtra(EXTRA_PERCENT, percent);
            intent.putExtra(EXTRA_PAGE_TEXT, pageText);
        }
        if (!(c instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        c.startActivity(intent);
    }

    public static boolean fullscreen() {
        return AppState.get().fullScreenMode != AppState.FULL_SCREEN_NORMAL;
    }

    public static String downloadUrl() {
        return AppsConfig.IS_FDROID ? DOWNLOAD_URL_GITHUB : DOWNLOAD_URL_PLAY;
    }

    /**
     * Offers LibreraX for a book this app could not open: opening it threw, it came up with no
     * pages, or the app died opening it last time. With LibreraX installed the same book opens
     * there; without it, the page to get it from - Google Play, or GitHub for the F-Droid build.
     *
     * @param crashed    the app died opening it last time, rather than failing just now
     * @param onTryAgain when not null, a button to open it here once more; the dialog then cannot
     *                   be dismissed without a choice, as the book is not open behind it
     */
    public static void showCannotOpen(final Activity a, final Uri uri, final boolean crashed,
                                      final Runnable onTryAgain) {
        if (a == null || a.isFinishing()) {
            return;
        }
        final boolean installed = isInstalled(a);
        final String url = downloadUrl();
        String message = a.getString(crashed ? R.string.book_open_crashed : R.string.book_open_failed) + "\n\n";
        message += installed ? a.getString(R.string.try_librerax) : a.getString(R.string.get_librerax) + "\n" + url;

        final AlertDialog.Builder builder = new AlertDialog.Builder(a)
                .setTitle(R.string.librerax)
                .setMessage(message);
        if (installed && uri != null) {
            builder.setPositiveButton(R.string.open_in_librerax, (d, which) -> {
                open(a, uri, 0f, null);
                a.finish();
            });
        } else {
            builder.setPositiveButton(R.string.download, (d, which) -> {
                Urls.open(a, url);
                if (onTryAgain != null) {
                    a.finish();
                }
            });
        }
        if (onTryAgain != null) {
            builder.setNegativeButton(R.string.try_again, (d, which) -> onTryAgain.run());
            builder.setNeutralButton(R.string.close, (d, which) -> a.finish());
            builder.setCancelable(false);
        } else {
            builder.setNegativeButton(R.string.close, null);
        }
        builder.show();
    }

    /**
     * Asks which of the current profile's book progress, favorites, bookmarks and tags to hand to
     * LibreraX, then sends them, from every device in the profile, as one zip that LibreraX
     * imports at once. Started for a result: LibreraX takes it only from a caller it can name.
     */
    public static void exportTo(final Activity a) {
        if (!isInstalled(a)) {
            showNotInstalled(a, null);
            return;
        }
        final boolean[] checked = {true, true, true, true};
        final CharSequence[] labels = {a.getString(R.string.reading_progress), a.getString(R.string.favorites),
                a.getString(R.string.bookmarks), a.getString(R.string.tags)};
        new AlertDialog.Builder(a)
                .setTitle(R.string.export_to_librerax)
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.export_, (d, which) -> {
                    final List<String> names = new ArrayList<>();
                    for (int i = 0; i < EXPORT_PARTS.length; i++) {
                        if (checked[i]) {
                            names.addAll(Arrays.asList(EXPORT_PARTS[i]));
                        }
                    }
                    if (!names.isEmpty()) {
                        names.add(EXPORT_ALWAYS);
                        export(a, names);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void export(final Activity a, final List<String> names) {
        final String profile = AppProfile.getCurrent();
        final MyProgressDialog wait = MyProgressDialog.show(a, a.getString(R.string.please_wait));
        wait.setCancelable(false);
        new Thread(() -> {
            File zip = null;
            final List<Uri> books = new ArrayList<>();
            try {
                SharedBooks.flush();
                zip = zipProfile(a, profile, names, books);
            } catch (Exception e) {
                LOG.e(e);
            }
            final File done = zip;
            a.runOnUiThread(() -> {
                if (a.isFinishing()) {
                    return;
                }
                if (wait != null && wait.isShowing()) {
                    wait.dismiss();
                }
                if (done == null) {
                    Toast.makeText(a, R.string.msg_unexpected_error, Toast.LENGTH_LONG).show();
                    return;
                }
                final Uri uri = FileProvider.getUriForFile(a, Apps.getPackageName(a) + ".provider", done);
                final Intent intent = new Intent()
                        .setClassName(PACKAGE, IMPORT_ACTIVITY)
                        .setDataAndType(uri, "application/zip")
                        .putExtra(EXTRA_PROFILE, profile)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                final ClipData clip = ClipData.newRawUri(null, uri);
                for (Uri book : books) {
                    clip.addItem(new ClipData.Item(book));
                }
                intent.setClipData(clip);
                try {
                    a.startActivityForResult(intent, REQUEST_EXPORT);
                } catch (ActivityNotFoundException e) {
                    showUpdate(a);
                }
            });
        }, "LibreraX-export").start();
    }

    private static File zipProfile(Context c, String profile, List<String> names, List<Uri> granted)
            throws Exception {
        final File folder = AppProfile.SYNC_FOLDER_PROFILE;
        final File[] devices = folder == null ? null : folder.listFiles();
        if (devices == null) {
            return null;
        }
        final File out = new File(c.getCacheDir(), "librerax");
        out.mkdirs();
        final File[] old = out.listFiles();
        if (old != null) {
            for (File f : old) {
                f.delete();
            }
        }
        final File zip = new File(out, "Librera-" + profile.replaceAll("[^\\p{L}\\p{N}_-]", "_") + ".zip");
        final byte[] buffer = new byte[64 * 1024];
        final Set<String> paths = new LinkedHashSet<>();
        final Set<String> fileNames = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            for (File device : devices) {
                if (!device.isDirectory() || !device.getName().startsWith(AppProfile.DEVICE_PREFIX)) {
                    continue;
                }
                for (String name : names) {
                    final File file = new File(device, name);
                    if (!file.isFile()) {
                        continue;
                    }
                    final ZipEntry entry = new ZipEntry(folder.getName() + "/" + device.getName() + "/" + name);
                    entry.setTime(file.lastModified());
                    zos.putNextEntry(entry);
                    try (InputStream in = new FileInputStream(file)) {
                        int read;
                        while ((read = in.read(buffer)) > 0) {
                            zos.write(buffer, 0, read);
                        }
                    }
                    zos.closeEntry();
                    if (!EXPORT_ALWAYS.equals(name)) {
                        booksIn(file, AppProfile.APP_PROGRESS_JSON.equals(name), paths, fileNames);
                    }
                }
            }
            if (!fileNames.isEmpty()) {
                for (FileMeta meta : AppDB.get().getAll()) {
                    if (meta.getPath() != null && fileNames.contains(ExtUtils.getFileName(meta.getPath()))) {
                        paths.add(meta.getPath());
                    }
                }
            }
            final JSONArray books = new JSONArray();
            final String authority = Apps.getPackageName(c) + ".provider";
            for (String path : paths) {
                final File book = new File(path);
                if (!book.isFile()) {
                    continue;
                }
                try {
                    final Uri uri = FileProvider.getUriForFile(c, authority, book);
                    books.put(new JSONObject().put("path", book.getPath()).put("uri", uri.toString()));
                    granted.add(uri);
                } catch (IllegalArgumentException e) {
                    LOG.e(e, path);
                }
            }
            zos.putNextEntry(new ZipEntry(EXPORT_BOOKS));
            zos.write(books.toString().getBytes("UTF-8"));
            zos.closeEntry();
        }
        return zip;
    }

    /** The books a profile file names: by path anywhere in it, or, for the progress file, by file name. */
    private static void booksIn(File file, boolean byName, Set<String> paths, Set<String> fileNames) {
        try {
            final Object json = new JSONTokener(IO.readString(file)).nextValue();
            if (byName && json instanceof JSONObject) {
                final Iterator<String> keys = ((JSONObject) json).keys();
                while (keys.hasNext()) {
                    fileNames.add(keys.next());
                }
                return;
            }
            collectPaths(json, paths);
        } catch (Exception e) {
            LOG.e(e, file);
        }
    }

    private static void collectPaths(Object json, Set<String> paths) throws Exception {
        if (json instanceof JSONObject) {
            final JSONObject object = (JSONObject) json;
            final Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                addPath(key, paths);
                collectPaths(object.get(key), paths);
            }
        } else if (json instanceof JSONArray) {
            final JSONArray array = (JSONArray) json;
            for (int i = 0; i < array.length(); i++) {
                collectPaths(array.get(i), paths);
            }
        } else if (json instanceof String) {
            addPath((String) json, paths);
        }
    }

    private static void addPath(String value, Set<String> paths) {
        if (value.startsWith(MyPath.INTERNAL_PREFIX) || value.startsWith("/")) {
            paths.add(MyPath.toAbsolute(value));
        }
    }

    private static void showUpdate(final Activity a) {
        final String url = downloadUrl();
        new AlertDialog.Builder(a)
                .setTitle(R.string.librerax)
                .setMessage(a.getString(R.string.update_librerax) + "\n\n" + url)
                .setPositiveButton(R.string.download, (d, which) -> Urls.open(a, url))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void showNotInstalled(final Activity a, final Runnable onDismiss) {
        final String url = downloadUrl();
        final AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(R.string.librerax)
                .setMessage(a.getString(R.string.librerax_not_installed) + "\n\n" + url)
                .setPositiveButton(R.string.download, (d, which) -> Urls.open(a, url))
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnDismissListener(d -> {
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        dialog.show();
    }
}
