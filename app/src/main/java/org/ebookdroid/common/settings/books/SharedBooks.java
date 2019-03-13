package org.ebookdroid.common.settings.books;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.ui2.AppDB;

import java.util.List;

public class SharedBooks {

    public static synchronized void updateProgress(List<FileMeta> list) {
        for (FileMeta meta : list) {
            if (AppBook.getCacheFile(meta.getPath()).isFile()) {
                AppBook book = load(meta.getPath());
                meta.setIsRecentProgress(book.p);
                LOG.d("SharedBooks-updateProgress", meta.getPath(), book.p);
            }
        }
        AppDB.get().updateAll(list);
    }

    public static synchronized AppBook load(String fileName) {
        AppBook bs = new AppBook(fileName);
        IO.readObj(bs.getCacheFile(), bs);
        LOG.d("SharedBooks", "load", bs.path);
        return bs;
    }

    public static synchronized void save(AppBook bs) {
        IO.writeObjAsync(bs.getCacheFile(), bs);
        LOG.d("SharedBooks-save", bs.path);
    }

    public static void delete(AppBook bs) {
        if (bs == null) {
            return;
        }
        bs.getCacheFile().delete();
        LOG.d("SharedBooks", "delete", bs.path);
    }


}
