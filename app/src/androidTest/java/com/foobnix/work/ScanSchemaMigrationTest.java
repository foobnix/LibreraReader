package com.foobnix.work;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import com.foobnix.dao2.DatabaseUpgradeHelper;
import org.greenrobot.greendao.database.StandardDatabase;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScanSchemaMigrationTest {
    @Test public void upgradesExistingMembershipWithoutLosingRowsAndIndexesPath() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE SCAN_MEMBERSHIP(ROOT TEXT NOT NULL,PATH TEXT NOT NULL,PRIMARY KEY(ROOT,PATH))");
            db.execSQL("INSERT INTO SCAN_MEMBERSHIP VALUES('root','book')");
            new DatabaseUpgradeHelper(InstrumentationRegistry.getInstrumentation().getTargetContext(), null)
                    .onUpgrade(new StandardDatabase(db), 10, 11);
            try (Cursor rows = db.rawQuery("SELECT PATH FROM SCAN_MEMBERSHIP", null)) {
                assertTrue(rows.moveToFirst()); assertEquals("book", rows.getString(0));
            }
            try (Cursor plan = db.rawQuery("EXPLAIN QUERY PLAN SELECT 1 FROM SCAN_MEMBERSHIP WHERE PATH=?", new String[]{"book"})) {
                assertTrue(plan.moveToFirst());
                assertTrue(plan.getString(3), plan.getString(3).contains("SCAN_MEMBERSHIP_PATH"));
            }
        }
    }
    @Test public void sidecarRevisionUpgradePreservesBookProgressAndMembership() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE FILE_META(PATH TEXT PRIMARY KEY,IS_RECENT_PROGRESS REAL)");
            db.execSQL("INSERT INTO FILE_META VALUES('book',0.75)");
            db.execSQL("CREATE TABLE SCAN_MEMBERSHIP(ROOT TEXT NOT NULL,PATH TEXT NOT NULL,PRIMARY KEY(ROOT,PATH))");
            db.execSQL("INSERT INTO SCAN_MEMBERSHIP VALUES('root','book')");
            new DatabaseUpgradeHelper(InstrumentationRegistry.getInstrumentation().getTargetContext(), null)
                    .onUpgrade(new StandardDatabase(db), 11, 12);
            try (Cursor row = db.rawQuery("SELECT IS_RECENT_PROGRESS,SAF_SIDECAR_REVISION FROM FILE_META", null)) {
                assertTrue(row.moveToFirst()); assertEquals(0.75f, row.getFloat(0), 0); assertTrue(row.isNull(1));
            }
            assertEquals(1, android.database.DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM SCAN_MEMBERSHIP", null));
        }
    }

}
