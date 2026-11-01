package com.foobnix.librerax;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Remembers which book the app was opening when it died, so the next open of it can offer
 * LibreraX instead of crashing again.
 *
 * A native reader that crashes takes the process with it, and nothing in it runs to say so - so
 * the book is marked before it is opened, with a synchronous write the crash cannot lose, and
 * unmarked once it is up and its first pages have been drawn. A mark still there when the book
 * is opened again is a crash. ImageExtractor guards book covers the same way.
 *
 * Only a mark left by an earlier run of the app is a crash. Switching between the paged and the
 * scrolled viewer opens the same book in the new one before the old one is destroyed, so the old
 * one's mark can still be there: it is this run's, and the old viewer is merely closing. Each
 * open's mark names that open, so the old viewer unmarking the book on its way out leaves the new
 * viewer's mark alone.
 */
public final class BookOpenGuard {

    private static final String PREFS = "BookOpenGuard";

    /** How long after a book is up a crash still counts against opening it: its first pages draw then. */
    private static final long SETTLED_MS = 5000;

    /** This run of the app: a new process, and so a new run, follows every crash. */
    private static final String RUN = UUID.randomUUID().toString();
    private static final AtomicInteger OPENS = new AtomicInteger();

    private BookOpenGuard() {
    }

    private static SharedPreferences prefs(final Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Whether the app died the last time it was opening this book. */
    public static boolean crashedLastTime(final Context c, final String path) {
        if (TxtUtils.isEmpty(path)) {
            return false;
        }
        // A Boolean is a mark from before marks named their open: a run long gone
        final Object mark = prefs(c).getAll().get(path);
        return mark != null && !(mark instanceof String && ((String) mark).startsWith(RUN + "/"))
                && !lastRunEndedNormally(c);
    }

    /**
     * A mark says only that the app stopped while the book was opening, and an update or the user
     * closing it stops it too - easily done, as the first open of a long book can take a while.
     * Android knows which it was from version 11, so a book is not blamed for those.
     */
    private static boolean lastRunEndedNormally(final Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        try {
            final ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            // Newest first, and the WebView's own processes are in the list too
            for (ApplicationExitInfo exit : manager.getHistoricalProcessExitReasons(null, 0, 16)) {
                if (c.getPackageName().equals(exit.getProcessName())) {
                    final int reason = exit.getReason();
                    return reason != ApplicationExitInfo.REASON_CRASH
                            && reason != ApplicationExitInfo.REASON_CRASH_NATIVE
                            && reason != ApplicationExitInfo.REASON_ANR
                            && reason != ApplicationExitInfo.REASON_LOW_MEMORY
                            && reason != ApplicationExitInfo.REASON_SIGNALED
                            && reason != ApplicationExitInfo.REASON_INITIALIZATION_FAILURE;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    /**
     * Marks the book as being opened; written before returning, so a crash right after keeps it.
     *
     * @return this open's mark, to unmark the book with
     */
    public static String opening(final Context c, final String path) {
        if (TxtUtils.isEmpty(path)) {
            return null;
        }
        final String mark = RUN + "/" + OPENS.incrementAndGet();
        prefs(c).edit().putString(path, mark).commit();
        return mark;
    }

    /** The open that made {@code mark} ended, or failed in a way the app handled itself: no crash to remember. */
    public static void opened(final Context c, final String path, final String mark) {
        if (TxtUtils.isEmpty(path) || mark == null) {
            return;
        }
        final SharedPreferences prefs = prefs(c);
        if (mark.equals(prefs.getAll().get(path))) {
            prefs.edit().remove(path).apply();
        }
    }

    /** Unmarks the book once its first pages have had time to draw. */
    public static void openedSoon(final View view, final String path, final String mark) {
        final Context c = view.getContext().getApplicationContext();
        view.postDelayed(() -> opened(c, path, mark), SETTLED_MS);
    }

    /** Forgets the crash the book was marked with, once it has been offered LibreraX for it. */
    public static void forget(final Context c, final String path) {
        if (TxtUtils.isNotEmpty(path)) {
            prefs(c).edit().remove(path).apply();
        }
    }
}
