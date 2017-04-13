package com.foobnix.pdf.info;
import java.io.File;
import java.util.List;


public interface ServiceCommands {
	public void scan(List<File> result, Runnable run);
}
