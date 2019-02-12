package com.foobnix.pdf.info.io;

import java.io.File;
import java.util.Comparator;

public class FileOrderComparator implements Comparator<File> {

	@Override
    public int compare(File arg0, File arg1) {
        if (arg0.isDirectory() && arg1.isFile()) {
			return -1;
        } else if (arg0.isFile() && arg1.isDirectory()) {
			return 1;
		};
        return arg0.getPath().compareTo(arg1.getPath());
	}
}
