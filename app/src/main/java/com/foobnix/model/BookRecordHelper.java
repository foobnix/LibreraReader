package com.foobnix.model;

import org.greenrobot.greendao.database.Database;

/**
 * YuriReader Phase 2: Static helper providing BookRecord CRUD
 * without requiring callers to hold a Database reference.
 *
 * Call {@link #init(Database)} once (e.g., from AppDB.open()).
 * Safe to call load/save before init — returns null / no-op.
 */
public class BookRecordHelper {

    private static Database sDb;

    public static void init(Database db) {
        BookRecordHelper.sDb = db;
    }

    public static BookRecord load(String bookKey) {
        if (sDb == null || bookKey == null) return null;
        return BookRecordRepository.load(sDb, bookKey);
    }

    public static void save(BookRecord record) {
        if (sDb == null || record == null) return;
        BookRecordRepository.upsert(sDb, record);
    }

    public static void delete(String bookKey) {
        if (sDb == null || bookKey == null) return;
        BookRecordRepository.delete(sDb, bookKey);
    }

    public static String getStyleOverride(String bookKey) {
        BookRecord rec = load(bookKey);
        return rec != null ? rec.getStyleSettingsJson() : null;
    }

    public static void setStyleOverride(String bookKey, String styleSettingsJson) {
        BookRecord rec = load(bookKey);
        if (rec == null) {
            rec = new BookRecord(bookKey);
        }
        rec.setStyleSettingsJson(styleSettingsJson);
        save(rec);
    }

    public static String getParagraphConfig(String bookKey) {
        BookRecord rec = load(bookKey);
        return rec != null ? rec.getParagraphConfigJson() : null;
    }

    public static void setParagraphConfig(String bookKey, String paragraphConfigJson) {
        BookRecord rec = load(bookKey);
        if (rec == null) {
            rec = new BookRecord(bookKey);
        }
        rec.setParagraphConfigJson(paragraphConfigJson);
        save(rec);
    }

    public static String getEpubLocator(String bookKey) {
        BookRecord rec = load(bookKey);
        return rec != null ? rec.getEpubLocatorJson() : null;
    }

    public static void setEpubLocator(String bookKey, String epubLocatorJson) {
        BookRecord rec = load(bookKey);
        if (rec == null) {
            rec = new BookRecord(bookKey);
        }
        rec.setEpubLocatorJson(epubLocatorJson);
        save(rec);
    }

    public static String getTxtLocator(String bookKey) {
        BookRecord rec = load(bookKey);
        return rec != null ? rec.getTxtLocatorJson() : null;
    }

    public static void setTxtLocator(String bookKey, String txtLocatorJson) {
        BookRecord rec = load(bookKey);
        if (rec == null) {
            rec = new BookRecord(bookKey);
        }
        rec.setTxtLocatorJson(txtLocatorJson);
        save(rec);
    }

}