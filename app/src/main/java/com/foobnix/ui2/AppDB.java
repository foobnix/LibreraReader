package com.foobnix.ui2;

import android.content.Context;
import android.net.Uri;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.foobnix.LibreraBuildConfig;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.StringDB;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.DaoMaster;
import com.foobnix.dao2.DaoSession;
import com.foobnix.dao2.DatabaseUpgradeHelper;
import com.foobnix.dao2.DictMeta;
import com.foobnix.dao2.DictMetaDao;
import com.foobnix.dao2.FileMeta;
import com.foobnix.dao2.FileMetaDao;
import com.foobnix.ext.PubDate;
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.SafDocumentIdentity;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fragment.SearchFragment2;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AppDB {

    private final static AppDB in = new AppDB();
    DatabaseUpgradeHelper helper;
    String currentDB;
    private FileMetaDao fileMetaDao;
    private DaoSession daoSession;
    private DictMetaDao dictMetaDao;


    public static AppDB get() {
        return in;
    }

    public static List<FileMeta> removeNotExist(List<FileMeta> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<FileMeta>();
        }
        Iterator<FileMeta> iterator = items.iterator();
        while (iterator.hasNext()) {
            FileMeta next = iterator.next();
            if (Clouds.isCloud(next.getPath())) {
                continue;
            }

            if (!ExtUtils.isAvailableBookSource(next.getPath())) {
                iterator.remove();
            }
        }
        return items;
    }

    public static void removeClouds(List<FileMeta> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Iterator<FileMeta> iterator = items.iterator();
        while (iterator.hasNext()) {
            FileMeta next = iterator.next();
            if (Clouds.isCloud(next.getPath())) {
                File cacheFile = Clouds.getCacheFile(next.getPath());
                if (cacheFile != null) {
                    next.setPath(cacheFile.getPath());
                } else {
                    iterator.remove();

                }
            }
        }
    }

    public FileMetaDao getDao() {
        return fileMetaDao;
    }

    public boolean isFolder(FileMeta meta) {
        return meta.getCusType() != null && meta.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY;

    }

    public synchronized void open(Context c, String appDB) {

        if (appDB.equals(currentDB)) {
            LOG.d("Open-DB skip", appDB);
            return;
        }
        LOG.d("Open-DB", appDB);
        currentDB = appDB;

        if (helper != null) {
            helper.close();
        }
        helper = new DatabaseUpgradeHelper(c, appDB);


        SQLiteDatabase writableDatabase = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(writableDatabase);


        daoSession = daoMaster.newSession();

        fileMetaDao = daoSession.getFileMetaDao();

        // Calibre's "no date" (0101-01-01, or 0100-12-31 after a time zone) used to be kept
        // as a book published in the year 100 or 101.
        try {
            writableDatabase.execSQL("UPDATE " + FileMetaDao.TABLENAME + " SET " + FileMetaDao.Properties.Year.columnName +
                                     " = NULL WHERE " + FileMetaDao.Properties.Year.columnName + " <= " + PubDate.UNDEFINED_YEAR);
            writableDatabase.execSQL("UPDATE " + FileMetaDao.TABLENAME + " SET " + FileMetaDao.Properties.PubDate.columnName +
                                     " = NULL WHERE " + FileMetaDao.Properties.Year.columnName + " IS NULL");
        } catch (Exception e) {
            LOG.e(e);
        }

        if (AppsConfig.IS_LOG) {
            QueryBuilder.LOG_SQL = true;
            QueryBuilder.LOG_VALUES = true;
        }

    }

    public void openDictDB(Context c, String path) {
        DaoMaster.OpenHelper helper = new DaoMaster.OpenHelper(c, path) {
            @Override
            public void onCreate(Database db) {
                //super.onCreate(db);
            }
        };

        SQLiteDatabase readableDatabase = helper.getReadableDatabase();
        DaoMaster daoMaster = new DaoMaster(readableDatabase);
        DaoSession daoSession = daoMaster.newSession();

        dictMetaDao = daoSession.getDictMetaDao();
        LOG.d("openDictDB open", path);
    }

    public String findDict(String key) {
        key = key.toLowerCase();
        LOG.d("openDictDB findDict key", key);

        final List<DictMeta> list = dictMetaDao.queryBuilder().where(DictMetaDao.Properties.Key.eq(key)).list();
        if (TxtUtils.isListNotEmpty(list)) {
            final String value = list.get(0).getValue();
            LOG.d("openDictDB findDict value", value);
            return value;
        }
        return key;
    }


    //public void dropCreateTables(Context c) {
    //    DatabaseUpgradeHelper helper = new DatabaseUpgradeHelper(c, DB_NAME);
    //    DaoMaster.dropAllTables(helper.getWritableDb(), true);
    //    DaoMaster.createAllTables(helper.getWritableDb(), true);
    // }

    public void deleteAllData() {
        if (fileMetaDao == null) {
            return;
        }
        fileMetaDao.deleteAll();

    }

    public List<FileMeta> deleteAllSafe() {
        try {
            List<FileMeta> list = fileMetaDao.queryBuilder().whereOr(FileMetaDao.Properties.Tag.isNotNull(), FileMetaDao.Properties.IsStar.eq(1), FileMetaDao.Properties.IsRecent.eq(1)).list();
            if (list == null) {
                list = new ArrayList<FileMeta>();
            }
            fileMetaDao.deleteAll();
            return list;
        } catch (Exception e) {
            LOG.e(e);
            return new ArrayList<FileMeta>();
        }
    }

    public void delete(FileMeta meta) {
        fileMetaDao.delete(meta);
    }

    public void deleteBy(String metaByPath) {
        fileMetaDao.deleteByKey(metaByPath);
    }

    public List<FileMeta> getRecentDeprecated() {
        try {
            List<FileMeta> list = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).list();
            return removeNotExist(list);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public FileMeta getRecentLastNoFolder() {
        List<FileMeta> list = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).limit(1).list();
        removeNotExist(list);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public void addRecent(String path) {
        if (!UITab.isShowRecent()) {
            return;
        }

        if (!ExtUtils.isAvailableBookSource(path)) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("Add Recent", path);
        if (!path.endsWith("json") && !path.endsWith("temp.txt")) {
            FileMeta load = getOrCreate(path);
            load.setIsRecent(true);
            load.setIsRecentTime(System.currentTimeMillis());
            fileMetaDao.update(load);

            AppData.get().addRecent(new SimpleMeta(path, System.currentTimeMillis()));
        }

    }

    public void addStarFile(String path) {
        if (!ExtUtils.isAvailableBookSource(path)) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("addStarFile", path);
        FileMeta load = getOrCreate(path);
        load.setIsStar(true);
        load.setIsStarTime(System.currentTimeMillis());
        load.setCusType(FileMetaAdapter.DISPLAY_TYPE_FILE);
        fileMetaDao.update(load);
    }

    public void addStarFolder(String path) {
        if (!new File(path).isDirectory()) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("addStarFile", path);
        FileMeta load = getOrCreate(path);
        load.setPathTxt(ExtUtils.getFileName(path));
        load.setIsStar(true);
        load.setIsStarTime(System.currentTimeMillis());
        load.setCusType(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY);
        fileMetaDao.update(load);
    }

    public void save(FileMeta meta) {
        fileMetaDao.save(meta);
    }

    public long getCount() {
        try {
            return fileMetaDao.queryBuilder().count();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<FileMeta> getAll() {
        try {
            return fileMetaDao.queryBuilder().list();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<FileMeta> getAllByState(int state) {
        try {
            return fileMetaDao.queryBuilder().where(FileMetaDao.Properties.State.eq(state)).list();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public void setIsSearchBook(String path, boolean value) {
        final FileMeta load = AppDB.get().load(path);
        if (load != null) {
            load.setIsSearchBook(value);
            AppDB.get().update(load);
        }
    }

    public void update(FileMeta load) {
        if (fileMetaDao != null) {
            fileMetaDao.save(load);
        }

    }
    public void updateUpdate(FileMeta load) {
        if (fileMetaDao != null) {
            fileMetaDao.update(load);
        }

    }

    public FileMeta load(String path) {
        if (fileMetaDao == null) {
            return null;
        }
        if (ExtUtils.isExteralSD(path))
            path = SafDocumentIdentity.canonical(Uri.parse(path)).toString();
        return fileMetaDao.load(path);
    }

    /** Update only reading progress, preserving concurrently extracted book metadata. */
    public void updateReadingProgress(String path, float progress) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null) return;
        dao.getDatabase().execSQL("UPDATE FILE_META SET IS_RECENT_PROGRESS=? WHERE PATH=?",
                new Object[]{progress, path});
        FileMeta cached = dao.load(path);
        if (cached != null) cached.setIsRecentProgress(progress);
    }

    public void initializeReadingProgress(String path, float progress, long time) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null) return;
        dao.getDatabase().execSQL("UPDATE FILE_META SET "
                        + "IS_RECENT_PROGRESS=COALESCE(IS_RECENT_PROGRESS,?),"
                        + "IS_RECENT_TIME=COALESCE(IS_RECENT_TIME,?) WHERE PATH=?",
                new Object[]{progress, time, path});
        dao.detachAll();
    }

    /** Scan-owned sidecar facts are independent of extracted metadata and user edits. */
    public void updateSidecarRevision(FileMeta book, String revision) {
        book.setSafSidecarRevision(revision);
        updateSidecarRevision(fileMetaDao.getDatabase(), book);
        fileMetaDao.detachAll();
    }

    private static void updateSidecarRevision(Database db, FileMeta book) {
        if (book.getSafSidecarRevision() == null) return;
        db.execSQL("UPDATE FILE_META SET SAF_SIDECAR_REVISION=? WHERE PATH=? "
                        + "AND SAF_SIDECAR_REVISION IS NOT ?",
                new Object[]{book.getSafSidecarRevision(), book.getPath(), book.getSafSidecarRevision()});
    }

    /** Unscanned picker/Recents books have no known sidecars; allow ordinary cover caching. */
    private void initializeUnscannedSidecarRevision(FileMeta book) {
        if (!ExtUtils.isExteralSD(book.getPath()) || book.getSafSidecarRevision() != null) return;
        try (Cursor rows = fileMetaDao.getDatabase().rawQuery(
                "SELECT 1 FROM SCAN_MEMBERSHIP WHERE PATH=? LIMIT 1", new String[]{book.getPath()})) {
            if (!rows.moveToFirst()) book.setSafSidecarRevision("");
        }
    }

    public void updateAnnotationIfMissing(String path, String annotation) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null || annotation == null || annotation.isEmpty()) return;
        dao.getDatabase().execSQL("UPDATE FILE_META SET ANNOTATION=? WHERE PATH=? "
                        + "AND (ANNOTATION IS NULL OR ANNOTATION='')",
                new Object[]{annotation, path});
        dao.detachAll();
    }

    /** Detached rows, so discovery does not hold mutable DAO identity-cache objects. */
    public List<FileMeta> scanSnapshot() {
        FileMetaDao dao = fileMetaDao;
        List<FileMeta> result = new ArrayList<>();
        if (dao == null) return result;
        try (Cursor cursor = dao.getDatabase().rawQuery("SELECT * FROM FILE_META", null)) {
            while (cursor.moveToNext()) result.add(dao.readEntity(cursor, 0));
        }
        return result;
    }

    private static boolean underRoot(String path, Set<String> roots) {
        for (String root : roots) {
            if (path.equals(root) || path.startsWith(root.endsWith("/") ? root : root + "/")) return true;
        }
        return false;
    }

    /** Drop only membership of roots deliberately removed from the folder selection. */
    public void reconcileDeselectedRoots(Set<String> selectedRoots, Set<String> explicitRemovals) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null) return;
        Database db = dao.getDatabase();
        Set<String> removedRoots = new HashSet<>();
        Set<String> affected = new HashSet<>();
        Set<String> selectedLocal = new HashSet<>();
        for (String root : selectedRoots) if (!ExtUtils.isExteralSD(root)) selectedLocal.add(root);
        db.beginTransaction();
        try {
            try (Cursor cursor = db.rawQuery("SELECT ROOT,PATH FROM SCAN_MEMBERSHIP", null)) {
                while (cursor.moveToNext()) {
                    String root = cursor.getString(0);
                    if (!selectedRoots.contains(root)) {
                        removedRoots.add(root);
                        affected.add(cursor.getString(1));
                    }
                }
            }
            for (String root : explicitRemovals) {
                if (selectedRoots.contains(root) || ExtUtils.isExteralSD(root)) continue;
                // Legacy local scans had no membership records; the removed root itself
                // is the only reliable scope for clearing those old scan flags.
                try (Cursor cursor = db.rawQuery(
                        "SELECT PATH FROM FILE_META WHERE IS_SEARCH_BOOK=1", null)) {
                    while (cursor.moveToNext()) {
                        String path = cursor.getString(0);
                        if (!ExtUtils.isExteralSD(path)
                                && underRoot(path, Collections.singleton(root))) affected.add(path);
                    }
                }
            }
            for (String root : removedRoots) {
                db.execSQL("DELETE FROM SCAN_MEMBERSHIP WHERE ROOT=?", new Object[]{root});
            }
            for (String path : affected) {
                if (!ExtUtils.isExteralSD(path) && underRoot(path, selectedLocal)) continue;
                try (Cursor cursor = db.rawQuery(
                        "SELECT 1 FROM SCAN_MEMBERSHIP WHERE PATH=? LIMIT 1",
                        new String[]{path})) {
                    if (!cursor.moveToFirst()) {
                        db.execSQL("UPDATE FILE_META SET IS_SEARCH_BOOK=0 WHERE PATH=?",
                                new Object[]{path});
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            dao.detachAll();
        }
    }

    /** A completed listing changes only scan-owned fields, not reading or favorite state. */
    public void publishDiscoveredBooks(String root, List<FileMeta> books) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null || books.isEmpty()) return;
        Database db = dao.getDatabase();
        db.beginTransaction();
        try {
            for (FileMeta book : books) {
                if (book.getPath() == null) continue;
                db.execSQL("INSERT OR IGNORE INTO FILE_META (PATH,TITLE,IS_SEARCH_BOOK) VALUES (?,?,?)",
                        new Object[]{book.getPath(), book.getTitle(), book.getIsSearchBook() ? 1 : 0});
                db.execSQL("UPDATE FILE_META SET IS_SEARCH_BOOK=?,"
                                + "SIZE=COALESCE(?,SIZE),DATE=COALESCE(?,DATE),"
                                + "PATH_TXT=COALESCE(?,PATH_TXT),EXT=COALESCE(?,EXT) WHERE PATH=?",
                        new Object[]{book.getIsSearchBook() ? 1 : 0, book.getSize(),
                                book.getDate(), book.getPathTxt(), book.getExt(), book.getPath()});
                updateSidecarRevision(db, book);
                db.execSQL("INSERT OR IGNORE INTO SCAN_MEMBERSHIP (ROOT,PATH) VALUES (?,?)",
                        new Object[]{root, book.getPath()});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            dao.detachAll();
        }
    }

    /** A completed listing changes only scan-owned fields, not reading or favorite state. */
    public void reconcileCompletedScan(List<FileMeta> found, Set<String> completeRoots) {
        reconcileCompletedScan(found, completeRoots, Collections.emptyMap());
    }

    /** Track roots explicitly: SAF IDs are opaque and local roots may overlap. */
    public void reconcileCompletedScan(List<FileMeta> found, Set<String> completeRoots,
                                       Map<String, Set<String>> safMembership) {
        reconcileCompletedScan(found, completeRoots, safMembership, Collections.emptySet());
    }

    public void reconcileCompletedScan(List<FileMeta> found, Set<String> completeRoots,
                                       Map<String, Set<String>> safMembership, Set<String> incompleteLocal) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null) return;
        org.greenrobot.greendao.database.Database db = dao.getDatabase();
        Set<String> seen = new HashSet<>();
        db.beginTransaction();
        try {
            for (FileMeta book : found) {
                String path = book.getPath();
                if (path == null || !seen.add(path)) continue;
                db.execSQL("INSERT OR IGNORE INTO FILE_META (PATH,TITLE,IS_SEARCH_BOOK) VALUES (?,?,?)",
                        new Object[]{path, book.getTitle(), book.getIsSearchBook() ? 1 : 0});
                db.execSQL("UPDATE FILE_META SET IS_SEARCH_BOOK=? WHERE PATH=?",
                        new Object[]{book.getIsSearchBook() ? 1 : 0, path});
                if (ExtUtils.isExteralSD(path)) {
                    updateSidecarRevision(db, book);
                    db.execSQL("UPDATE FILE_META SET STATE=COALESCE(STATE,?),"
                                    + "SIZE=COALESCE(?,SIZE),"
                                    + "DATE=COALESCE(?,DATE),PATH_TXT=COALESCE(?,PATH_TXT),"
                                    + "EXT=COALESCE(?,EXT) WHERE PATH=?",
                            new Object[]{FileMetaCore.STATE_BASIC, book.getSize(), book.getDate(),
                                    book.getPathTxt(), book.getExt(), path});
                }
            }
            try (Cursor cursor = db.rawQuery(
                    "SELECT PATH FROM FILE_META WHERE IS_SEARCH_BOOK=1", null)) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(0);
                    if (!seen.contains(path) && !ExtUtils.isExteralSD(path)
                            && underRoot(path, completeRoots) && !underRoot(path, incompleteLocal)) {
                        db.execSQL("UPDATE FILE_META SET IS_SEARCH_BOOK=0 WHERE PATH=?",
                                new Object[]{path});
                    }
                }
            }
            for (Map.Entry<String, Set<String>> root : safMembership.entrySet()) {
                Set<String> previous = new HashSet<>();
                try (Cursor cursor = db.rawQuery(
                        "SELECT PATH FROM SCAN_MEMBERSHIP WHERE ROOT=?",
                        new String[]{root.getKey()})) {
                    while (cursor.moveToNext()) previous.add(cursor.getString(0));
                }
                db.execSQL("DELETE FROM SCAN_MEMBERSHIP WHERE ROOT=?",
                        new Object[]{root.getKey()});
                for (String path : root.getValue()) {
                    db.execSQL("INSERT OR IGNORE INTO SCAN_MEMBERSHIP (ROOT,PATH) VALUES (?,?)",
                            new Object[]{root.getKey(), path});
                }
                for (String path : previous) {
                    if (seen.contains(path) || !ExtUtils.isExteralSD(path)) continue;
                    try (Cursor cursor = db.rawQuery(
                            "SELECT 1 FROM SCAN_MEMBERSHIP WHERE PATH=? LIMIT 1",
                            new String[]{path})) {
                        if (!cursor.moveToFirst()) {
                            db.execSQL("UPDATE FILE_META SET IS_SEARCH_BOOK=0 WHERE PATH=?",
                                    new Object[]{path});
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            dao.detachAll();
        }
    }

    /** Compare metadata to the scan-start row in SQL, preserving intervening user edits. */
    public void updateScannedMetadata(FileMeta extracted, FileMeta before) {
        updateScannedMetadata(extracted, before, true);
    }

    /** A failed content read can update verified file facts, but cannot erase known metadata. */
    public void updateScannedMetadata(FileMeta extracted, FileMeta before,
                                      boolean extractionSucceeded) {
        FileMetaDao dao = fileMetaDao;
        if (dao == null) return;
        String[] columns = {"TITLE", "AUTHOR", "SEQUENCE", "GENRE", "CHILD", "ANNOTATION",
                "S_INDEX", "EXT", "SIZE", "DATE", "DATE_TXT", "SIZE_TXT", "PATH_TXT",
                "LANG", "PAGES", "KEYWORD", "YEAR", "STATE", "PUBLISHER", "ISBN", "PARENT_PATH"};
        Set<String> basicMetadataColumns = new HashSet<>(java.util.Arrays.asList(
                "EXT", "SIZE", "DATE", "DATE_TXT", "SIZE_TXT", "PATH_TXT", "PARENT_PATH"));
        Object[] oldValues = metadataValues(before);
        Object[] newValues = metadataValues(extracted);
        StringBuilder sql = new StringBuilder("UPDATE FILE_META SET ");
        java.util.List<Object> arguments = new java.util.ArrayList<>();
        for (int i = 0; i < columns.length; i++) {
            if (!extractionSucceeded && !basicMetadataColumns.contains(columns[i])) continue;
            if (!arguments.isEmpty()) sql.append(',');
            sql.append(columns[i]).append("=CASE WHEN ").append(columns[i])
                    .append(" IS ? THEN ? ELSE ").append(columns[i]).append(" END");
            arguments.add(oldValues[i]);
            arguments.add(newValues[i]);
        }
        sql.append(" WHERE PATH=?");
        arguments.add(extracted.getPath());
        dao.getDatabase().execSQL(sql.toString(), arguments.toArray());
        dao.detachAll();
    }

    private static Object[] metadataValues(FileMeta book) {
        return new Object[]{book.getTitle(), book.getAuthor(), book.getSequence(), book.getGenre(),
                book.getChild(), book.getAnnotation(), book.getSIndex(), book.getExt(),
                book.getSize(), book.getDate(), book.getDateTxt(), book.getSizeTxt(), book.getPathTxt(),
                book.getLang(), book.getPages(), book.getKeyword(), book.getYear(), book.getState(),
                book.getPublisher(), book.getIsbn(), book.getParentPath()};
    }

    public FileMeta getOrCreate(String path) {
        path = migrateSafIdentity(path);
        if (fileMetaDao == null) {
            FileMeta fileMeta = new FileMeta(path);
            fileMeta.setPages(200);
            return fileMeta;
        }
        FileMeta load = null;
        try {
            load = fileMetaDao.load(path);


            if (load == null) {
                load = new FileMeta(path);
                fileMetaDao.insert(load);

            }
        } catch (Exception e) {
            LOG.e(e);
        }
        if (load == null) {
            load = new FileMeta(path);
            load.setPages(100);
        }

        if (fileMetaDao != null && load.getSafSidecarRevision() == null) {
            initializeUnscannedSidecarRevision(load);
            if (load.getSafSidecarRevision() != null) updateSidecarRevision(load, load.getSafSidecarRevision());
        }

        if (load.getState() == null) {
            load.setState(FileMetaCore.STATE_NONE);
        }

        return load;
    }

    /** Merge old grant-bearing rows into the grant-neutral document key on first use. */
    public synchronized String migrateSafIdentity(String path) {
        if (!ExtUtils.isExteralSD(path)) return path;
        String identity = SafDocumentIdentity.canonical(Uri.parse(path)).toString();
        if (fileMetaDao == null) return identity;
        List<String> aliases = new ArrayList<>();
        try (Cursor cursor = fileMetaDao.getDatabase().rawQuery(
                "SELECT PATH FROM FILE_META WHERE PATH LIKE 'content:%'", null)) {
            while (cursor.moveToNext()) {
                String candidate = cursor.getString(0);
                if (!identity.equals(candidate) && identity.equals(
                        SafDocumentIdentity.canonical(Uri.parse(candidate)).toString())) {
                    aliases.add(candidate);
                }
            }
        }
        if (aliases.isEmpty()) return identity;
        Database db = fileMetaDao.getDatabase();
        db.beginTransaction();
        try {
            for (String alias : aliases) {
                migrateSafAliasRow(db, alias, identity);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            fileMetaDao.detachAll();
        }
        return identity;
    }

    /** Migrate all legacy grant-bearing rows before scan snapshots or membership decisions. */
    public synchronized void migrateAllSafRows() {
        if (fileMetaDao == null) return;
        List<String[]> aliases = new ArrayList<>();
        try (Cursor cursor = fileMetaDao.getDatabase().rawQuery(
                "SELECT PATH FROM FILE_META WHERE PATH LIKE 'content:%'", null)) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(0);
                String identity = SafDocumentIdentity.canonical(Uri.parse(path)).toString();
                if (!path.equals(identity)) aliases.add(new String[]{path, identity});
            }
        }
        if (aliases.isEmpty()) return;
        Database db = fileMetaDao.getDatabase();
        db.beginTransaction();
        try {
            for (String[] alias : aliases) migrateSafAliasRow(db, alias[0], alias[1]);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            fileMetaDao.detachAll();
        }
    }

    private void migrateSafAliasRow(Database db, String alias, String identity) {
        FileMeta old = fileMetaDao.load(alias);
        if (old == null) return;
        FileMeta current = fileMetaDao.load(identity);
        if (current == null) {
            fileMetaDao.deleteByKey(alias);
            old.setPath(identity);
            fileMetaDao.insert(old);
        } else {
            mergeSafAlias(current, old);
            fileMetaDao.update(current);
            fileMetaDao.deleteByKey(alias);
        }
        db.execSQL("INSERT OR IGNORE INTO SCAN_MEMBERSHIP(ROOT,PATH) "
                        + "SELECT ROOT,? FROM SCAN_MEMBERSHIP WHERE PATH=?",
                new Object[]{identity, alias});
        db.execSQL("DELETE FROM SCAN_MEMBERSHIP WHERE PATH=?", new Object[]{alias});
        fileMetaDao.detachAll();
    }

    private static void mergeSafAlias(FileMeta current, FileMeta old) {
        if (Boolean.TRUE.equals(old.getIsStar())) current.setIsStar(true);
        if (old.getIsStarTime() != null && (current.getIsStarTime() == null
                || old.getIsStarTime() > current.getIsStarTime()))
            current.setIsStarTime(old.getIsStarTime());
        if (Boolean.TRUE.equals(old.getIsRecent())) current.setIsRecent(true);
        if (old.getIsRecentTime() != null && (current.getIsRecentTime() == null
                || old.getIsRecentTime() > current.getIsRecentTime())) {
            current.setIsRecentTime(old.getIsRecentTime());
            current.setIsRecentProgress(old.getIsRecentProgress());
        } else if (current.getIsRecentProgress() == null) {
            current.setIsRecentProgress(old.getIsRecentProgress());
        }
        if (Boolean.TRUE.equals(old.getIsSearchBook())) current.setIsSearchBook(true);
        if (current.getTag() == null) current.setTag(old.getTag());
        if (current.getCusType() == null) current.setCusType(old.getCusType());
        if (current.getAnnotation() == null) current.setAnnotation(old.getAnnotation());
        if (current.getSafSidecarRevision() == null) current.setSafSidecarRevision(old.getSafSidecarRevision());
        if (current.getState() == null || old.getState() != null
                && old.getState() > current.getState()) {
            current.setTitle(old.getTitle());
            current.setAuthor(old.getAuthor());
            current.setSequence(old.getSequence());
            current.setGenre(old.getGenre());
            current.setChild(old.getChild());
            current.setSIndex(old.getSIndex());
            current.setExt(old.getExt());
            current.setSize(old.getSize());
            current.setDate(old.getDate());
            current.setDateTxt(old.getDateTxt());
            current.setSizeTxt(old.getSizeTxt());
            current.setPathTxt(old.getPathTxt());
            current.setLang(old.getLang());
            current.setPages(old.getPages());
            current.setKeyword(old.getKeyword());
            current.setYear(old.getYear());
            current.setPublisher(old.getPublisher());
            current.setIsbn(old.getIsbn());
            current.setParentPath(old.getParentPath());
            current.setState(old.getState());
        }
    }

    public void clearSession() {
        try {
            daoSession.clear();
            currentDB = null;
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void saveAll(List<FileMeta> list) {
        if (fileMetaDao == null) {
            return;
        }

        long time = System.currentTimeMillis();
        LOG.d("Save all begin");
        for (FileMeta book : list) initializeUnscannedSidecarRevision(book);
        fileMetaDao.insertOrReplaceInTx(list, true);
        long end = System.currentTimeMillis() - time;
        LOG.d("Save all end", end / 1000, list.size());
    }

    public void updateAll(List<FileMeta> list) {
        if (fileMetaDao == null) {
            return;
        }

        try {
            if (fileMetaDao != null) {
                long time = System.currentTimeMillis();
                LOG.d("udpdate all begin");
                fileMetaDao.updateInTx(list);
                long end = System.currentTimeMillis() - time;
                LOG.d("update all end", end / 1000, list.size());
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public List<String> getAll(SEARCH_IN in) {
        String SQL_DISTINCT_ENAME = "SELECT DISTINCT " + in.getProperty().columnName + " as c FROM " + FileMetaDao.TABLENAME + " WHERE " + FileMetaDao.Properties.IsSearchBook.columnName + " == 1";

        ArrayList<String> result = new ArrayList<String>();
        Cursor c = daoSession.getDatabase().rawQuery(SQL_DISTINCT_ENAME, null);
        try {
            if (c.moveToFirst()) {
                do {
                    String item = c.getString(0);
                    if (item == null || TxtUtils.isEmpty(item)) {
                        continue;
                    }
                    if (in == SEARCH_IN.TAGS) {
                        TxtUtils.addFilteredTags(item, result);
                    } else {
                        TxtUtils.addFilteredGenreSeries(item, result, false);
                    }
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public List<FileMeta> getStarsFilesDeprecated() {
        QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
        List<FileMeta> list = where.where(FileMetaDao.Properties.IsStar.eq(1), where.or(FileMetaDao.Properties.CusType.isNull(), FileMetaDao.Properties.CusType.eq(FileMetaAdapter.DISPLAY_TYPE_FILE))).orderDesc(FileMetaDao.Properties.IsStarTime).list();
        return removeNotExist(list);
    }

    public List<FileMeta> getStarsFoldersDeprecated() {
        return fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsStar.eq(1), FileMetaDao.Properties.CusType.eq(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY)).orderAsc(FileMetaDao.Properties.PathTxt).list();
    }



    public boolean isStarFolderByFiles(String path) {
        final List<FileMeta> folders = AppData.get()
                                              .getAllFavoriteFolders();
        return TxtUtils.isListNotEmpty(folders) && folders.contains(new FileMeta(path));

    }
    public boolean isStarFolder(String path) {
        try {
            FileMeta load = fileMetaDao.load(path);
            if (load == null) {
                return false;
            }
            return load != null && load.getIsStar();
        } catch (Exception e) {
            return false;
        }
    }

    public void clearAllRecent() {
        if (fileMetaDao == null) {
            return;
        }
        List<FileMeta> recent = getRecentDeprecated();
        for (FileMeta meta : recent) {
            meta.setIsRecent(false);
        }
        fileMetaDao.updateInTx(recent);

    }

    public void clearAllFavorites() {
        if (fileMetaDao == null) {
            return;
        }
        List<FileMeta> recent = getRecentDeprecated();
        for (FileMeta meta : recent) {
            meta.setIsStar(false);
        }
        fileMetaDao.updateInTx(recent);

    }

    public void clearAllStars() {
        if (fileMetaDao == null) {
            return;
        }
        List<FileMeta> stars = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsStar.eq(1)).list();
        for (FileMeta meta : stars) {
            meta.setIsStar(false);
        }
        fileMetaDao.updateInTx(stars);
    }

    public List<FileMeta> getAllWithTag(String tagName) {
        LOG.d("getAllWithTag", tagName);
        try {
            QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
            where = where.where(SEARCH_IN.TAGS.getProperty().like("%" + tagName + StringDB.DIVIDER + "%"), FileMetaDao.Properties.IsSearchBook.eq(1));
            //where = where.where(SEARCH_IN.TAGS.getProperty().like("%" + tagName + StringDB.DIVIDER + "%"));
            List<FileMeta> list = where.list();
            ExtUtils.removeNotFound(list);
            return list;
        } catch (Exception e) {
            return new ArrayList<FileMeta>();
        }
    }

    public List<FileMeta> getAllWithTag() {
        if (fileMetaDao == null || fileMetaDao.queryBuilder() == null) {
            return new ArrayList<FileMeta>();
        }
        QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
        where = where.where(FileMetaDao.Properties.Tag.isNotNull(), FileMetaDao.Properties.Tag.notEq(""));
        try {
            return where.list() == null ? new ArrayList<FileMeta>() : where.list();
        } catch (Exception e) {
            return new ArrayList<FileMeta>();
        }

    }

    public List<FileMeta> getAllWithProgress() {
        if (fileMetaDao == null || fileMetaDao.queryBuilder() == null) {
            return new ArrayList<FileMeta>();
        }
        QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
        where = where.where(FileMetaDao.Properties.IsRecentProgress.isNotNull(), FileMetaDao.Properties.IsRecentProgress.eq(1.0f));
        try {
            return where.list() == null ? new ArrayList<FileMeta>() : where.list();
        } catch (Exception e) {
            return new ArrayList<FileMeta>();
        }

    }


    public List<FileMeta> searchBy(String str, SORT_BY sortby, boolean isAsc) {
        LOG.d("searchBy", str);
        try {
            QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
            where.preferLocalizedStringOrder();

            SEARCH_IN searchIn = null;
            for (SEARCH_IN in : SEARCH_IN.values()) {
                if (str.startsWith(in.getDotPrefix())) {
                    str = str.replace(in.getDotPrefix(), "").trim();

                    if (in == SEARCH_IN.LANGUAGES) {
                        str = str.substring(str.indexOf("(") + 1).replace(")", "").trim();
                    }

                    searchIn = in;
                    break;
                }
            }

            if (searchIn == SEARCH_IN.TAGS) {
                str = str + StringDB.DIVIDER;

            }
            LOG.d("searchBy", searchIn, str, "-");
            if (str.startsWith(SearchFragment2.EMPTY_ID)) {
                where = where.whereOr(searchIn.getProperty().like(""), searchIn.getProperty().isNull());
            } else {
                if (TxtUtils.isNotEmpty(str)) {
                    str = str.replace(" ", "%").replace("*", "%");
                    str = str.replace(StringDB.EXACTMATCHCHAR, StringDB.DIVIDER);

                    String string = "%" + str + "%";


                    LOG.d("searchBy-final", string);

                    if (searchIn != null) {
                        where = where.whereOr(searchIn.getProperty().like(string), searchIn.getProperty().like(string.toLowerCase(Locale.US)));
                    } else {
                        where = where.whereOr(//
                                FileMetaDao.Properties.PathTxt.like(string), //
                                FileMetaDao.Properties.Title.like(string), //
                                FileMetaDao.Properties.Author.like(string)//
                        );
                    }
                }
            }
            where = where.where(FileMetaDao.Properties.IsSearchBook.eq(1));

            if (sortby == SORT_BY.RECENT_TIME) {
                where = where.where(FileMetaDao.Properties.IsRecentTime.ge(1));
            }


            if (isAsc) {
                where = where.orderAsc(sortby.getProperty());
            } else {
                where = where.orderDesc(sortby.getProperty());
            }
            if (sortby == SORT_BY.SERIES) {
                where = where.orderAsc(FileMetaDao.Properties.SIndex);
            }


            if (sortby != SORT_BY.TITLE) {
                where = where.orderAsc(SORT_BY.TITLE.getProperty());
            }


            return where.list();

        } catch (Exception e) {
            LOG.e(e);
            return new ArrayList<FileMeta>();
        }
    }

    public  enum SEARCH_IN {
        //
        PATH(FileMetaDao.Properties.Path, -1), //
        SERIES(FileMetaDao.Properties.Sequence, AppState.MODE_SERIES), //
        GENRE(FileMetaDao.Properties.Genre, AppState.MODE_GENRE), //
        AUTHOR(FileMetaDao.Properties.Author, AppState.MODE_AUTHORS), //
        TAGS(FileMetaDao.Properties.Tag, AppState.MODE_USER_TAGS), //
        KEYWRODS(FileMetaDao.Properties.Keyword, AppState.MODE_KEYWORDS), //
        LANGUAGES(FileMetaDao.Properties.Lang, AppState.MODE_LANGUAGES),
        YEAR(FileMetaDao.Properties.Year, AppState.MODE_PUBLICATION_DATE),
        PUBLISHER(FileMetaDao.Properties.Publisher, AppState.MODE_PUBLISHER);
        // ANNOT(FileMetaDao.Properties.Annotation, -1); //
        // REGEX(FileMetaDao.Properties.Path, -1);//
        //
        private final Property property;
        private final int mode;

        private SEARCH_IN(Property property, int mode) {
            this.property = property;
            this.mode = mode;
        }

        public static SEARCH_IN getByMode(int index) {
            for (SEARCH_IN sortBy : values()) {
                if (sortBy.getMode() == index) {
                    return sortBy;
                }
            }
            return SEARCH_IN.AUTHOR;
        }

        public static SEARCH_IN getByPrefix(String string) {
            for (SEARCH_IN sortBy : values()) {
                if (string.startsWith(sortBy.getDotPrefix())) {
                    return sortBy;
                }
            }
            return SEARCH_IN.PATH;
        }

        public Property getProperty() {
            return property;
        }

        public String getDotPrefix() {
            return "@" + name().toLowerCase(Locale.US);
        }

        public int getMode() {
            return mode;
        }
    }

    public enum SORT_BY {
        //
        // In the order the sort menu lists them; the index is what is kept in the settings.
        DATA(3, R.string.by_date, FileMetaDao.Properties.Date), //
        PUBLICATION_YEAR(11, R.string.publication_date, FileMetaDao.Properties.PubDate),//
        SIZE(2, R.string.by_size, FileMetaDao.Properties.Size), //
        PATH(0, R.string.folder, FileMetaDao.Properties.ParentPath), //
        FILE_NAME(1, R.string.by_file_name, FileMetaDao.Properties.PathTxt), //
        TITLE(4, R.string.by_title, FileMetaDao.Properties.Title), //
        AUTHOR(5, R.string.by_author, FileMetaDao.Properties.Author), //
        SERIES(6, R.string.by_series, FileMetaDao.Properties.Sequence), //
        SERIES_INDEX(7, R.string.by_number_in_serie, FileMetaDao.Properties.SIndex), //
        PAGES(8, R.string.by_number_of_pages, FileMetaDao.Properties.Pages), //
        EXT(9, R.string.by_extension, FileMetaDao.Properties.Ext), //
        LANGUAGE(10, R.string.language, FileMetaDao.Properties.Lang),//
        PUBLISHER(12, R.string.publisher, FileMetaDao.Properties.Publisher),//
        RECENT_TIME(13, R.string.recent, FileMetaDao.Properties.IsRecentTime);//


        private final int index;
        private final int resName;
        private final Property property;

        private SORT_BY(int index, int resName, Property property) {
            this.index = index;
            this.resName = resName;
            this.property = property;
        }

        public static SORT_BY getByID(int index) {
            for (SORT_BY sortBy : values()) {
                if (sortBy.getIndex() == index) {
                    return sortBy;
                }
            }
            return SORT_BY.PATH;

        }

        public int getIndex() {
            return index;
        }

        public int getResName() {
            return resName;
        }

        public Property getProperty() {
            return property;
        }

    }

}
