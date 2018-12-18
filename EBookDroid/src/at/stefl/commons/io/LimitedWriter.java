package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

import at.stefl.commons.util.array.ArrayUtil;

public class LimitedWriter extends DelegationWriter {

	private long left;

	public LimitedWriter(Writer out, long limit) {
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
	public void write(int c) throws IOException {
		checkWrite(1);
		out.write(c);
		left--;
	}
	
	@Override
	public void write(char[] cbuf) throws IOException {
		write(cbuf, 0, cbuf.length);
	}
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		ArrayUtil.validateArguments(cbuf.length, off, len);
		checkWrite(len);
		out.write(cbuf, off, len);
		left -= len;
	}
	
	@Override
	public void write(String str) throws IOException {
		write(str, 0, str.length());
	}
	
	@Override
	public void write(String str, int off, int len) throws IOException {
		ArrayUtil.validateArguments(str.length(), off, len);
		checkWrite(len);
		out.write(str, off, len);
		left -= len;
	}

}