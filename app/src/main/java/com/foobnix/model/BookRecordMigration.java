package com.foobnix.model;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;

import org.greenrobot.greendao.database.Database;
import org.librera.LinkedJSONObject;

import java.io.File;

/**
 * YuriReader Phase 2: One-time migration from scattered JSON persistence
 * (SharedBooks APP_PROGRESS_JSON) → unified BOOK_RECORD SQLite table.
 *
 * Called once at app startup. Idempotent: if BOOK_RECORD already has entries,
 * migration is skipped.
 */
public class BookRecordMigration {

    private static final String TAG = "BookRecordMigration";

    public static void migrateIfNeeded(Database db) {
        if (db == null) {
            LOG.d(TAG, "db null, skipping");
            return;
        }

        // Idempotency: count existing BOOK_RECORD rows
        int existing = BookRecordRepository.loadAll(db).size();
        if (existing > 0) {
            LOG.d(TAG, "Migration already done, existing rows:", existing);
            return;
        }

        LOG.d(TAG, "Starting one-time migration from JSON → BOOK_RECORD");
        long t0 = System.currentTimeMillis();
        int migrated = 0;
        int skipped = 0;

        for (File file : AppProfile.getAllFiles(AppProfile.APP_PROGRESS_JSON)) {
            try {
                LinkedJSONObject obj = IO.readJsonObject(file);
                if (obj == null || !obj.keys().hasNext()) {
                    continue;
                }

                // Each top-level key is a bookKey (file name)
                for (java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                    String key = it.next();
                    if (TxtUtils.isEmpty(key)) {
                        continue;
                    }
                        try {
                            LinkedJSONObject bookObj = obj.getJSONObject(key);
                            AppBook appBook = new AppBook(key);
                            com.foobnix.android.utils.Objects.loadFromJson(appBook, bookObj);

                        // Skip entries with no progress or no path
                        if (appBook.p <= 0 && appBook.t <= 0) {
                            skipped++;
                            continue;
                        }

                        BookRecord rec = BookRecord.fromAppBook(appBook, key);
                        BookRecordRepository.insert(db, rec);
                        migrated++;
                    } catch (Exception inner) {
                        LOG.e(inner);
                    }
                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        long dt = System.currentTimeMillis() - t0;
        LOG.d(TAG, "Migration complete:", migrated, "rows migrated,", skipped, "skipped in", dt, "ms");
    }

}