package com.foobnix.librerax;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.foobnix.android.utils.Apps;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

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
    // a JSON array of {text, p, pt, t}
    public static final String EXTRA_BOOKMARKS = "librera.bookmarks";
    // "scroll" for the vertical view, "paged" for the book view
    public static final String EXTRA_VIEW = "librera.view";
    public static final String VIEW_SCROLL = "scroll";
    public static final String VIEW_PAGED = "paged";

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
