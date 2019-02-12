package com.foobnix.pdf.info;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Locale;

public class ExtFilter implements FileFilter {

    private final List<String> exts;

    public ExtFilter(List<String> exts) {
			this.exts = exts;
		}
		@Override
        public boolean accept(File pathname) {
			for (String s : exts) {
			if (pathname.getName().toLowerCase(Locale.US).endsWith(s))
					return true;
			}
			return pathname.isDirectory();
		}
	};