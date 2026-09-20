package com.foobnix.work;

import com.foobnix.dao2.FileMeta;
import com.foobnix.model.SimpleMeta;
import java.util.List;
import java.util.function.BooleanSupplier;

/** The same search-membership decision for full and background scans. */
final class ScanMembership {
    private ScanMembership() {}

    static boolean apply(List<FileMeta> found, List<SimpleMeta> excluded,
                         List<FileMeta> synced, BooleanSupplier stopped) {
        for (FileMeta row : found) {
            if (stopped.getAsBoolean()) return false;
            row.setIsSearchBook(true);
            if (excluded.contains(SimpleMeta.SyncSimpleMeta(row.getPath())))
                row.setIsSearchBook(false);
            for (FileMeta other : synced) {
                if (row.getTitle() != null && row.getTitle().equals(other.getTitle())
                        && !row.getPath().equals(other.getPath())) {
                    row.setIsSearchBook(false);
                    break;
                }
            }
        }
        return true;
    }
}
