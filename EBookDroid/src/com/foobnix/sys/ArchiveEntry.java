package com.foobnix.sys;

import com.foobnix.android.utils.TxtUtils;

import net.lingala.zip4j.model.FileHeader;

public class ArchiveEntry {

    private FileHeader header;

    public ArchiveEntry(FileHeader header) {
        this.header = header;
    }

    public long getSize() {
        return header.getUncompressedSize();
    }

    public long getCompressedSize() {
        return header.getCompressedSize();
    }

    public boolean isDirectory() {
        return header.isDirectory();
    }

    public String getName() {
        return TxtUtils.encode1251(header.getFileName());
    }

}
