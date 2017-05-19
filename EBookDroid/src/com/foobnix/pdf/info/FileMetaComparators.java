package com.foobnix.pdf.info;

import java.util.Comparator;

import com.foobnix.dao2.FileMeta;
import com.foobnix.ui2.adapter.FileMetaAdapter;

public class FileMetaComparators {

    public static Comparator<FileMeta> BY_PATH = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            return o1.getPath().compareTo(o2.getPath());
        }
    };

    public static Comparator<FileMeta> BY_DATE = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            return compareLong(o1.getDate(), o2.getDate());
        }
    };

    public static Comparator<FileMeta> BY_SIZE = new Comparator<FileMeta>() {
        @Override
        public int compare(FileMeta o1, FileMeta o2) {
            return o1.getSize().compareTo(o2.getSize());
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

    public static int compareLong(long x, long y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }

}
