package com.foobnix.ui2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
import com.foobnix.model.AppData;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.BuildConfig;
import com.foobnix.pdf.info.Clouds;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.ui2.adapter.FileMetaAdapter;
import com.foobnix.ui2.fragment.SearchFragment2;

import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AppDB {

    private final static AppDB in = new AppDB();
    DatabaseUpgradeHelper helper;
    String currentDB;
    private FileMetaDao fileMetaDao;
    private DaoSession daoSession;
    private DictMetaDao dictMetaDao;

    {
        //my custom method
        //return obj != null && path.equals(((FileMeta) obj).path);
        new FileMeta().equals(null);
    }


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

            if (!new File(next.getPath()).isFile()) {
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

        if (BuildConfig.DEBUG) {
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

        if (!new File(path).isFile()) {
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
        if (!new File(path).isFile()) {
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
        fileMetaDao.insertOrReplace(meta);
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

    public FileMeta load(String path) {
        if (fileMetaDao == null) {
            return null;
        }
        return fileMetaDao.load(path);
    }

    public FileMeta getOrCreate(String path) {
        if (fileMetaDao == null) {
            return new FileMeta(path);
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
        }

        if (load.getState() == null) {
            load.setState(FileMetaCore.STATE_NONE);
        }

        return load;
    }

    public void update(FileMeta meta) {
        if (fileMetaDao == null) {
            return;
        }
        try {
            fileMetaDao.update(meta);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public void refresh(FileMeta meta) {
        if (fileMetaDao == null) {
            return;
        }
        try {
            fileMetaDao.refresh(meta);
        } catch (Exception e) {
            LOG.e(e);
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

    public synchronized void updateOrSave(FileMeta meta) {
        if (fileMetaDao.load(meta.getPath()) == null) {
            fileMetaDao.insert(meta);
            //LOG.d("updateOrSave insert", LOG.ojectAsString(meta));
        } else {
            fileMetaDao.update(meta);
            //LOG.d("updateOrSave update", LOG.ojectAsString(meta));
        }

    }

    public void saveAll(List<FileMeta> list) {
        if (fileMetaDao == null) {
            return;
        }

        long time = System.currentTimeMillis();
        LOG.d("Save all begin");
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
            return where.list();
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
            if (sortby != SORT_BY.TITLE) {
                where = where.orderAsc(SORT_BY.TITLE.getProperty());
            }


            return where.list();

        } catch (Exception e) {
            LOG.e(e);
            return new ArrayList<FileMeta>();
        }
    }

    public enum SEARCH_IN {
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
        PATH(0, R.string.folder, FileMetaDao.Properties.ParentPath), //
        FILE_NAME(1, R.string.by_file_name, FileMetaDao.Properties.PathTxt), //
        SIZE(2, R.string.by_size, FileMetaDao.Properties.Size), //
        DATA(3, R.string.by_date, FileMetaDao.Properties.Date), //
        TITLE(4, R.string.by_title, FileMetaDao.Properties.Title), //
        AUTHOR(5, R.string.by_author, FileMetaDao.Properties.Author), //
        SERIES(6, R.string.by_series, FileMetaDao.Properties.Sequence), //
        SERIES_INDEX(7, R.string.by_number_in_serie, FileMetaDao.Properties.SIndex), //
        PAGES(8, R.string.by_number_of_pages, FileMetaDao.Properties.Pages), //
        EXT(9, R.string.by_extension, FileMetaDao.Properties.Ext), //
        LANGUAGE(10, R.string.language, FileMetaDao.Properties.Lang),//
        PUBLICATION_YEAR(11, R.string.publication_date, FileMetaDao.Properties.Year),//
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
