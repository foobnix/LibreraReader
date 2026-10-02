package com.foobnix.pdf.info;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Looper;
import com.foobnix.model.AppProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Per-profile optimization records. UI reads use memory; disk work runs on background callers. */
public final class SafStateStore {
    private static final Map<String, Helper> DATABASES = new HashMap<>();
    private static final ExecutorService READS = Executors.newSingleThreadExecutor();
    private static final int MAX_ENTRIES = 50_000;
    private static final long MAX_BYTES = 32L * 1024 * 1024;
    private final Helper helper;
    private final String namespace;
    private final String identity;

    private static final class Helper extends SQLiteOpenHelper {
        final Map<String, String> memory = new LinkedHashMap<>(128, .75f, true);
        final Set<String> loading = new HashSet<>();
        final Object writer = new Object();
        final Object memoryLock = new Object();
        long generation;
        long memoryChars;
        Helper(Context context, String name) {
            super(context, name, null, 2);
            setWriteAheadLoggingEnabled(true);
        }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE STATE (NAMESPACE TEXT NOT NULL, KEY TEXT NOT NULL, "
                    + "VALUE TEXT NOT NULL, UPDATED INTEGER NOT NULL, PRIMARY KEY(NAMESPACE,KEY))");
            db.execSQL("CREATE INDEX STATE_AGE ON STATE(NAMESPACE,UPDATED)");
            db.execSQL("CREATE INDEX STATE_GLOBAL_AGE ON STATE(UPDATED)");
            createAccounting(db);
        }
        private void createAccounting(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE STATE_TOTALS (NAMESPACE TEXT PRIMARY KEY, BYTES INTEGER NOT NULL, ENTRIES INTEGER NOT NULL)");
            // One migration-time aggregate; subsequent writes account only for changed rows.
            db.execSQL("INSERT INTO STATE_TOTALS SELECT NAMESPACE,SUM(length(CAST(VALUE AS BLOB))),COUNT(*) FROM STATE GROUP BY NAMESPACE");
            db.execSQL("CREATE TRIGGER STATE_INSERT AFTER INSERT ON STATE BEGIN "
                    + "INSERT OR IGNORE INTO STATE_TOTALS VALUES(NEW.NAMESPACE,0,0); "
                    + "UPDATE STATE_TOTALS SET BYTES=BYTES+length(CAST(NEW.VALUE AS BLOB)),ENTRIES=ENTRIES+1 WHERE NAMESPACE=NEW.NAMESPACE; END");
            db.execSQL("CREATE TRIGGER STATE_DELETE AFTER DELETE ON STATE BEGIN "
                    + "UPDATE STATE_TOTALS SET BYTES=BYTES-length(CAST(OLD.VALUE AS BLOB)),ENTRIES=ENTRIES-1 WHERE NAMESPACE=OLD.NAMESPACE; END");
            db.execSQL("CREATE TRIGGER STATE_UPDATE AFTER UPDATE ON STATE BEGIN "
                    + "UPDATE STATE_TOTALS SET BYTES=BYTES-length(CAST(OLD.VALUE AS BLOB))+length(CAST(NEW.VALUE AS BLOB)) WHERE NAMESPACE=NEW.NAMESPACE; END");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1) createAccounting(db);
        }
        void invalidate(String namespace, Set<String> changed, boolean clear) {
            synchronized (memoryLock) {
                generation++;
                String prefix = namespace + "\n";
                java.util.Iterator<Map.Entry<String, String>> entries = memory.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<String, String> entry = entries.next();
                    if (namespace == null || (entry.getKey().startsWith(prefix)
                            && (clear || changed.contains(entry.getKey().substring(prefix.length()))))) {
                        if (entry.getValue() != null) memoryChars -= entry.getValue().length();
                        entries.remove();
                    }
                }
            }
        }
        void remember(String key, String value, long readGeneration) {
            synchronized (memoryLock) {
                if (generation != readGeneration || (value != null && value.length() > 64 * 1024)) return;
                String previous = memory.put(key, value);
                memoryChars += (value == null ? 0 : value.length()) - (previous == null ? 0 : previous.length());
                while (memory.size() > 1024 || memoryChars > 2 * 1024 * 1024) {
                    String oldest = memory.keySet().iterator().next();
                    String removed = memory.remove(oldest);
                    if (removed != null) memoryChars -= removed.length();
                }
            }
        }
    }

    public static SafStateStore get(Context context, String namespace) {
        return new SafStateStore(context, namespace);
    }
    private SafStateStore(Context context, String namespace) {
        String profile = AppProfile.getCurrent();
        String name = "saf-state-" + android.net.Uri.encode(profile == null ? "default" : profile) + ".db";
        String key = context.getApplicationInfo().dataDir + "/databases/" + name;
        synchronized (DATABASES) {
            helper = DATABASES.computeIfAbsent(key, ignored -> new Helper(context.getApplicationContext(), name));
        }
        this.namespace = namespace;
        identity = key + "|" + namespace;
    }
    String identity() { return identity; }
    private String memoryKey(String key) { return namespace + "\n" + key; }
    public String getString(String key, String fallback) {
        String memoryKey = memoryKey(key);
        long generation;
        synchronized (helper.memoryLock) {
            if (helper.memory.containsKey(memoryKey)) {
                String value = helper.memory.get(memoryKey);
                return value == null ? fallback : value;
            }
            generation = helper.generation;
            if (Looper.myLooper() == Looper.getMainLooper()) {
                if (helper.loading.add(memoryKey)) READS.execute(() -> {
                    try { getString(key, null); }
                    catch (RuntimeException failure) { com.foobnix.android.utils.LOG.e(failure); }
                    finally { synchronized (helper.memoryLock) { helper.loading.remove(memoryKey); } }
                });
                return fallback;
            }
        }
        String value;
        try (Cursor rows = helper.getReadableDatabase().rawQuery(
                "SELECT VALUE FROM STATE WHERE NAMESPACE=? AND KEY=?", new String[]{namespace, key})) {
            value = rows.moveToFirst() ? rows.getString(0) : null;
        }
        helper.remember(memoryKey, value, generation);
        return value == null ? fallback : value;
    }
    public boolean contains(String key) { return getString(key, null) != null; }
    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();
        try (Cursor rows = helper.getReadableDatabase().rawQuery(
                "SELECT KEY,VALUE FROM STATE WHERE NAMESPACE=?", new String[]{namespace})) {
            while (rows.moveToNext()) result.put(rows.getString(0), rows.getString(1));
        }
        return result;
    }
    /** Prune keys without loading the corresponding potentially large values. */
    public void retain(Set<String> keys) {
        Editor editor = edit();
        try (Cursor rows = helper.getReadableDatabase().rawQuery(
                "SELECT KEY FROM STATE WHERE NAMESPACE=?", new String[]{namespace})) {
            while (rows.moveToNext()) if (!keys.contains(rows.getString(0))) editor.remove(rows.getString(0));
        }
        editor.commit();
    }
    public Editor edit() { return new Editor(); }
    public final class Editor {
        final Map<String, String> changed = new HashMap<>();
        boolean clear;
        public Editor putString(String key, String value) {
            changed.put(key, value != null && value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 2 * 1024 * 1024 ? null : value);
            return this;
        }
        public Editor remove(String key) { return putString(key, null); }
        public Editor clear() { clear = true; return this; }
        /** Synchronous persistence: callers must run writes off the UI thread. */
        public boolean commit() {
            if (!clear && changed.isEmpty()) return true;
            synchronized (helper.writer) { return persist(); }
        }
        private boolean persist() {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            helper.invalidate(namespace, changed.keySet(), clear);
            boolean trimmed = false;
            boolean succeeded = false;
            try {
                if (clear) db.delete("STATE", "NAMESPACE=?", new String[]{namespace});
                long now = System.currentTimeMillis();
                for (Map.Entry<String, String> entry : changed.entrySet()) {
                    if (entry.getValue() == null) db.delete("STATE", "NAMESPACE=? AND KEY=?", new String[]{namespace, entry.getKey()});
                    else {
                        // UPDATE rather than REPLACE ensures delete triggers cannot be skipped.
                        db.execSQL("INSERT OR IGNORE INTO STATE VALUES(?,?,?,?)", new Object[]{namespace, entry.getKey(), entry.getValue(), now});
                        db.execSQL("UPDATE STATE SET VALUE=?,UPDATED=? WHERE NAMESPACE=? AND KEY=?", new Object[]{entry.getValue(), now, namespace, entry.getKey()});
                    }
                }
                long count = DatabaseUtils.longForQuery(db, "SELECT COALESCE((SELECT ENTRIES FROM STATE_TOTALS WHERE NAMESPACE=?),0)", new String[]{namespace});
                if (count > MAX_ENTRIES) {
                    trimmed = true;
                    db.execSQL("DELETE FROM STATE WHERE rowid IN (SELECT rowid FROM STATE WHERE NAMESPACE=? ORDER BY UPDATED,rowid LIMIT ?)", new Object[]{namespace, count - MAX_ENTRIES});
                }
                while (DatabaseUtils.longForQuery(db, "SELECT COALESCE(SUM(BYTES),0) FROM STATE_TOTALS", null) > MAX_BYTES) {
                    trimmed = true;
                    db.execSQL("DELETE FROM STATE WHERE rowid IN (SELECT rowid FROM STATE ORDER BY UPDATED,rowid LIMIT 16)");
                }
                db.setTransactionSuccessful();
                succeeded = true;
                return true;
            } catch (android.database.sqlite.SQLiteException unavailable) {
                com.foobnix.android.utils.LOG.e(unavailable);
                return false;
            } finally {
                db.endTransaction();
                synchronized (helper.memoryLock) {
                    helper.invalidate(trimmed ? null : namespace, changed.keySet(), clear);
                    if (succeeded && !trimmed) for (Map.Entry<String, String> entry : changed.entrySet())
                        helper.remember(memoryKey(entry.getKey()), entry.getValue(), helper.generation);
                }
            }
        }
    }
}
