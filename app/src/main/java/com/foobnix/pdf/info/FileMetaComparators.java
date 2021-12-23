package com.foobnix.pdf.info;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.drive.GFile;
import com.foobnix.model.SimpleMeta;
import com.foobnix.ui2.adapter.FileMetaAdapter;

import java.io.File;
import java.util.Comparator;

public class FileMetaComparators {

    public static Comparator<FileMeta> BY_PATH = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getPath(), o2.getPath());
            } catch (Exception e) {
                LOG.e(e);
                return 0;
            }
        }
    };
    public static NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
    public static Comparator<FileMeta> BY_PATH_NUMBER = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return naturalOrderComparator.compare(o1.getPath().toLowerCase(), o2.getPath().toLowerCase());
            } catch (Exception e) {
                return BY_PATH.compare(o1, o2);
            }
        }

    };


    public static Comparator<File> BY_PATH_FILE = new Comparator<File>() {
        @Override
        public int compare(File o1, File o2) {
            try {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getPath(), o2.getPath());
            } catch (Exception e) {
                LOG.e(e);
                return 0;
            }
        }
    };

    public static Comparator<FileMeta> BY_DATE = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return compareLong(o1.getDate(), o2.getDate());
            } catch (Exception e) {
                return 0;
            }
        }
    };

    public static Comparator<FileMeta> BY_SYNC_DATE = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return compareLong(GFile.getLastModified(new File(o1.getPath())), GFile.getLastModified(new File(o2.getPath())));
            } catch (Exception e) {
                return 0;
            }
        }
    };

    public static Comparator<FileMeta> BY_RECENT_TIME = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return compareLong(o1.getIsRecentTime(), o2.getIsRecentTime());
            } catch (Exception e) {
                return 0;
            }
        }
    };

    public static Comparator<FileMeta> BY_SIZE = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return o1.getSize().compareTo(o2.getSize());
            } catch (Exception e) {
                LOG.e(e);
                return 0;
            }
        }
    };

    public static Comparator<FileMeta> BR_BY_EXT = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getExt(), o2.getExt());
            } catch (Exception e) {
                LOG.e(e);
                return 0;
            }
        }
    };


    public static Comparator<FileMeta> BR_BY_NUMBER1 = new Comparator<FileMeta>() {

        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            int g1 = o1.getSIndex() == null ? 0 : o1.getSIndex();
            int g2 = o2.getSIndex() == null ? 0 : o2.getSIndex();
            LOG.d("BR_BY_NUMBER1", g1, g2);
            return compareInt(g1, g2);
        }

    };

    public static Comparator<FileMeta> BR_BY_PAGES = new Comparator<FileMeta>() {

        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            int g1 = o1.getPages() == null ? 0 : o1.getPages();
            int g2 = o2.getPages() == null ? 0 : o2.getPages();
            LOG.d("BR_BY_PAGES", g1, g2);
            return compareInt(g1, g2);
        }
    };

    public static Comparator<FileMeta> BR_BY_TITLE = new Comparator<FileMeta>() {

        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                String t2 = TxtUtils.nullToEmpty(o1.getTitle());
                String t3 = TxtUtils.nullToEmpty(o2.getTitle());
                return String.CASE_INSENSITIVE_ORDER.compare(t2, t3);
            } catch (Exception e) {
                LOG.e(e);
                return 0;
            }
        }
    };
    public static Comparator<FileMeta> BR_BY_AUTHOR = new Comparator<FileMeta>() {

        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                String t2 = TxtUtils.nullToEmpty(o1.getAuthor());
                String t3 = TxtUtils.nullToEmpty(o2.getAuthor());
                return String.CASE_INSENSITIVE_ORDER.compare(t2, t3);
            } catch (Exception e) {
                LOG.e(e);
                return 0;
            }
        }
    };

    public static Comparator<FileMeta> DIRS = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            if (o1.getCusType() != null && o1.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY && o2.getCusType() == null)
                return -1;
            if (o2.getCusType() != null && o2.getCusType() == FileMetaAdapter.DISPLAY_TYPE_DIRECTORY && o1.getCusType() == null)
                return 1;

            return 0;
        }
    };


    public static Comparator<SimpleMeta> SIMPLE_META_BY_TIME = new Comparator<SimpleMeta>() {
        @Override
        public int compare(SimpleMeta o1, SimpleMeta o2) {
            return compareLong(o2.time, o1.time);
        }
    };

    public static int compareInt(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int compareLong(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
