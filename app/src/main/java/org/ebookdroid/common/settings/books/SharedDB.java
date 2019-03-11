package org.ebookdroid.common.settings.books;

import android.content.Context;

import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;

public class SharedDB {

    public SharedDB(Context context) {
    }

    public void load() {

    }

    public synchronized BookSettings getBookSettings(String fileName) {
        BookSettings bs = new BookSettings(fileName);
        IO.readObj(bs.getCacheFile(), bs);
        LOG.d("SharedDB", "getBookSettings", bs.path);
        return bs;

    }


    public synchronized void storeBookSettings(BookSettings bs) {
        IO.writeObj(bs.getCacheFile(), bs);
        LOG.d("SharedDB", "storeBookSettings", bs.path);
    }

    public void delete(BookSettings bs) {
        if (bs == null) {
            return;
        }
        bs.getCacheFile().delete();
        LOG.d("SharedDB", "delete", bs.path);
    }


}
