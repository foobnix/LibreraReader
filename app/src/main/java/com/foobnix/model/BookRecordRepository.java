package com.foobnix.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.foobnix.android.utils.LOG;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.StandardDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * YuriReader BookRecord Repository (Phase 2)
 *
 * CRUD for the unified BOOK_RECORD table.
 * Does NOT depend on GreenDAO code generation.
 * Table created via raw SQL migration (MigrationV10).
 */
public class BookRecordRepository {

    public static final String TABLE_NAME = "BOOK_RECORD";

    public static final String COL_BOOK_KEY = "BOOK_KEY";
    public static final String COL_LAST_READ_TIME = "LAST_READ_TIME";
    public static final String COL_CURRENT_PAGE_INDEX = "CURRENT_PAGE_INDEX";
    public static final String COL_PROGRESS_PERCENT = "PROGRESS_PERCENT";
    public static final String COL_TOTAL_PAGES = "TOTAL_PAGES";
    public static final String COL_RENDER_FLAGS_JSON = "RENDER_FLAGS_JSON";
    public static final String COL_ZOOM = "ZOOM";
    public static final String COL_OFFSET_X = "OFFSET_X";
    public static final String COL_OFFSET_Y = "OFFSET_Y";
    public static final String COL_SCROLL_SPEED = "SCROLL_SPEED";
    public static final String COL_BOOKMARKS_JSON = "BOOKMARKS_JSON";
    public static final String COL_ANNOTATIONS_JSON = "ANNOTATIONS_JSON";
    public static final String COL_STYLE_SETTINGS_JSON = "STYLE_SETTINGS_JSON";
    public static final String COL_PARAGRAPH_CONFIG_JSON = "PARAGRAPH_CONFIG_JSON";
    public static final String COL_EPUB_LOCATOR_JSON = "EPUB_LOCATOR_JSON";
    public static final String COL_TXT_LOCATOR_JSON = "TXT_LOCATOR_JSON";
    public static final String COL_CONTENT_FINGERPRINT = "CONTENT_FINGERPRINT";
    public static final String COL_ADDED_TIME = "ADDED_TIME";

    private static final String[] ALL_COLUMNS = {
            COL_BOOK_KEY, COL_LAST_READ_TIME, COL_CURRENT_PAGE_INDEX, COL_PROGRESS_PERCENT,
            COL_TOTAL_PAGES, COL_RENDER_FLAGS_JSON, COL_ZOOM, COL_OFFSET_X, COL_OFFSET_Y,
            COL_SCROLL_SPEED, COL_BOOKMARKS_JSON, COL_ANNOTATIONS_JSON, COL_STYLE_SETTINGS_JSON,
            COL_PARAGRAPH_CONFIG_JSON, COL_EPUB_LOCATOR_JSON, COL_TXT_LOCATOR_JSON,
            COL_CONTENT_FINGERPRINT, COL_ADDED_TIME
    };

    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists ? "IF NOT EXISTS " : "";
        db.execSQL("CREATE TABLE " + constraint + TABLE_NAME + " (" +
                COL_BOOK_KEY + " TEXT PRIMARY KEY," +
                COL_LAST_READ_TIME + " INTEGER," +
                COL_CURRENT_PAGE_INDEX + " INTEGER DEFAULT 0," +
                COL_PROGRESS_PERCENT + " REAL DEFAULT 0," +
                COL_TOTAL_PAGES + " INTEGER DEFAULT 0," +
                COL_RENDER_FLAGS_JSON + " TEXT," +
                COL_ZOOM + " REAL DEFAULT 1.0," +
                COL_OFFSET_X + " REAL DEFAULT 0," +
                COL_OFFSET_Y + " REAL DEFAULT 0," +
                COL_SCROLL_SPEED + " INTEGER DEFAULT 0," +
                COL_BOOKMARKS_JSON + " TEXT," +
                COL_ANNOTATIONS_JSON + " TEXT," +
                COL_STYLE_SETTINGS_JSON + " TEXT," +
                COL_PARAGRAPH_CONFIG_JSON + " TEXT," +
                COL_EPUB_LOCATOR_JSON + " TEXT," +
                COL_TXT_LOCATOR_JSON + " TEXT," +
                COL_CONTENT_FINGERPRINT + " TEXT," +
                COL_ADDED_TIME + " INTEGER DEFAULT 0" +
                ");");
    }

    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + TABLE_NAME;
        db.execSQL(sql);
    }

    public static BookRecord load(Database db, String bookKey) {
        if (bookKey == null) return null;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_BOOK_KEY + " = ?",
                    new String[]{bookKey});
            if (cursor.moveToFirst()) {
                return cursorToRecord(cursor);
            }
        } catch (Exception e) {
            LOG.e(e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public static void save(Database db, BookRecord record) {
        upsert(db, record);
    }

    public static void insert(Database db, BookRecord record) {
        if (record == null || record.getBookKey() == null) return;
        try {
            SQLiteDatabase sqlDb = ((StandardDatabase) db).getRawDatabase();
            ContentValues cv = recordToContentValues(record);
            sqlDb.insert(TABLE_NAME, null, cv);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static int update(Database db, String bookKey, ContentValues cv) {
        if (bookKey == null) return 0;
        try {
            SQLiteDatabase sqlDb = ((StandardDatabase) db).getRawDatabase();
            return sqlDb.update(TABLE_NAME, cv, COL_BOOK_KEY + " = ?", new String[]{bookKey});
        } catch (Exception e) {
            LOG.e(e);
            return 0;
        }
    }

    public static void upsert(Database db, BookRecord record) {
        if (record == null || record.getBookKey() == null) return;
        int rows = update(db, record.getBookKey(), recordToContentValues(record));
        if (rows == 0) {
            insert(db, record);
        }
    }

    public static void delete(Database db, String bookKey) {
        if (bookKey == null) return;
        try {
            SQLiteDatabase sqlDb = ((StandardDatabase) db).getRawDatabase();
            sqlDb.delete(TABLE_NAME, COL_BOOK_KEY + " = ?", new String[]{bookKey});
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static List<BookRecord> loadAll(Database db) {
        List<BookRecord> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
            while (cursor.moveToNext()) {
                list.add(cursorToRecord(cursor));
            }
        } catch (Exception e) {
            LOG.e(e);
        }finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public static void deleteAll(Database db) {
        try {
            db.execSQL("DELETE FROM " + TABLE_NAME);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    // --- Helpers ---

    static BookRecord cursorToRecord(Cursor cursor) {
        BookRecord rec = new BookRecord();
        rec.setBookKey(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_KEY)));
        rec.setLastReadTime(cursor.getLong(cursor.getColumnIndexOrThrow(COL_LAST_READ_TIME)));
        rec.setCurrentPageIndex(cursor.getInt(cursor.getColumnIndexOrThrow(COL_CURRENT_PAGE_INDEX)));
        rec.setProgressPercent(cursor.getFloat(cursor.getColumnIndexOrThrow(COL_PROGRESS_PERCENT)));
        rec.setTotalPages(cursor.getInt(cursor.getColumnIndexOrThrow(COL_TOTAL_PAGES)));
        rec.setRenderFlagsJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_RENDER_FLAGS_JSON)));
        rec.setZoom(cursor.getFloat(cursor.getColumnIndexOrThrow(COL_ZOOM)));
        rec.setOffsetX(cursor.getFloat(cursor.getColumnIndexOrThrow(COL_OFFSET_X)));
        rec.setOffsetY(cursor.getFloat(cursor.getColumnIndexOrThrow(COL_OFFSET_Y)));
        rec.setScrollSpeed(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCROLL_SPEED)));
        rec.setBookmarksJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKMARKS_JSON)));
        rec.setAnnotationsJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_ANNOTATIONS_JSON)));
        rec.setStyleSettingsJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_STYLE_SETTINGS_JSON)));
        rec.setParagraphConfigJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_PARAGRAPH_CONFIG_JSON)));
        rec.setEpubLocatorJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_EPUB_LOCATOR_JSON)));
        rec.setTxtLocatorJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_TXT_LOCATOR_JSON)));
        rec.setContentFingerprint(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT_FINGERPRINT)));
        rec.setAddedTime(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ADDED_TIME)));
        return rec;
    }

    static ContentValues recordToContentValues(BookRecord rec) {
        ContentValues cv = new ContentValues();
        cv.put(COL_BOOK_KEY, rec.getBookKey());
        cv.put(COL_LAST_READ_TIME, rec.getLastReadTime());
        cv.put(COL_CURRENT_PAGE_INDEX, rec.getCurrentPageIndex());
        cv.put(COL_PROGRESS_PERCENT, rec.getProgressPercent());
        cv.put(COL_TOTAL_PAGES, rec.getTotalPages());
        cv.put(COL_RENDER_FLAGS_JSON, rec.getRenderFlagsJson());
        cv.put(COL_ZOOM, rec.getZoom());
        cv.put(COL_OFFSET_X, rec.getOffsetX());
        cv.put(COL_OFFSET_Y, rec.getOffsetY());
        cv.put(COL_SCROLL_SPEED, rec.getScrollSpeed());
        cv.put(COL_BOOKMARKS_JSON, rec.getBookmarksJson());
        cv.put(COL_ANNOTATIONS_JSON, rec.getAnnotationsJson());
        cv.put(COL_STYLE_SETTINGS_JSON, rec.getStyleSettingsJson());
        cv.put(COL_PARAGRAPH_CONFIG_JSON, rec.getParagraphConfigJson());
        cv.put(COL_EPUB_LOCATOR_JSON, rec.getEpubLocatorJson());
        cv.put(COL_TXT_LOCATOR_JSON, rec.getTxtLocatorJson());
        cv.put(COL_CONTENT_FINGERPRINT, rec.getContentFingerprint());
        cv.put(COL_ADDED_TIME, rec.getAddedTime());
        return cv;
    }
}