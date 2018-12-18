package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RandomAccessFileInputStream extends InputStream {

	private final RandomAccessFile file;

	public RandomAccessFileInputStream(RandomAccessFile file) {
		this.file = file;
	}

	public RandomAccessFile getFile() {
		return file;
	}

	@Override
	public int read() throws IOException {
		return file.read();
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return file.read(buffer);
	}

	public int read(byte[] buffer, int byteOffset, int byteCount)
			throws IOException {
		return file.read(buffer, byteOffset, byteCount);
	}

}