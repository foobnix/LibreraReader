package com.foobnix.pdf.info;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.model.AppSP;
import org.junit.Test;
import static org.junit.Assert.*;

public class SafStateStoreTest {
    @Test public void recordsSurviveReopeningAndStayInTheirProfile() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String original = AppSP.get().currentProfile;
        String profile = "state-test-" + java.util.UUID.randomUUID();
        try {
            AppSP.get().currentProfile = profile;
            SafStateStore.get(context, "labels").edit().putString("book", "Books / Title.epub").commit();
            assertEquals("Books / Title.epub", SafStateStore.get(context, "labels").getString("book", null));
            AppSP.get().currentProfile = profile + "-other";
            assertNull(SafStateStore.get(context, "labels").getString("book", null));
            AppSP.get().currentProfile = profile;
            assertEquals("Books / Title.epub", SafStateStore.get(context, "labels").getString("book", null));
            SafStateStore.get(context, "labels").edit().clear().commit();
            assertNull(SafStateStore.get(context, "labels").getString("book", null));
        } finally { AppSP.get().currentProfile = original; }
    }

    @Test public void oversizedOptimizationIsDiscardedAndOrphansArePruned() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SafStateStore cache = SafStateStore.get(context, "state-test-" + java.util.UUID.randomUUID());
        char[] oversized = new char[2 * 1024 * 1024 + 1];
        java.util.Arrays.fill(oversized, 'x');
        cache.edit().putString("oversized", new String(oversized)).putString("retained", "revision")
                .putString("orphan", "old").commit();
        assertFalse(cache.contains("oversized"));
        cache.retain(java.util.Collections.singleton("retained"));
        assertTrue(cache.contains("retained"));
        assertFalse(cache.contains("orphan"));
        cache.edit().clear().commit();
    }
    @Test public void coldUiReadsDoNotOpenSqliteOrPerformDiskIo() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String original = AppSP.get().currentProfile;
        String profile = "ui-cold-" + java.util.UUID.randomUUID();
        AppSP.get().currentProfile = profile;
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                android.os.StrictMode.ThreadPolicy previous = android.os.StrictMode.getThreadPolicy();
                android.os.StrictMode.setThreadPolicy(new android.os.StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads().detectDiskWrites().penaltyDeath().build());
                try {
                    assertEquals("fallback", SafStateStore.get(context, "labels").getString("missing", "fallback"));
                    assertEquals(context.getString(R.string.folder), SafPathLabels.displayName(context, "content://cold.fixture/document/book"));
                } catch (Throwable error) { failure.set(error); }
                finally { android.os.StrictMode.setThreadPolicy(previous); }
            });
            if (failure.get() != null) throw new AssertionError(failure.get());
        } finally { AppSP.get().currentProfile = original; }
    }

    @Test public void accountingTracksUpdatesDeletesAndUtf8Bytes() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String original = AppSP.get().currentProfile;
        String profile = "accounting-" + java.util.UUID.randomUUID();
        try {
            AppSP.get().currentProfile = profile;
            SafStateStore store = SafStateStore.get(context, "accounting");
            store.edit().putString("book", "éé").commit();
            store.edit().putString("book", "é").putString("other", "abc").commit();
            store.edit().remove("other").commit();
            FileAccounting.assertTotals(context, profile, 2, 1);
            store.edit().clear().commit();
            FileAccounting.assertTotals(context, profile, 0, 0);
        } finally { AppSP.get().currentProfile = original; }
    }
    private static class FileAccounting {
        static void assertTotals(Context context, String profile, long bytes, long entries) {
            try (android.database.sqlite.SQLiteDatabase db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath("saf-state-" + android.net.Uri.encode(profile) + ".db").getPath(), null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
                assertEquals(bytes, android.database.DatabaseUtils.longForQuery(db, "SELECT SUM(BYTES) FROM STATE_TOTALS", null));
                assertEquals(entries, android.database.DatabaseUtils.longForQuery(db, "SELECT SUM(ENTRIES) FROM STATE_TOTALS", null));
            }
        }
    }

    @Test public void uiReadDoesNotWaitForTheDatabaseOpeningMonitor() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SafStateStore store = SafStateStore.get(context, "opening-" + java.util.UUID.randomUUID());
        java.lang.reflect.Field field = SafStateStore.class.getDeclaredField("helper");
        field.setAccessible(true);
        Object helper = field.get(store);
        java.util.concurrent.CountDownLatch locked = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch read = new java.util.concurrent.CountDownLatch(1);
        Thread opening = new Thread(() -> {
            synchronized (helper) {
                locked.countDown();
                try { release.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        });
        opening.start();
        try {
            assertTrue(locked.await(2, java.util.concurrent.TimeUnit.SECONDS));
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                store.getString("missing", "fallback");
                read.countDown();
            });
            assertTrue("UI waited behind SQLiteOpenHelper's opening monitor",
                    read.await(1, java.util.concurrent.TimeUnit.SECONDS));
        } finally { release.countDown(); opening.join(5000); }
    }

}
