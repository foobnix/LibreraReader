package com.foobnix.ui2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.ebookdroid.common.settings.SettingsManager;
import org.ebookdroid.common.settings.books.BookSettings;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.QueryBuilder;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.DaoMaster;
import com.foobnix.dao2.DaoSession;
import com.foobnix.dao2.FileMeta;
import com.foobnix.dao2.FileMetaDao;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AppDB {

    private static final String DB_NAME = "all-5";

    private final static AppDB in = new AppDB();

    public enum SEARCH_IN {
        //
        PATH(FileMetaDao.Properties.Path, -1), //
        SERIES(FileMetaDao.Properties.Sequence, AppState.MODE_SERIES), //
        GENRE(FileMetaDao.Properties.Genre, AppState.MODE_GENRE), //
        AUTHOR(FileMetaDao.Properties.Author, AppState.MODE_AUTHORS), //
        ANNOT(FileMetaDao.Properties.Annotation, -1); //
        // REGEX(FileMetaDao.Properties.Path, -1);//
        //
        private final Property property;
        private final int mode;

        private SEARCH_IN(Property property, int mode) {
            this.property = property;
            this.mode = mode;
        }

        public Property getProperty() {
            return property;
        }

        public String getDotPrefix() {
            return "@" + name().toLowerCase(Locale.US);
        }

        public static SEARCH_IN getByMode(int index) {
            for (SEARCH_IN sortBy : values()) {
                if (sortBy.getMode() == index) {
                    return sortBy;
                }
            }
            return SEARCH_IN.AUTHOR;
        }

        public int getMode() {
            return mode;
        }
    }

    public enum SORT_BY {
        //
        PATH(0, R.string.by_path, FileMetaDao.Properties.Path), //
        FILE_NAME(1, R.string.by_file_name, FileMetaDao.Properties.PathTxt), //
        SIZE(2, R.string.by_size, FileMetaDao.Properties.Size), //
        DATA(3, R.string.by_date, FileMetaDao.Properties.Date), //
        TITLE(4, R.string.by_title, FileMetaDao.Properties.Title), //
        AUTHOR(5, R.string.by_author, FileMetaDao.Properties.Author), //
        SERIES(6, R.string.by_series, FileMetaDao.Properties.Sequence), //
        SERIES_INDEX(7, R.string.by_number, FileMetaDao.Properties.SIndex);//

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

    public static AppDB get() {
        return in;
    }

    private FileMetaDao fileMetaDao;
    private DaoSession daoSession;

    public void open(Context c) {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(c, DB_NAME, null);
        SQLiteDatabase writableDatabase = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(writableDatabase);

        daoSession = daoMaster.newSession();

        fileMetaDao = daoSession.getFileMetaDao();

        if (c.getResources().getBoolean(R.bool.is_log_enable)) {
            QueryBuilder.LOG_SQL = true;
            QueryBuilder.LOG_VALUES = true;
        }
    }

    public List<FileMeta> deleteAllSafe() {
        List<FileMeta> list = fileMetaDao.queryBuilder().whereOr(FileMetaDao.Properties.IsStar.eq(1), FileMetaDao.Properties.IsRecent.eq(1)).list();
        if (list == null) {
            list = new ArrayList<FileMeta>();
        }
        fileMetaDao.deleteAll();
        return list;
    }

    public void delete(FileMeta meta) {
        fileMetaDao.delete(meta);
    }

    public List<FileMeta> getRecent() {
        return fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).list();
    }

    public FileMeta getRecentLast() {
        List<FileMeta> list = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).limit(1).list();
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public List<FileMeta> getAllRecentWithProgress() {
        List<FileMeta> list = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsRecent.eq(1)).orderDesc(FileMetaDao.Properties.IsRecentTime).list();
        for (FileMeta meta : list) {
            BookSettings bs = SettingsManager.getTempBookSettings(meta.getPath());
            try {
                meta.setIsRecentProgress((float) (bs.currentPage.viewIndex + 1) / bs.getPages());
            } catch (Exception e) {
                meta.setIsRecentProgress(1f);
            }
        }

        return list;
    }

    public void addRecent(String path) {
        if (!AppState.getInstance().isShowRecent) {
            return;
        }

        if (!new File(path).isFile()) {
            LOG.d("Can't add to recent, it's not a file", path);
            return;
        }
        LOG.d("Add Recent", path);
        FileMeta load = getOrCreate(path);
        load.setIsRecent(true);
        load.setIsRecentTime(System.currentTimeMillis());
        fileMetaDao.update(load);
    }

    public void save(FileMeta meta) {
        fileMetaDao.insertOrReplace(meta);
    }

    public long getCount() {
        return fileMetaDao.queryBuilder().count();
    }

    public FileMeta getOrCreate(String path) {
        FileMeta load = fileMetaDao.load(path);
        try {
            if (load == null) {
                load = new FileMeta(path);
                fileMetaDao.insert(load);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return load;
    }

    public void update(FileMeta meta) {
        fileMetaDao.update(meta);
    }

    public void updateOrSave(FileMeta meta) {
        if (fileMetaDao.load(meta.getPath()) == null) {
            fileMetaDao.insert(meta);
        } else {
            fileMetaDao.update(meta);
        }

    }

    public void saveAll(List<FileMeta> list) {
        long time = System.currentTimeMillis();
        LOG.d("Save all begin");
        fileMetaDao.insertOrReplaceInTx(list, true);
        long end = System.currentTimeMillis() - time;
        LOG.d("Save all end", end / 1000, list.size());
    }

    public void updateAll(List<FileMeta> list) {
        long time = System.currentTimeMillis();
        LOG.d("udpdate all begin");
        fileMetaDao.updateInTx(list);
        long end = System.currentTimeMillis() - time;
        LOG.d("update all end", end / 1000, list.size());
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
                    if (item.contains(",") || item.contains(";")) {
                        String[] split = item.split("[,;]");
                        for (String txt : split) {
                            if (TxtUtils.isNotEmpty(txt)) {
                                txt = txt.trim();
                                if (!result.contains(txt)) {
                                    result.add(txt);
                                }
                            }
                        }
                    } else {
                        result.add(item.trim());
                    }
                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public List<String> getAll(boolean isAsc) {
        QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.Author.notEq(""));

        if (isAsc) {
            where = where.orderAsc(FileMetaDao.Properties.Author);
        } else {
            where = where.orderDesc(FileMetaDao.Properties.Author);
        }
        List<String> result = new ArrayList<String>();
        for (FileMeta meta : where.list()) {
            // if (!result.contains(meta.getAuthor()) &&
            // TxtUtils.isNotEmpty(meta.getAuthor())) {
            result.add(meta.getAuthor());
            // }
        }
        return result;
    }

    public List<FileMeta> getStarsFiles() {
        QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();
        return where.where(FileMetaDao.Properties.IsStar.eq(1), where.or(FileMetaDao.Properties.CusType.isNull(), FileMetaDao.Properties.CusType.eq(FileMetaAdapter.DISPLAY_TYPE_FILE))).orderDesc(FileMetaDao.Properties.IsStarTime).list();
    }

    public List<FileMeta> getStarsFolder() {
        return fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsStar.eq(1), FileMetaDao.Properties.CusType.eq(FileMetaAdapter.DISPLAY_TYPE_DIRECTORY)).orderAsc(FileMetaDao.Properties.PathTxt).list();
    }

    public boolean isStarFolder(String path) {
        FileMeta load = fileMetaDao.load(path);
        if (load == null) {
            return false;
        }
        return load != null && load.getIsStar();
    }

    public void clearAllRecent() {
        List<FileMeta> recent = getRecent();
        for (FileMeta meta : recent) {
            meta.setIsRecent(false);
        }
        fileMetaDao.updateInTx(recent);

    }

    public void clearAllStars() {
        List<FileMeta> stars = fileMetaDao.queryBuilder().where(FileMetaDao.Properties.IsStar.eq(1)).list();
        for (FileMeta meta : stars) {
            meta.setIsStar(false);
        }
        fileMetaDao.updateInTx(stars);
    }

    public List<FileMeta> searchBy(String str, SORT_BY sortby, boolean isAsc) {
        try {
            QueryBuilder<FileMeta> where = fileMetaDao.queryBuilder();

            SEARCH_IN searchIn = null;
            for (SEARCH_IN in : SEARCH_IN.values()) {
                if (str.startsWith(in.getDotPrefix())) {
                    str = str.replace(in.getDotPrefix(), "").trim();
                    searchIn = in;
                    break;
                }
            }

            if (TxtUtils.isNotEmpty(str)) {
                str = str.replace(" ", "%");
                String string = "%" + str + "%";
                if (searchIn != null) {
                    where = where.where(searchIn.getProperty().like(string));
                } else {
                    where = where.whereOr(//
                            FileMetaDao.Properties.PathTxt.like(string), //
                            FileMetaDao.Properties.Title.like(string), //
                            FileMetaDao.Properties.Author.like(string)//
                    );
                }
            }
            where = where.where(FileMetaDao.Properties.IsSearchBook.eq(1));

            if (isAsc) {
                return where.orderAsc(sortby.getProperty()).list();
            } else {
                return where.orderDesc(sortby.getProperty()).list();
            }

        } catch (Exception e) {
            LOG.e(e);
            return new ArrayList<FileMeta>();
        }
    }

}
