package com.foobnix.pdf.info;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.adapter.FileMetaAdapter;

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

    public static Comparator<FileMeta> BR_BY_NUMBER = new Comparator<FileMeta>() {
        Pattern compile = Pattern.compile("[0-9]+");

        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            try {
                Matcher m1 = compile.matcher(o1.getPathTxt());
                Matcher m2 = compile.matcher(o2.getPathTxt());
                if (m1.find() && m2.find()) {
                    int g1 = Integer.parseInt(m1.group(0));
                    int g2 = Integer.parseInt(m2.group(0));
                    return compareInt(g1, g2);
                }
            } catch (Exception e) {
                LOG.e(e);
            }

            return BY_PATH.compare(o1, o2);
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
                String t2 = o1.getTitle() == null ? "" : o1.getTitle();
                String t3 = o2.getTitle() == null ? "" : o2.getTitle();
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

    public static int compareInt(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

    public static int compareLong(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
