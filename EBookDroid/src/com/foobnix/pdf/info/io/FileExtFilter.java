package com.foobnix.pdf.info.io;

import java.io.File;
import java.io.FileFilter;

public class FileExtFilter implements FileFilter {

    private final String[] exts;

    public FileExtFilter(String[] exts) {
        this.exts = exts;
    }

    @Override
    public boolean accept(File file) {
        if (exts == null) {
			return false;
        }
        for (String ext : exts) {
            if (file.getName().endsWith(ext)) {
                return true;
            }
        }
        return false;

    }

}