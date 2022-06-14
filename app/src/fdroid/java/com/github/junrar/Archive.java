package com.github.junrar;

import com.github.junrar.rarfile.FileHeader;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class Archive {
    public Archive(Object fileVolumeManager) {
    }

    public List<FileHeader> getFileHeaders() {
        return null;
    }

    public void close() {
    }

    public void extractFile(FileHeader fileHeader, ByteArrayOutputStream out) {
    }
}
