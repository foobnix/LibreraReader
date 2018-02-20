package com.foobnix.dao2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.greenrobot.greendao.database.Database;

import android.content.Context;

public class DatabaseUpgradeHelper extends DaoMaster.OpenHelper {

    public DatabaseUpgradeHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        List<Migration> migrations = getMigrations();

        for (Migration migration : migrations) {
            if (oldVersion < migration.getVersion()) {
                migration.runMigration(db);
            }
        }
    }

    private List<Migration> getMigrations() {
        List<Migration> migrations = new ArrayList<Migration>();
        migrations.add(new MigrationV3());
        migrations.add(new MigrationV4());

        // Sorting just to be safe, in case other people add migrations in the wrong order.
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

    private interface Migration {
        Integer getVersion();

        void runMigration(Database db);
    }
}