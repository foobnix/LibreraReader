package com.foobnix.librerax;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.foobnix.android.utils.TxtUtils;

/**
 * Remembers which book the app was opening when it died, so the next open of it can offer
 * LibreraX instead of crashing again.
 *
 * A native reader that crashes takes the process with it, and nothing in it runs to say so - so
 * the book is marked before it is opened, with a synchronous write the crash cannot lose, and
 * unmarked once it is up and its first pages have been drawn. A mark still there when the book
 * is opened again is a crash. ImageExtractor guards book covers the same way.
 */
public final class BookOpenGuard {

    private static final String PREFS = "BookOpenGuard";

    /** How long after a book is up a crash still counts against opening it: its first pages draw then. */
    private static final long SETTLED_MS = 5000;

    private BookOpenGuard() {
    }

    private static SharedPreferences prefs(final Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Whether the app died the last time it was opening this book. */
    public static boolean crashedLastTime(final Context c, final String path) {
        return TxtUtils.isNotEmpty(path) && prefs(c).contains(path);
    }

    /** Marks the book as being opened; written before returning, so a crash right after keeps it. */
    public static void opening(final Context c, final String path) {
        if (TxtUtils.isNotEmpty(path)) {
            prefs(c).edit().putBoolean(path, true).commit();
        }
    }

    /** The book opened, or failed in a way the app handled itself: no crash to remember. */
    public static void opened(final Context c, final String path) {
        if (TxtUtils.isNotEmpty(path)) {
            prefs(c).edit().remove(path).apply();
        }
    }

    /** Unmarks the book once its first pages have had time to draw. */
    public static void openedSoon(final View view, final String path) {
        final Context c = view.getContext().getApplicationContext();
        view.postDelayed(() -> opened(c, path), SETTLED_MS);
    }
}
