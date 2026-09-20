package com.foobnix.dao2;

import org.greenrobot.greendao.database.Database;

/** Scan membership belongs to the profile database's versioned schema. */
public final class ScanTables {
    private ScanTables() {}

    public static void create(Database db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS SCAN_MEMBERSHIP ("
                + "ROOT TEXT NOT NULL, PATH TEXT NOT NULL, PRIMARY KEY(ROOT,PATH))");
        db.execSQL("CREATE INDEX IF NOT EXISTS SCAN_MEMBERSHIP_PATH ON SCAN_MEMBERSHIP(PATH)");
    }

    public static void drop(Database db) { db.execSQL("DROP TABLE IF EXISTS SCAN_MEMBERSHIP"); }
}
