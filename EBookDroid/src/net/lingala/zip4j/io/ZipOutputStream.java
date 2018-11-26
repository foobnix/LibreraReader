package net.lingala.zip4j.io;

import java.io.IOException;
import java.io.OutputStream;

import net.lingala.zip4j.model.ZipModel;

public class ZipOutputStream extends DeflaterOutputStream {
	
	public ZipOutputStream(OutputStream outputStream) {
		this(outputStream, null);
	}
	
	public ZipOutputStream(OutputStream outputStream, ZipModel zipModel) {
		super(outputStream, zipModel);
	}
	
	public void write(int bval) throws IOException {
		byte[] b = new byte[1];
		b[0] = (byte) bval;
		write(b, 0, 1);
	}
	
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		crc.update(b, off, len);
		updateTotalBytesRead(len);
		super.write(b, off, len);
	}
}
