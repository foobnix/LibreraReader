package com.foobnix.librerax;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.foobnix.android.utils.Apps;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.search.activity.HorizontalViewActivity;

import org.ebookdroid.ui.viewer.VerticalViewActivity;

/**
 * LibreraX as a fourth reading mode: the book is handed over with the place it is read at
 * (the percent and the text at the top of the page), and the place LibreraX closes it at
 * comes back as the result, see LibreraXActivity. Notes and bookmarks stay where they are.
 *
 * The same extras are read by com.librerax.reader.ReaderActivity on the other side.
 */
public class LibreraX {

    public static final String PACKAGE = "com.librerax";
    // Started for a result: the reader LibreraX keeps for itself is singleTask and would
    // answer at once with RESULT_CANCELED.
    public static final String READER_FOR_RESULT = "com.librerax.reader.ReaderForResultActivity";
    public static final String DOWNLOAD_URL = "https://github.com/foobnix/LibreraReader/releases";

    public static final String EXTRA_PERCENT = "librera.percent";
    public static final String EXTRA_PAGE_TEXT = "librera.pageText";

    // the activity a book is opened with in the current reading mode, for widgets and shortcuts
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

    // percent > 0 opens the book at that place (a bookmark), else where it was left
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

    public static void showNotInstalled(final Activity a, final Runnable onDismiss) {
        final AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(R.string.librerax)
                .setMessage(a.getString(R.string.librerax_not_installed) + "\n\n" + DOWNLOAD_URL)
                .setPositiveButton(R.string.download, (d, which) -> Urls.open(a, DOWNLOAD_URL))
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
