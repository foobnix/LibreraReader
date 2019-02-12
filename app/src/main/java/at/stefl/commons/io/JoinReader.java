package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Arrays;

// TODO: improve
public class JoinReader extends Reader {

	private Reader[] ins;
	private int index;

	public JoinReader(Reader... ins) {
		this.ins = Arrays.copyOf(ins, ins.length);
	}

	public int getStreamIndex() {
		return index;
	}

	public int getStreamCount() {
		return ins.length;
	}

	public boolean isStreamAvailable() {
		return index < ins.length;
	}

	@Override
	public int read() throws IOException {
		int result;

		while (true) {
			if (!isStreamAvailable())
				return -1;
			result = ins[index].read();
			if (result != -1)
				return result;
			index++;
		}
	}

	@Override
	public int read(char[] cbuf) throws IOException {
		int result;

		while (true) {
			if (!isStreamAvailable())
				return -1;
			result = ins[index].read(cbuf);
			if (result != -1)
				return result;
			index++;
		}
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int result;

		while (true) {
			if (!isStreamAvailable())
				return -1;
			result = ins[index].read(cbuf, off, len);
			if (result != -1)
				return result;
			index++;
		}
	}

	@Override
	public int read(CharBuffer target) throws IOException {
		int result;

		while (true) {
			if (!isStreamAvailable())
				return -1;
			result = ins[index].read(target);
			if (result != -1)
				return result;
			index++;
		}
	}

	// TODO: improve
	@Override
	public long skip(long n) throws IOException {
		if (!isStreamAvailable())
			return 0;

		return CharStreamUtil.skipCharwise(this, n);
	}

	@Override
	public boolean ready() throws IOException {
		if (!isStreamAvailable())
			return false;

		return ins[index].ready();
	}

	// TODO: implement
	@Override
	public boolean markSupported() {
		return false;
	}

	// TODO: implement
	@Override
	public void mark(int readAheadLimit) throws IOException {
	}

	// TODO: implement
	@Override
	public void reset() throws IOException {
	}

	@Override
	public void close() throws IOException {
		IOException last = null;

		for (; index < ins.length; index++) {
			try {
				ins[index].close();
			} catch (IOException e) {
				last = e;
			}
		}

		if (last != null)
			throw last;
	}

}