package com.foobnix.sys;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.foobnix.android.utils.LOG;

public class UnclosableBufferedInputStream extends BufferedInputStream {

    public UnclosableBufferedInputStream(InputStream in) {
        super(in);
        super.mark(Integer.MAX_VALUE);
    }

    @Override
    public void close() throws IOException {
        super.reset();
    }

    public void closeClose() {
        try {
            super.close();
        } catch (Exception e) {
            LOG.e(e);
        }
    }
}