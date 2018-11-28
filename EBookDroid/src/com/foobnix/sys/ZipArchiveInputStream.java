package com.foobnix.sys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.google.android.gms.common.util.IOUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

public class ZipArchiveInputStream extends InputStream {

    private Iterator<FileHeader> iterator;
    private FileHeader current;
    private ZipFile zp;
    private ZipInputStream inputStream;
    private File tempFile;
    private String encoding = "UTF-8";

    public ZipArchiveInputStream(String file) {
        try {
            zp = new ZipFile(file);
            iterator = zp.getFileHeaders().iterator();
        } catch (ZipException e) {
            LOG.e(e);
        }
    }

    public ZipArchiveInputStream(InputStream is, String encoding) {
        this.encoding = encoding;
        try {
            if (tempFile != null) {
                tempFile.delete();
            }
            tempFile = new File(CacheDir.ZipApp.getDir(), "temp.zip");

            LOG.d("zip-tempFile", tempFile.getPath());
            IOUtils.copyStream(is, new FileOutputStream(tempFile));

            zp = new ZipFile(tempFile);
            iterator = zp.getFileHeaders().iterator();
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (tempFile != null) {
            tempFile.delete();
        }
    }

    public ArchiveEntry getNextEntry() {
        try {
            if (iterator == null || !iterator.hasNext()) {
                return null;
            }
            current = iterator.next();
            inputStream = zp.getInputStream(current);
        } catch (ZipException e) {
            LOG.e(e);
            return null;
        }

        return current != null ? new ArchiveEntry(current, encoding) : null;
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

}
