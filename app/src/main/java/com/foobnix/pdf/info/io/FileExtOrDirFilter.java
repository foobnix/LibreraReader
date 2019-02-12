package com.foobnix.pdf.info.io;

import java.io.File;

public class FileExtOrDirFilter extends FileExtFilter {

    public FileExtOrDirFilter(String ext[]) {
        super(ext);
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }
        return super.accept(file);
    }

}
