package com.foobnix.sys;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;

import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.PageUrl;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The app's side of {@link CoverRenderService}: the covers of PDF, DjVu and TIFF books are drawn
 * in that process, one at a time, and one that is not drawn in {@link #timeLimitMs} has the
 * process killed under it. The book then gets the cover with its title, and is remembered - by its
 * path, size and time - so that it is not tried again each time the library is shown. A book that
 * crashes the process is remembered the same way.
 */
public final class CoverProcess {

    /** How long a cover may take; a page that takes longer is not one anybody waits for. */
    static volatile long timeLimitMs = 10_000;

    /** How long the process may take to start. */
    private static final long CONNECT_MS = 10_000;

    private static final String PREFS = "CoverProcess";

    private static final Object lock = new Object();
    private static final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();

    private static ICoverRenderer renderer;
    private static ServiceConnection connection;
    /** The cover process's id, once connected. */
    static volatile int pid;

    /** What the process made of a cover. */
    public static final class Result {
        /** The page, or null when it could not be drawn. */
        public final Bitmap bitmap;
        /** The page took too long or crashed the process, now or once before: it is not drawn. */
        public final boolean failed;

        Result(final Bitmap bitmap, final boolean failed) {
            this.bitmap = bitmap;
            this.failed = failed;
        }
    }

    private CoverProcess() {
    }

    /**
     * The page the url asks for, drawn in the cover process. Called on a thread that may wait:
     * on the main thread, or when the process cannot be reached, the page is drawn here instead.
     */
    public static Result render(final Context c, final PageUrl pageUrl) {
        final String path = pageUrl.getPath();
        if (isFailed(c, path)) {
            LOG.d("CoverProcess skip, failed before", path);
            return new Result(null, true);
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return new Result(ImageExtractor.getInstance(c).proccessOtherPage(pageUrl), false);
        }

        // One cover at a time: two waiting in the process would be killed together, and the
        // second one remembered for what the first did
        synchronized (CoverProcess.class) {
            final ICoverRenderer r = connect(c);
            if (r == null) {
                LOG.d("CoverProcess not connected, drawing here", path);
                return new Result(ImageExtractor.getInstance(c).proccessOtherPage(pageUrl), false);
            }
            final int target = pid;
            final AtomicBoolean late = new AtomicBoolean();
            final ScheduledFuture<?> kill = watchdog.schedule(() -> {
                late.set(true);
                LOG.d("CoverProcess too late, killing", target, path);
                Process.killProcess(target);
            }, timeLimitMs, TimeUnit.MILLISECONDS);
            final long start = System.currentTimeMillis();
            try {
                final Bitmap bitmap = r.render(pageUrl.toString(), AppState.get().getPdfEngine());
                kill.cancel(false);
                LOG.d("CoverProcess drawn", System.currentTimeMillis() - start, "ms", path);
                return new Result(bitmap, false);
            } catch (RemoteException | RuntimeException e) {
                kill.cancel(false);
                // killed for being late, or crashed by the book: either way not tried again
                LOG.d("CoverProcess failed", late.get() ? "late" : "crashed", System.currentTimeMillis() - start,
                        "ms", path, e);
                remember(c, path);
                disconnect(c);
                return new Result(null, true);
            }
        }
    }

    private static ICoverRenderer connect(final Context c) {
        synchronized (lock) {
            if (renderer != null && renderer.asBinder().isBinderAlive()) {
                return renderer;
            }
            renderer = null;
            if (connection == null) {
                connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(final ComponentName name, final IBinder service) {
                        synchronized (lock) {
                            renderer = ICoverRenderer.Stub.asInterface(service);
                            try {
                                pid = renderer.pid();
                            } catch (RemoteException e) {
                                LOG.e(e);
                                renderer = null;
                            }
                            lock.notifyAll();
                        }
                    }

                    @Override
                    public void onServiceDisconnected(final ComponentName name) {
                        synchronized (lock) {
                            renderer = null;
                        }
                    }
                };
                try {
                    final boolean bound = c.getApplicationContext().bindService(
                            new Intent(c, CoverRenderService.class), connection, Context.BIND_AUTO_CREATE);
                    if (!bound) {
                        connection = null;
                        return null;
                    }
                } catch (RuntimeException e) {
                    LOG.e(e);
                    connection = null;
                    return null;
                }
            }
            final long until = System.currentTimeMillis() + CONNECT_MS;
            while (renderer == null) {
                final long left = until - System.currentTimeMillis();
                if (left <= 0) {
                    return null;
                }
                try {
                    lock.wait(left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return renderer;
        }
    }

    /** Lets the killed process go; the next cover starts a new one. */
    private static void disconnect(final Context c) {
        synchronized (lock) {
            renderer = null;
            if (connection != null) {
                try {
                    c.getApplicationContext().unbindService(connection);
                } catch (RuntimeException e) {
                    LOG.e(e);
                }
                connection = null;
            }
        }
    }

    /** The book as it is now: a book changed since it failed is tried again. */
    private static String key(final String path) {
        final File file = new File(path);
        return path + "|" + file.length() + "|" + file.lastModified();
    }

    static boolean isFailed(final Context c, final String path) {
        return path != null && prefs(c).getBoolean(key(path), false);
    }

    private static void remember(final Context c, final String path) {
        if (path != null) {
            prefs(c).edit().putBoolean(key(path), true).apply();
        }
    }

    /** For tests: forgets every book that failed. */
    static void forgetFailed(final Context c) {
        prefs(c).edit().clear().commit();
    }

    private static SharedPreferences prefs(final Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
