package com.foobnix.librerax;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppBookmark;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.BookmarksData;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.search.activity.msg.NotifyAllFragments;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.MyContextWrapper;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * Opens a book in LibreraX and waits for it to be closed there: the place it was closed at
 * is saved as the book's progress, the same as a book closed in Librera's own reader.
 * Nothing is shown unless LibreraX is missing, then the popup to download it.
 */
public class LibreraXActivity extends Activity {

    private static final int REQUEST_READ = 1;
    private static final String STATE_STARTED = "started";

    private String path;
    private boolean started;

    @Override protected void attachBaseContext(Context context) {
        // the language chosen in the app, not the system one
        super.attachBaseContext(MyContextWrapper.wrap(context));
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        if (AppState.get().isDayNotInvert) {
            setTheme(R.style.LibreraXWhite);
        } else {
            setTheme(R.style.LibreraXBlack);
        }
        super.onCreate(savedInstanceState);

        final Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }
        if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        }
        // Recreated while LibreraX is open: the result is still to come.
        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_STARTED)) {
            started = true;
            return;
        }
        if (!LibreraX.isInstalled(this)) {
            LibreraX.showNotInstalled(this, this::finish);
            return;
        }

        float percent = getIntent().getFloatExtra(LibreraX.EXTRA_PERCENT, 0f);
        String pageText = getIntent().getStringExtra(LibreraX.EXTRA_PAGE_TEXT);
        if (percent <= 0f && path != null) {
            final AppBook bs = SharedBooks.load(path);
            percent = bs.p;
            pageText = bs.pt;
        }

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClassName(LibreraX.PACKAGE, LibreraX.READER_FOR_RESULT);
        if (path != null) {
            final File file = new File(path);
            intent.setDataAndType(ExtUtils.getUriProvider(this, file), ExtUtils.getMimeType(file));
        } else {
            intent.setData(uri);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(LibreraX.EXTRA_PERCENT, percent);
        intent.putExtra(LibreraX.EXTRA_PAGE_TEXT, pageText);
        if (path != null) {
            intent.putExtra(LibreraX.EXTRA_BOOKMARKS, bookmarksJson(path));
        }
        LOG.d("LibreraX-open", path, percent, pageText);

        try {
            startActivityForResult(intent, REQUEST_READ);
            started = true;
        } catch (ActivityNotFoundException e) {
            // A LibreraX from before it could be asked for a result
            LOG.e(e);
            LibreraX.showNotInstalled(this, this::finish);
            return;
        } catch (Exception e) {
            LOG.e(e);
            finish();
            return;
        }
        if (path != null) {
            AppDB.get().addRecent(path);
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_STARTED, started);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_READ) {
            return;
        }
        if (data != null && path != null && data.hasExtra(LibreraX.EXTRA_PERCENT)) {
            saveProgress(path, data.getFloatExtra(LibreraX.EXTRA_PERCENT, 0f),
                         data.getStringExtra(LibreraX.EXTRA_PAGE_TEXT));
        }
        finish();
    }

    private static final int BOOKMARKS_MAX = 1000;
    private static final int BOOKMARK_TEXT_MAX = 500;

    // this book's bookmarks, kept small enough for an intent
    private String bookmarksJson(String path) {
        final JSONArray array = new JSONArray();
        try {
            final String quick = getString(R.string.fast_bookmark);
            for (AppBookmark bookmark : BookmarksData.get().getBookmarksByBook(path)) {
                if (array.length() >= BOOKMARKS_MAX) {
                    break;
                }
                // a quick bookmark's text is a label, not words of the page
                String find = bookmark.pt;
                if (TxtUtils.isEmpty(find) && !quick.equals(bookmark.text)) {
                    find = bookmark.text;
                }
                array.put(new JSONObject()
                        .put("text", cut(bookmark.text))
                        .put("p", (double) bookmark.p)
                        .put("pt", cut(find))
                        .put("t", bookmark.t));
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return array.toString();
    }

    private static String cut(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > BOOKMARK_TEXT_MAX ? text.substring(0, BOOKMARK_TEXT_MAX) : text;
    }

    private static void saveProgress(String path, float percent, String pageText) {
        try {
            // 0 is the first page: a book closed there is progress too
            if (percent < 0f || percent > 1f) {
                return;
            }
            final AppBook bs = SharedBooks.load(path);
            bs.p = percent;
            bs.pt = TxtUtils.isEmpty(pageText) ? null : pageText;
            bs.t = System.currentTimeMillis();
            SharedBooks.save(bs);
            SharedBooks.syncToDb(path);
            AppDB.get().addRecent(path);
            LOG.d("LibreraX-progress", path, percent, pageText);
            EventBus.getDefault().post(new NotifyAllFragments());
        } catch (Exception e) {
            LOG.e(e);
        }
    }
}
