package com.foobnix.work;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.DocumentsContract;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Keeps loading cursors alive until the provider has refreshed its listing. */
final class SafFolderQuery {
    private static final long TIMEOUT_MS = 30_000;

    private SafFolderQuery() {}

    interface Source {
        Cursor query(Uri uri, String[] projection);
        void observe(Uri uri, ContentObserver observer);
        void unobserve(ContentObserver observer);
    }

    static Cursor query(ContentResolver resolver, Uri childrenUri, String[] projection,
                        BooleanSupplier stopped) throws IOException, InterruptedException {
        return query(new Source() {
            public Cursor query(Uri uri, String[] columns) {
                return resolver.query(uri, columns, null, null, null);
            }
            public void observe(Uri uri, ContentObserver observer) {
                resolver.registerContentObserver(uri, true, observer);
            }
            public void unobserve(ContentObserver observer) {
                resolver.unregisterContentObserver(observer);
            }
        }, childrenUri, projection, stopped, TIMEOUT_MS);
    }

    static Cursor query(Source source, Uri childrenUri, String[] projection,
                        BooleanSupplier stopped, long timeoutMs) throws IOException, InterruptedException {
        long started = SystemClock.elapsedRealtime();
        int queries = 0;
        Semaphore changed = new Semaphore(0);
        ContentObserver observer = new ContentObserver(null) {
            @Override public void onChange(boolean selfChange) {
                changed.release();
            }
        };
        // Nextcloud notifies a plain document URI, rather than the tree children URI.
        Uri documentUri = DocumentsContract.buildDocumentUri(childrenUri.getAuthority(),
                DocumentsContract.getDocumentId(childrenUri));
        Cursor cursor = null;
        boolean returned = false;
        try {
            // Register before querying so a fast refresh cannot race observer registration.
            source.observe(documentUri, observer);
            source.observe(childrenUri, observer);
            long deadline = SystemClock.elapsedRealtime() + timeoutMs;
            queries++;
            cursor = source.query(childrenUri, projection);
            while (true) {
                if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
                if (cursor == null) throw new IOException("No SAF listing: " + childrenUri);
                cursor.registerContentObserver(observer);
                Bundle extras = cursor.getExtras();
                if (extras != null && extras.containsKey(DocumentsContract.EXTRA_ERROR)) {
                    throw new IOException("SAF listing failed: " + extras.getString(DocumentsContract.EXTRA_ERROR));
                }
                if (extras == null || !extras.getBoolean(DocumentsContract.EXTRA_LOADING, false)) {
                    android.util.Log.d("SafScan", "folder ms="
                            + (SystemClock.elapsedRealtime() - started) + " queries=" + queries
                            + " rows=" + cursor.getCount());
                    returned = true;
                    return cursor;
                }
                boolean notified = false;
                while (!notified && SystemClock.elapsedRealtime() < deadline) {
                    if (stopped.getAsBoolean()) throw new IOException("SAF scan cancelled");
                    notified = changed.tryAcquire(Math.min(250,
                            Math.max(1, deadline - SystemClock.elapsedRealtime())), TimeUnit.MILLISECONDS);
                }
                cursor.unregisterContentObserver(observer);
                // Keep the old cursor open during requery: closing it cancels Nextcloud's task.
                queries++;
                Cursor refreshed = source.query(childrenUri, projection);
                cursor.close();
                cursor = refreshed;
                if (SystemClock.elapsedRealtime() >= deadline && (cursor == null
                        || (cursor.getExtras() != null
                        && cursor.getExtras().getBoolean(DocumentsContract.EXTRA_LOADING, false)))) {
                    throw new IOException("Timed out refreshing SAF folder: " + childrenUri);
                }
            }
        } finally {
            source.unobserve(observer);
            if (cursor != null) {
                if (returned) cursor.unregisterContentObserver(observer);
                else cursor.close();
            }
        }
    }
}
