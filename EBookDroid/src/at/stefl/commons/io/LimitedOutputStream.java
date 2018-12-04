package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

import at.stefl.commons.util.array.ArrayUtil;

public class LimitedOutputStream extends DelegationOutputStream {

	private long left;

	public LimitedOutputStream(OutputStream out, long limit) {
		super(out);

		if (limit < 0)
			throw new IllegalArgumentException("limit < 0");
		this.left = limit;
	}

	public long left() {
		return left;
	}

	private void checkWrite(int len) {
		if (left < len)
			throw new IllegalStateException("limit exceeded");
	}

	@Override
	public void write(int b) throws IOException {
		checkWrite(1);
		out.write(b);
		left--;
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		ArrayUtil.validateArguments(b.length, off, len);
		checkWrite(len);
		out.write(b, off, len);
		left -= len;
	}

}