package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileOutputStream extends OutputStream {

	private final RandomAccessFile file;

	public RandomAccessFileOutputStream(RandomAccessFile file) {
		this.file = file;
	}

	public RandomAccessFile getFile() {
		return file;
	}

	@Override
	public void write(int oneByte) throws IOException {
		file.write(oneByte);
	}

	@Override
	public void write(byte[] buffer) throws IOException {
		file.write(buffer);
	}

	@Override
	public void write(byte[] buffer, int offset, int count) throws IOException {
		file.write(buffer, offset, count);
	}

}