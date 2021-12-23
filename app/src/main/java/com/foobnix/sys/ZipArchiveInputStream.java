package com.foobnix.sys;

import com.foobnix.android.utils.LOG;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static com.foobnix.pdf.info.FileMetaComparators.naturalOrderComparator;

public class ZipArchiveInputStream extends InputStream {

    public boolean isValid;
    // public static final Lock lock = new ReentrantLock();
    ZipInputStream zis;
    private Iterator<FileHeader> iterator;
    private FileHeader current;
    private ZipFile zp;
    private ZipInputStream inputStream;
    private File tempFile;

    public ZipArchiveInputStream(String file) {
        // CacheZipUtils.cacheLock.lock();
        try {


            zp = new ZipFile(file);

//            if (!zp.isValidZipFile()) {
//                zis = new ZipInputStream(new FileInputStream(file));
//                return;
//            }


            final List<FileHeader> fileHeaders = zp.getFileHeaders();

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

        } catch (Exception e) {
            LOG.e(e, file);
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
        if (zis != null) {
            try {
                zis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            zis = null;

        }
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (Exception e) {
                LOG.e(e);
            }
        }
    }

    public ArchiveEntry getNextEntry() {
        if (zis != null) {
            try {
                final LocalFileHeader nextEntry = zis.getNextEntry();
                return nextEntry != null ? new ArchiveEntry(nextEntry) : null;

            } catch (IOException e) {
                return null;
            }
        }

        if (iterator == null || !iterator.hasNext()) {
            return null;
        }
        closeStream();
        current = iterator.next();
        return current != null ? new ArchiveEntry(current) : null;
    }

    private void openStream() throws IOException {
        if (zis != null) {
            return;
        }

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
        if (zis != null) {
            return zis.read();
        }
        openStream();
        return inputStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (zis != null) {
            return zis.read(b);
        }
        openStream();
        return inputStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (zis != null) {
            return zis.read(b, off, len);
        }
        openStream();
        return inputStream.read(b, off, len);
    }

}
