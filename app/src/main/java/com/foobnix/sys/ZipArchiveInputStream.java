package com.foobnix.sys;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.mobi.parser.IOUtils;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static com.foobnix.pdf.info.FileMetaComparators.naturalOrderComparator;

public class ZipArchiveInputStream extends InputStream {

    private Iterator<FileHeader> iterator;
    private FileHeader current;
    private ZipFile zp;
    private ZipInputStream inputStream;
    private File tempFile;

    // public static final Lock lock = new ReentrantLock();

    public ZipArchiveInputStream(String file) {
        // CacheZipUtils.cacheLock.lock();
        try {
            zp = new ZipFile(file);
            final List fileHeaders = zp.getFileHeaders();

            Collections.sort(fileHeaders, new Comparator<FileHeader>() {
                @Override
                public int compare(FileHeader o1, FileHeader o2) {
                    try {
                        return naturalOrderComparator.compare(o1.getFileName(), o2.getFileName());
                    } catch (Exception e) {
                        LOG.e(e);
                        return 0;
                    }
                }
            });

            iterator = fileHeaders.iterator();
            LOG.d("ZipArchiveInputStream", file);

        } catch (ZipException e) {
            LOG.e(e, file);
        }
    }

    public ZipArchiveInputStream(InputStream is) {
        // CacheZipUtils.cacheLock.lock();
        try {
            if (tempFile != null) {
                tempFile.delete();
            }
            tempFile = new File(CacheDir.ZipApp.getDir(), "temp.zip");

            LOG.d("zip-tempFile", tempFile.getPath());

            LOG.d("ZipArchiveInputStream", "InputStream", "zip-tempFile", tempFile.getPath());

            IOUtils.copyClose(is, new FileOutputStream(tempFile));


            zp = new ZipFile(tempFile);
            iterator = zp.getFileHeaders().iterator();


        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public void close() throws IOException {
        // CacheZipUtils.cacheLock.unlock();
        closeStream();
    }

    public void release() {
        if (tempFile != null) {
            tempFile.delete();
        }
        closeStream();
        if (zp != null) {
            zp = null;
        }
    }

    private void closeStream() {
        if (inputStream != null) {
            try {
                inputStream.close(true);
                inputStream = null;
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    public ArchiveEntry getNextEntry() {
        if (iterator == null || !iterator.hasNext()) {
            return null;
        }
        closeStream();
        current = iterator.next();
        return current != null ? new ArchiveEntry(current) : null;
    }

    private void openStream() throws IOException {

        if (inputStream == null) {
            LOG.d("openStream", zp);
            try {
                inputStream = zp.getInputStream(current);
            } catch (ZipException e) {
                throw new IOException();
            }
        }
    }

    @Override
    public int read() throws IOException {
        openStream();
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        openStream();
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        openStream();
        return inputStream.read(b, off, len);
    }

}
