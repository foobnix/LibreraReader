package com.foobnix.dao2;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "FILE_META".
*/
public class FileMetaDao extends AbstractDao<FileMeta, String> {

    public static final String TABLENAME = "FILE_META";

    /**
     * Properties of entity FileMeta.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Path = new Property(0, String.class, "path", true, "PATH");
        public final static Property Title = new Property(1, String.class, "title", false, "TITLE");
        public final static Property Author = new Property(2, String.class, "author", false, "AUTHOR");
        public final static Property Sequence = new Property(3, String.class, "sequence", false, "SEQUENCE");
        public final static Property Genre = new Property(4, String.class, "genre", false, "GENRE");
        public final static Property Child = new Property(5, String.class, "child", false, "CHILD");
        public final static Property Annotation = new Property(6, String.class, "annotation", false, "ANNOTATION");
        public final static Property SIndex = new Property(7, Integer.class, "sIndex", false, "S_INDEX");
        public final static Property CusType = new Property(8, Integer.class, "cusType", false, "CUS_TYPE");
        public final static Property Ext = new Property(9, String.class, "ext", false, "EXT");
        public final static Property Size = new Property(10, Long.class, "size", false, "SIZE");
        public final static Property Date = new Property(11, Long.class, "date", false, "DATE");
        public final static Property DateTxt = new Property(12, String.class, "dateTxt", false, "DATE_TXT");
        public final static Property SizeTxt = new Property(13, String.class, "sizeTxt", false, "SIZE_TXT");
        public final static Property PathTxt = new Property(14, String.class, "pathTxt", false, "PATH_TXT");
        public final static Property IsStar = new Property(15, Boolean.class, "isStar", false, "IS_STAR");
        public final static Property IsStarTime = new Property(16, Long.class, "isStarTime", false, "IS_STAR_TIME");
        public final static Property IsRecent = new Property(17, Boolean.class, "isRecent", false, "IS_RECENT");
        public final static Property IsRecentTime = new Property(18, Long.class, "isRecentTime", false, "IS_RECENT_TIME");
        public final static Property IsRecentProgress = new Property(19, Float.class, "isRecentProgress", false, "IS_RECENT_PROGRESS");
        public final static Property IsSearchBook = new Property(20, Boolean.class, "isSearchBook", false, "IS_SEARCH_BOOK");
        public final static Property Lang = new Property(21, String.class, "lang", false, "LANG");
        public final static Property Tag = new Property(22, String.class, "tag", false, "TAG");
        public final static Property Pages = new Property(23, Integer.class, "pages", false, "PAGES");
        public final static Property Keyword = new Property(24, String.class, "keyword", false, "KEYWORD");
        public final static Property Year = new Property(25, Integer.class, "year", false, "YEAR");
        public final static Property State = new Property(26, Integer.class, "state", false, "STATE");
        public final static Property Publisher = new Property(27, String.class, "publisher", false, "PUBLISHER");
        public final static Property Isbn = new Property(28, String.class, "isbn", false, "ISBN");
        public final static Property ParentPath = new Property(29, String.class, "parentPath", false, "PARENT_PATH");
        public final static Property FilesCount = new Property(30, Integer.class, "filesCount", false, "FILES_COUNT");
        public final static Property ReadCount = new Property(31, Integer.class, "readCount", false, "READ_COUNT");
    }


    public FileMetaDao(DaoConfig config) {
        super(config);
    }
    
    public FileMetaDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"FILE_META\" (" + //
                "\"PATH\" TEXT PRIMARY KEY NOT NULL ," + // 0: path
                "\"TITLE\" TEXT," + // 1: title
                "\"AUTHOR\" TEXT," + // 2: author
                "\"SEQUENCE\" TEXT," + // 3: sequence
                "\"GENRE\" TEXT," + // 4: genre
                "\"CHILD\" TEXT," + // 5: child
                "\"ANNOTATION\" TEXT," + // 6: annotation
                "\"S_INDEX\" INTEGER," + // 7: sIndex
                "\"CUS_TYPE\" INTEGER," + // 8: cusType
                "\"EXT\" TEXT," + // 9: ext
                "\"SIZE\" INTEGER," + // 10: size
                "\"DATE\" INTEGER," + // 11: date
                "\"DATE_TXT\" TEXT," + // 12: dateTxt
                "\"SIZE_TXT\" TEXT," + // 13: sizeTxt
                "\"PATH_TXT\" TEXT," + // 14: pathTxt
                "\"IS_STAR\" INTEGER," + // 15: isStar
                "\"IS_STAR_TIME\" INTEGER," + // 16: isStarTime
                "\"IS_RECENT\" INTEGER," + // 17: isRecent
                "\"IS_RECENT_TIME\" INTEGER," + // 18: isRecentTime
                "\"IS_RECENT_PROGRESS\" REAL," + // 19: isRecentProgress
                "\"IS_SEARCH_BOOK\" INTEGER," + // 20: isSearchBook
                "\"LANG\" TEXT," + // 21: lang
                "\"TAG\" TEXT," + // 22: tag
                "\"PAGES\" INTEGER," + // 23: pages
                "\"KEYWORD\" TEXT," + // 24: keyword
                "\"YEAR\" INTEGER," + // 25: year
                "\"STATE\" INTEGER," + // 26: state
                "\"PUBLISHER\" TEXT," + // 27: publisher
                "\"ISBN\" TEXT," + // 28: isbn
                "\"PARENT_PATH\" TEXT," + // 29: parentPath
                "\"FILES_COUNT\" INTEGER," + // 30: filesCount
                "\"READ_COUNT\" INTEGER);"); // 31: readCount
        // Add Indexes
        db.execSQL("CREATE UNIQUE INDEX " + constraint + "path_asc ON FILE_META" +
                " (\"PATH\" ASC);");
        db.execSQL("CREATE UNIQUE INDEX " + constraint + "path_desc ON FILE_META" +
                " (\"PATH\" DESC);");
        db.execSQL("CREATE INDEX " + constraint + "title_asc ON FILE_META" +
                " (\"TITLE\" ASC);");
        db.execSQL("CREATE INDEX " + constraint + "title_desc ON FILE_META" +
                " (\"TITLE\" DESC);");
        db.execSQL("CREATE INDEX " + constraint + "author_asc ON FILE_META" +
                " (\"AUTHOR\" ASC);");
        db.execSQL("CREATE INDEX " + constraint + "author_desc ON FILE_META" +
                " (\"AUTHOR\" DESC);");
        db.execSQL("CREATE INDEX " + constraint + "size_asc ON FILE_META" +
                " (\"SIZE\" ASC);");
        db.execSQL("CREATE INDEX " + constraint + "size_desc ON FILE_META" +
                " (\"SIZE\" DESC);");
        db.execSQL("CREATE INDEX " + constraint + "date_asc ON FILE_META" +
                " (\"DATE\" ASC);");
        db.execSQL("CREATE INDEX " + constraint + "date_desc ON FILE_META" +
                " (\"DATE\" DESC);");
        db.execSQL("CREATE INDEX " + constraint + "pathTxt_asc ON FILE_META" +
                " (\"PATH_TXT\" ASC);");
        db.execSQL("CREATE INDEX " + constraint + "pathTxt_desc ON FILE_META" +
                " (\"PATH_TXT\" DESC);");
        db.execSQL("CREATE INDEX " + constraint + "parentPath_asc ON FILE_META" +
                " (\"PARENT_PATH\" ASC);");
        db.execSQL("CREATE INDEX " + constraint + "parentPath_desc ON FILE_META" +
                " (\"PARENT_PATH\" DESC);");
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"FILE_META\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, FileMeta entity) {
        stmt.clearBindings();
 
        String path = entity.getPath();
        if (path != null) {
            stmt.bindString(1, path);
        }
 
        String title = entity.getTitle();
        if (title != null) {
            stmt.bindString(2, title);
        }
 
        String author = entity.getAuthor();
        if (author != null) {
            stmt.bindString(3, author);
        }
 
        String sequence = entity.getSequence();
        if (sequence != null) {
            stmt.bindString(4, sequence);
        }
 
        String genre = entity.getGenre();
        if (genre != null) {
            stmt.bindString(5, genre);
        }
 
        String child = entity.getChild();
        if (child != null) {
            stmt.bindString(6, child);
        }
 
        String annotation = entity.getAnnotation();
        if (annotation != null) {
            stmt.bindString(7, annotation);
        }
 
        Integer sIndex = entity.getSIndex();
        if (sIndex != null) {
            stmt.bindLong(8, sIndex);
        }
 
        Integer cusType = entity.getCusType();
        if (cusType != null) {
            stmt.bindLong(9, cusType);
        }
 
        String ext = entity.getExt();
        if (ext != null) {
            stmt.bindString(10, ext);
        }
 
        Long size = entity.getSize();
        if (size != null) {
            stmt.bindLong(11, size);
        }
 
        Long date = entity.getDate();
        if (date != null) {
            stmt.bindLong(12, date);
        }
 
        String dateTxt = entity.getDateTxt();
        if (dateTxt != null) {
            stmt.bindString(13, dateTxt);
        }
 
        String sizeTxt = entity.getSizeTxt();
        if (sizeTxt != null) {
            stmt.bindString(14, sizeTxt);
        }
 
        String pathTxt = entity.getPathTxt();
        if (pathTxt != null) {
            stmt.bindString(15, pathTxt);
        }
 
        Boolean isStar = entity.getIsStar();
        if (isStar != null) {
            stmt.bindLong(16, isStar ? 1L: 0L);
        }
 
        Long isStarTime = entity.getIsStarTime();
        if (isStarTime != null) {
            stmt.bindLong(17, isStarTime);
        }
 
        Boolean isRecent = entity.getIsRecent();
        if (isRecent != null) {
            stmt.bindLong(18, isRecent ? 1L: 0L);
        }
 
        Long isRecentTime = entity.getIsRecentTime();
        if (isRecentTime != null) {
            stmt.bindLong(19, isRecentTime);
        }
 
        Float isRecentProgress = entity.getIsRecentProgress();
        if (isRecentProgress != null) {
            stmt.bindDouble(20, isRecentProgress);
        }
 
        Boolean isSearchBook = entity.getIsSearchBook();
        if (isSearchBook != null) {
            stmt.bindLong(21, isSearchBook ? 1L: 0L);
        }
 
        String lang = entity.getLang();
        if (lang != null) {
            stmt.bindString(22, lang);
        }
 
        String tag = entity.getTag();
        if (tag != null) {
            stmt.bindString(23, tag);
        }
 
        Integer pages = entity.getPages();
        if (pages != null) {
            stmt.bindLong(24, pages);
        }
 
        String keyword = entity.getKeyword();
        if (keyword != null) {
            stmt.bindString(25, keyword);
        }
 
        Integer year = entity.getYear();
        if (year != null) {
            stmt.bindLong(26, year);
        }
 
        Integer state = entity.getState();
        if (state != null) {
            stmt.bindLong(27, state);
        }
 
        String publisher = entity.getPublisher();
        if (publisher != null) {
            stmt.bindString(28, publisher);
        }
 
        String isbn = entity.getIsbn();
        if (isbn != null) {
            stmt.bindString(29, isbn);
        }
 
        String parentPath = entity.getParentPath();
        if (parentPath != null) {
            stmt.bindString(30, parentPath);
        }
 
        Integer filesCount = entity.getFilesCount();
        if (filesCount != null) {
            stmt.bindLong(31, filesCount);
        }
 
        Integer readCount = entity.getReadCount();
        if (readCount != null) {
            stmt.bindLong(32, readCount);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, FileMeta entity) {
        stmt.clearBindings();
 
        String path = entity.getPath();
        if (path != null) {
            stmt.bindString(1, path);
        }
 
        String title = entity.getTitle();
        if (title != null) {
            stmt.bindString(2, title);
        }
 
        String author = entity.getAuthor();
        if (author != null) {
            stmt.bindString(3, author);
        }
 
        String sequence = entity.getSequence();
        if (sequence != null) {
            stmt.bindString(4, sequence);
        }
 
        String genre = entity.getGenre();
        if (genre != null) {
            stmt.bindString(5, genre);
        }
 
        String child = entity.getChild();
        if (child != null) {
            stmt.bindString(6, child);
        }
 
        String annotation = entity.getAnnotation();
        if (annotation != null) {
            stmt.bindString(7, annotation);
        }
 
        Integer sIndex = entity.getSIndex();
        if (sIndex != null) {
            stmt.bindLong(8, sIndex);
        }
 
        Integer cusType = entity.getCusType();
        if (cusType != null) {
            stmt.bindLong(9, cusType);
        }
 
        String ext = entity.getExt();
        if (ext != null) {
            stmt.bindString(10, ext);
        }
 
        Long size = entity.getSize();
        if (size != null) {
            stmt.bindLong(11, size);
        }
 
        Long date = entity.getDate();
        if (date != null) {
            stmt.bindLong(12, date);
        }
 
        String dateTxt = entity.getDateTxt();
        if (dateTxt != null) {
            stmt.bindString(13, dateTxt);
        }
 
        String sizeTxt = entity.getSizeTxt();
        if (sizeTxt != null) {
            stmt.bindString(14, sizeTxt);
        }
 
        String pathTxt = entity.getPathTxt();
        if (pathTxt != null) {
            stmt.bindString(15, pathTxt);
        }
 
        Boolean isStar = entity.getIsStar();
        if (isStar != null) {
            stmt.bindLong(16, isStar ? 1L: 0L);
        }
 
        Long isStarTime = entity.getIsStarTime();
        if (isStarTime != null) {
            stmt.bindLong(17, isStarTime);
        }
 
        Boolean isRecent = entity.getIsRecent();
        if (isRecent != null) {
            stmt.bindLong(18, isRecent ? 1L: 0L);
        }
 
        Long isRecentTime = entity.getIsRecentTime();
        if (isRecentTime != null) {
            stmt.bindLong(19, isRecentTime);
        }
 
        Float isRecentProgress = entity.getIsRecentProgress();
        if (isRecentProgress != null) {
            stmt.bindDouble(20, isRecentProgress);
        }
 
        Boolean isSearchBook = entity.getIsSearchBook();
        if (isSearchBook != null) {
            stmt.bindLong(21, isSearchBook ? 1L: 0L);
        }
 
        String lang = entity.getLang();
        if (lang != null) {
            stmt.bindString(22, lang);
        }
 
        String tag = entity.getTag();
        if (tag != null) {
            stmt.bindString(23, tag);
        }
 
        Integer pages = entity.getPages();
        if (pages != null) {
            stmt.bindLong(24, pages);
        }
 
        String keyword = entity.getKeyword();
        if (keyword != null) {
            stmt.bindString(25, keyword);
        }
 
        Integer year = entity.getYear();
        if (year != null) {
            stmt.bindLong(26, year);
        }
 
        Integer state = entity.getState();
        if (state != null) {
            stmt.bindLong(27, state);
        }
 
        String publisher = entity.getPublisher();
        if (publisher != null) {
            stmt.bindString(28, publisher);
        }
 
        String isbn = entity.getIsbn();
        if (isbn != null) {
            stmt.bindString(29, isbn);
        }
 
        String parentPath = entity.getParentPath();
        if (parentPath != null) {
            stmt.bindString(30, parentPath);
        }
 
        Integer filesCount = entity.getFilesCount();
        if (filesCount != null) {
            stmt.bindLong(31, filesCount);
        }
 
        Integer readCount = entity.getReadCount();
        if (readCount != null) {
            stmt.bindLong(32, readCount);
        }
    }

    @Override
    public String readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0);
    }    

    @Override
    public FileMeta readEntity(Cursor cursor, int offset) {
        FileMeta entity = new FileMeta( //
            cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0), // path
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // title
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // author
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // sequence
            cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4), // genre
            cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5), // child
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6), // annotation
            cursor.isNull(offset + 7) ? null : cursor.getInt(offset + 7), // sIndex
            cursor.isNull(offset + 8) ? null : cursor.getInt(offset + 8), // cusType
            cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9), // ext
            cursor.isNull(offset + 10) ? null : cursor.getLong(offset + 10), // size
            cursor.isNull(offset + 11) ? null : cursor.getLong(offset + 11), // date
            cursor.isNull(offset + 12) ? null : cursor.getString(offset + 12), // dateTxt
            cursor.isNull(offset + 13) ? null : cursor.getString(offset + 13), // sizeTxt
            cursor.isNull(offset + 14) ? null : cursor.getString(offset + 14), // pathTxt
            cursor.isNull(offset + 15) ? null : cursor.getShort(offset + 15) != 0, // isStar
            cursor.isNull(offset + 16) ? null : cursor.getLong(offset + 16), // isStarTime
            cursor.isNull(offset + 17) ? null : cursor.getShort(offset + 17) != 0, // isRecent
            cursor.isNull(offset + 18) ? null : cursor.getLong(offset + 18), // isRecentTime
            cursor.isNull(offset + 19) ? null : cursor.getFloat(offset + 19), // isRecentProgress
            cursor.isNull(offset + 20) ? null : cursor.getShort(offset + 20) != 0, // isSearchBook
            cursor.isNull(offset + 21) ? null : cursor.getString(offset + 21), // lang
            cursor.isNull(offset + 22) ? null : cursor.getString(offset + 22), // tag
            cursor.isNull(offset + 23) ? null : cursor.getInt(offset + 23), // pages
            cursor.isNull(offset + 24) ? null : cursor.getString(offset + 24), // keyword
            cursor.isNull(offset + 25) ? null : cursor.getInt(offset + 25), // year
            cursor.isNull(offset + 26) ? null : cursor.getInt(offset + 26), // state
            cursor.isNull(offset + 27) ? null : cursor.getString(offset + 27), // publisher
            cursor.isNull(offset + 28) ? null : cursor.getString(offset + 28), // isbn
            cursor.isNull(offset + 29) ? null : cursor.getString(offset + 29), // parentPath
            cursor.isNull(offset + 30) ? null : cursor.getInt(offset + 30), // filesCount
            cursor.isNull(offset + 31) ? null : cursor.getInt(offset + 31) // readCount
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, FileMeta entity, int offset) {
        entity.setPath(cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0));
        entity.setTitle(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setAuthor(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setSequence(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setGenre(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setChild(cursor.isNull(offset + 5) ? null : cursor.getString(offset + 5));
        entity.setAnnotation(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
        entity.setSIndex(cursor.isNull(offset + 7) ? null : cursor.getInt(offset + 7));
        entity.setCusType(cursor.isNull(offset + 8) ? null : cursor.getInt(offset + 8));
        entity.setExt(cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9));
        entity.setSize(cursor.isNull(offset + 10) ? null : cursor.getLong(offset + 10));
        entity.setDate(cursor.isNull(offset + 11) ? null : cursor.getLong(offset + 11));
        entity.setDateTxt(cursor.isNull(offset + 12) ? null : cursor.getString(offset + 12));
        entity.setSizeTxt(cursor.isNull(offset + 13) ? null : cursor.getString(offset + 13));
        entity.setPathTxt(cursor.isNull(offset + 14) ? null : cursor.getString(offset + 14));
        entity.setIsStar(cursor.isNull(offset + 15) ? null : cursor.getShort(offset + 15) != 0);
        entity.setIsStarTime(cursor.isNull(offset + 16) ? null : cursor.getLong(offset + 16));
        entity.setIsRecent(cursor.isNull(offset + 17) ? null : cursor.getShort(offset + 17) != 0);
        entity.setIsRecentTime(cursor.isNull(offset + 18) ? null : cursor.getLong(offset + 18));
        entity.setIsRecentProgress(cursor.isNull(offset + 19) ? null : cursor.getFloat(offset + 19));
        entity.setIsSearchBook(cursor.isNull(offset + 20) ? null : cursor.getShort(offset + 20) != 0);
        entity.setLang(cursor.isNull(offset + 21) ? null : cursor.getString(offset + 21));
        entity.setTag(cursor.isNull(offset + 22) ? null : cursor.getString(offset + 22));
        entity.setPages(cursor.isNull(offset + 23) ? null : cursor.getInt(offset + 23));
        entity.setKeyword(cursor.isNull(offset + 24) ? null : cursor.getString(offset + 24));
        entity.setYear(cursor.isNull(offset + 25) ? null : cursor.getInt(offset + 25));
        entity.setState(cursor.isNull(offset + 26) ? null : cursor.getInt(offset + 26));
        entity.setPublisher(cursor.isNull(offset + 27) ? null : cursor.getString(offset + 27));
        entity.setIsbn(cursor.isNull(offset + 28) ? null : cursor.getString(offset + 28));
        entity.setParentPath(cursor.isNull(offset + 29) ? null : cursor.getString(offset + 29));
        entity.setFilesCount(cursor.isNull(offset + 30) ? null : cursor.getInt(offset + 30));
        entity.setReadCount(cursor.isNull(offset + 31) ? null : cursor.getInt(offset + 31));
     }
    
    @Override
    protected final String updateKeyAfterInsert(FileMeta entity, long rowId) {
        return entity.getPath();
    }
    
    @Override
    public String getKey(FileMeta entity) {
        if(entity != null) {
            return entity.getPath();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(FileMeta entity) {
        return entity.getPath() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
