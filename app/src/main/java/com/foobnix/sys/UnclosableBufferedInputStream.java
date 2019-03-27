package com.foobnix.sys;

import com.foobnix.android.utils.LOG;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

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