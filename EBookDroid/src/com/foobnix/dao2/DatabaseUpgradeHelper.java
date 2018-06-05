package com.foobnix.dao2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.greenrobot.greendao.database.Database;

import android.content.Context;
import android.util.Log;

public class DatabaseUpgradeHelper extends DaoMaster.OpenHelper {

    public DatabaseUpgradeHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.d("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion);
        List<Migration> migrations = getMigrations();

        for (Migration migration : migrations) {
            if (oldVersion < migration.getVersion()) {
                migration.runMigration(db);
                Log.d("greenDAO", "Upgrading schema run " + migration.getVersion());
            }
        }
    }

    private List<Migration> getMigrations() {
        List<Migration> migrations = new ArrayList<Migration>();
        migrations.add(new MigrationV3());
        migrations.add(new MigrationV4());
        migrations.add(new MigrationV5());
        migrations.add(new MigrationV6());
        migrations.add(new MigrationV7());

        Comparator<Migration> migrationComparator = new Comparator<Migration>() {
            @Override
            public int compare(Migration m1, Migration m2) {
                return m1.getVersion().compareTo(m2.getVersion());
            }
        };
        Collections.sort(migrations, migrationComparator);

        return migrations;
    }


    private static class MigrationV3 implements Migration {

        @Override
        public Integer getVersion() {
            return 3;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Lang.columnName + " TEXT");

        }
    }

    private static class MigrationV4 implements Migration {

        @Override
        public Integer getVersion() {
            return 4;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Tag.columnName + " TEXT");
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Pages.columnName + " INTEGER");
        }
    }

    private static class MigrationV5 implements Migration {

        @Override
        public Integer getVersion() {
            return 5;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Keyword.columnName + " TEXT");
        }
    }

    private static class MigrationV6 implements Migration {

        @Override
        public Integer getVersion() {
            return 6;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Year.columnName + " INTEGER");
        }
    }

    private static class MigrationV7 implements Migration {

        @Override
        public Integer getVersion() {
            return 7;
        }

        @Override
        public void runMigration(Database db) {
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.State.columnName + " INTEGER");
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Publisher.columnName + " TEXT");
            db.execSQL("ALTER TABLE " + FileMetaDao.TABLENAME + " ADD COLUMN " + FileMetaDao.Properties.Isbn.columnName + " TEXT");
        }
    }

    private interface Migration {
        Integer getVersion();

        void runMigration(Database db);
    }
}