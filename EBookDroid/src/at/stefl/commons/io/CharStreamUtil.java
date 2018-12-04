package at.stefl.commons.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.stefl.commons.util.collection.CharArrayQueue;
import at.stefl.commons.util.string.CharSequenceUtil;

// TODO: kill StringBuilder
public class CharStreamUtil {
	
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	
	public static int readTireless(Reader in, char[] cbuf) throws IOException {
		if (cbuf.length == 0)
			return 0;

		int result;
		int read;

		for (result = 0; result < cbuf.length; result += read) {
			read = in.read(cbuf, result, cbuf.length - result);
			if (read == -1)
				break;
		}

		return (result == 0) ? -1 : result;
	}
	
	public static int readTireless(Reader in, char[] cbuf, int off, int len)
			throws IOException {
		if (cbuf.length == 0)
			return 0;

		int result;
		int read;

		for (result = 0; result < len; result += read) {
			read = in.read(cbuf, off + result, len - result);
			if (read == -1)
				break;
		}

		return (result == 0) ? -1 : result;
	}

	public static int readTireless(Reader in, CharBuffer target)
			throws IOException {
		int len = target.remaining();
		if (len == 0)
			return 0;

		int result;
		int read;

		for (result = 0; result < len; result += read) {
			read = in.read(target);
			if (read == -1)
				break;
		}

		return (result == 0) ? -1 : result;
	}

	public static int readCharwise(Reader in, char[] cbuf) throws IOException {
		if (cbuf.length == 0)
			return 0;

		int result;
		int read;

		for (result = 0; result < cbuf.length; result++) {
			read = in.read();
			if (read == -1)
				break;

			cbuf[result] = (char) read;
		}

		return (result == 0) ? -1 : result;
	}

	public static int readCharwise(Reader in, char[] cbuf, int off, int len)
			throws IOException {
		int result;
		int read;

		for (result = 0; result < cbuf.length; result++) {
			read = in.read();
			if (read == -1)
				break;

			cbuf[off + result] = (char) read;
		}

		return (result == 0) ? -1 : result;
	}

	public static int readCharwise(Reader in, CharBuffer target)
			throws IOException {
		int len = target.remaining();
		if (len == 0)
			return 0;

		int result;
		int read;

		for (result = 0; result < len; result++) {
			read = in.read();
			if (read == -1)
				break;

			target.put((char) read);
		}

		return (result == 0) ? -1 : result;
	}

	public static char readFully(Reader in) throws IOException {
		int read = in.read();
		if (read == -1)
			throw new EOFException();
		return (char) read;
	}

	public static char[] readFully(Reader in, int len) throws IOException {
		char[] cbuf = new char[len];
		int read = readFully(in, cbuf);
		if (read < len)
			throw new EOFException();
		return cbuf;
	}

	public static int readFully(Reader in, char[] cbuf) throws IOException {
		int read = readTireless(in, cbuf);
		if (read < cbuf.length)
			throw new EOFException();
		return read;
	}

	public static int readFully(Reader in, char[] cbuf, int off, int len)
			throws IOException {
		int read = readTireless(in, cbuf, off, len);
		if (read < len)
			throw new EOFException();
		return read;
	}

	public static int readFully(Reader in, CharBuffer target)
			throws IOException {
		int remaining = target.remaining();
		int read = readTireless(in, target);
		if (read < remaining)
			throw new EOFException();
		return read;
	}

	public static char[] readChars(Reader in) throws IOException {
		DividedCharArrayWriter out = new DividedCharArrayWriter();
		out.write(in);
		out.close();
		return out.toCharArray();
	}

	public static String readString(Reader in) throws IOException {
		DividedCharArrayWriter out = new DividedCharArrayWriter();
		out.write(in);
		out.close();
		return out.toString();
	}

	public static String readLine(PushbackReader in) throws IOException {
		@SuppressWarnings("resource")
		DividedCharArrayWriter out = new DividedCharArrayWriter();
		int read;

		while (true) {
			read = in.read();

			switch (read) {
			case '\r':
				skipIfChar(in, '\n');
			case '\n':
			case -1:
				return out.isEmpty() ? null : out.toString();
			default:
				out.write(read);
			}
		}
	}

	public static String readUntilChar(Reader in, char c) throws IOException {
		@SuppressWarnings("resource")
		DividedCharArrayWriter out = new DividedCharArrayWriter();
		int read;

		while (true) {
			read = in.read();
			if (read == c)
				return out.toString();
			if (read == -1)
				throw new EOFException();

			out.write((char) read);
		}
	}

	public static String readUntilChar(Reader in, Set<Character> chars)
			throws IOException {
		@SuppressWarnings("resource")
		DividedCharArrayWriter out = new DividedCharArrayWriter();
		int read;

		while (true) {
			read = in.read();
			if (chars.contains((char) read))
				return out.toString();
			if (read == -1)
				throw new EOFException();

			out.write((char) read);
		}
	}

	// TODO: improve
	public static String readUntilString(Reader in, String string)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		int read;

		while ((read = in.read()) != -1) {
			builder.append((char) read);
			if ((builder.length() >= string.length())
					&& CharSequenceUtil.endsWith(builder, string))
				return builder.substring(0, builder.length() - string.length());
		}

		throw new EOFException();
	}

	public static void writeCharwise(Writer out, char[] cbuf)
			throws IOException {
		for (int i = 0; i < cbuf.length; i++) {
			out.write(cbuf[i]);
		}
	}

	public static void writeCharwise(Writer out, char[] cbuf, int off)
			throws IOException {
		for (int i = off; i < cbuf.length; i++) {
			out.write(cbuf[i]);
		}
	}

	public static void writeCharwise(Writer out, char[] cbuf, int off, int len)
			throws IOException {
		int end = off + len;

		for (int i = off; i < end; i++) {
			out.write(cbuf[i]);
		}
	}

	public static void writeCharwise(Writer out, String str) throws IOException {
		for (int i = 0; i < str.length(); i++) {
			out.write(str.charAt(i));
		}
	}

	public static void writeCharwise(Writer out, String str, int off)
			throws IOException {
		for (int i = off; i < str.length(); i++) {
			out.write(str.charAt(i));
		}
	}

	public static void writeCharwise(Writer out, String str, int off, int len)
			throws IOException {
		int end = off + len;

		for (int i = off; i < end; i++) {
			out.write(str.charAt(i));
		}
	}

	public static void appendCharwise(Writer out, CharSequence csq)
			throws IOException {
		if (csq == null)
			csq = CharSequenceUtil.NULL;

		int len = csq.length();

		for (int i = 0; i < len; i++) {
			out.append(csq.charAt(i));
		}
	}

	public static void appendCharwise(Writer out, CharSequence csq, int start)
			throws IOException {
		if (csq == null)
			csq = CharSequenceUtil.NULL;

		int len = csq.length();

		for (int i = start; i < len; i++) {
			out.append(csq.charAt(i));
		}
	}

	public static void appendCharwise(Writer out, CharSequence csq, int start,
			int end) throws IOException {
		if (csq == null) {
			csq = CharSequenceUtil.NULL;
			start = 0;
			end = CharSequenceUtil.NULL.length();
		}

		for (int i = start; i < end; i++) {
			out.append(csq.charAt(i));
		}
	}

	public static void writeStreamCharwise(Reader in, Writer out)
			throws IOException {
		for (int read; (read = in.read()) != -1;)
			out.write(read);
	}

	public static int writeStreamCharwiseLimited(Reader in, Writer out, int len)
			throws IOException {
		int read;
		int count = 0;

		while (true) {
			read = in.read();
			if (read == -1)
				return count;

			out.write(read);
			count++;
		}
	}

	public static int writeStreamBuffered(Reader in, Writer out)
			throws IOException {
		return writeStreamBuffered(in, out, DEFAULT_BUFFER_SIZE);
	}

	public static int writeStreamBuffered(Reader in, Writer out, int bufferSize)
			throws IOException {
		char[] cbuf = new char[bufferSize];
		return writeStreamBuffered(in, out, cbuf);
	}

	public static int writeStreamBuffered(Reader in, Writer out, char[] cbuf)
			throws IOException {
		int read;
		int count = 0;

		while (true) {
			read = in.read(cbuf);
			if (read == -1)
				return count;

			out.write(cbuf, 0, read);
			count += read;
		}
	}

	public static void flushCharwise(Reader in) throws IOException {
		while (in.read() != -1)
			;
	}

	public static void flushBuffered(Reader in) throws IOException {
		flushBuffered(in, DEFAULT_BUFFER_SIZE);
	}

	public static void flushBuffered(Reader in, int bufferSize)
			throws IOException {
		char[] cbuf = new char[bufferSize];
		while (in.read(cbuf, 0, bufferSize) != -1)
			;
	}

	public static int flushBytewiseCount(Reader in) throws IOException {
		int result = 0;
		while (in.read() != -1)
			result++;
		return result;
	}

	public static int flushBufferedCount(Reader in) throws IOException {
		return flushBufferedCount(in, DEFAULT_BUFFER_SIZE);
	}

	public static int flushBufferedCount(Reader in, int bufferSize)
			throws IOException {
		int result = 0;
		int read;
		char[] cbuf = new char[bufferSize];
		while ((read = in.read(cbuf, 0, bufferSize)) != -1)
			result += read;
		return result;
	}

	public static void flushLine(PushbackReader in) throws IOException {
		while (true) {
			switch (in.read()) {
			case '\r':
				skipIfChar(in, '\n');
			case '\n':
			case -1:
				return;
			default:
				break;
			}
		}
	}

	public static int flushWhitespace(Reader in) throws IOException {
		int read;

		while (true) {
			read = in.read();
			if (!Character.isWhitespace(read))
				break;
			if (read == -1)
				break;
		}

		return read;
	}

	public static void flushWhitespace(PushbackReader in) throws IOException {
		int read = flushWhitespace((Reader) in);
		if (read != -1)
			in.unread(read);
	}

	public static int flushChars(Reader in, char c) throws IOException {
		int read;

		while (true) {
			read = in.read();
			if (read != c)
				break;
			if (read == -1)
				break;
		}

		return read;
	}

	public static void flushChars(PushbackReader in, char c) throws IOException {
		int read = flushChars((Reader) in, c);
		if (read != -1)
			in.unread(read);
	}

	public static int flushUntilFilter(Reader in, CharFilter filter)
			throws IOException {
		int read;

		while (true) {
			read = in.read();
			if (filter.accept((char) read))
				break;
			if (read == -1)
				break;
		}

		return read;
	}

	public static void flushUntilFilter(PushbackReader in, CharFilter filter)
			throws IOException {
		int read = flushUntilFilter((Reader) in, filter);
		if (read == -1)
			throw new EOFException();
		in.unread(read);
	}

	public static void flushUntilChar(Reader in, char c) throws IOException {
		int read;

		while (true) {
			read = in.read();
			if (read == -1)
				throw new EOFException();
			if (read == c)
				break;
		}
	}

	// TODO: improve
	public static void flushUntilString(Reader in, String string)
			throws IOException {
		CharArrayQueue queue = new CharArrayQueue(string.length());
		int read;

		while (true) {
			if (CharSequenceUtil.equals(queue, string))
				return;
			read = in.read();
			if (read == -1)
				break;
			queue.put((char) read);
		}

		throw new EOFException();
	}

	// TODO: improve
	public static Matcher flushUntilMatch(Reader in, Pattern pattern)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		Matcher matcher = pattern.matcher(builder);
		int read;

		while (true) {
			read = in.read();
			if (read == -1)
				break;

			builder.append((char) read);

			matcher.reset();
			if (matcher.matches())
				return matcher;
		}

		throw new EOFException();
	}

	// TODO: improve
	public static Matcher flushUntilFind(Reader in, Pattern pattern)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		Matcher matcher = pattern.matcher(builder);
		int read;

		while (true) {
			read = in.read();
			if (read == -1)
				break;

			builder.append((char) read);

			matcher.reset();
			if (matcher.find())
				return matcher;
		}

		throw new EOFException();
	}

	public static long skipCharwise(Reader in, long n) throws IOException {
		long i = 0;

		while ((i < n) && (in.read() != -1))
			i++;

		return i;
	}

	public static boolean skipIfChar(PushbackReader in, char c)
			throws IOException {
		int read = readFully(in);
		if (read == c)
			return true;
		in.unread(read);
		return true;
	}

	// TODO: buffered version
	public static boolean matchChars(Reader in, char[] array)
			throws IOException {
		int read;

		for (int i = 0; i < array.length; i++) {
			read = in.read();
			if (read != array[i])
				return false;
			if (read == -1)
				return false;
		}

		return true;
	}

	// TODO: buffered version
	public static boolean matchChars(Reader in, char[] array, int off)
			throws IOException {
		int read;

		for (int i = off; i < array.length; i++) {
			read = in.read();
			if (read != array[i])
				return false;
			if (read == -1)
				return false;
		}

		return true;
	}

	// TODO: buffered version
	public static boolean matchChars(Reader in, char[] array, int off, int len)
			throws IOException {
		int end = off + len;
		int read;

		for (int i = off; i < end; i++) {
			read = in.read();
			if (read != array[i])
				return false;
			if (read == -1)
				return false;
		}

		return true;
	}

	// TODO: buffered version
	public static boolean matchChars(Reader in, CharSequence charSequence)
			throws IOException {
		int length = charSequence.length();
		int read;

		for (int i = 0; i < length; i++) {
			read = in.read();
			if (read != charSequence.charAt(i))
				return false;
			if (read == -1)
				return false;
		}

		return true;
	}

	// TODO: buffered version
	public static boolean matchChars(Reader in, CharSequence charSequence,
			int start, int end) throws IOException {
		int read;

		for (int i = start; i < end; i++) {
			read = in.read();
			if (read != charSequence.charAt(i))
				return false;
			if (read == -1)
				return false;
		}

		return true;
	}

	private final int bufferSize;
	private char[] cbuf;

	public CharStreamUtil() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public CharStreamUtil(boolean initBuffer) {
		this(DEFAULT_BUFFER_SIZE, false);
	}

	public CharStreamUtil(int bufferSize) {
		this(bufferSize, false);
	}

	public CharStreamUtil(int bufferSize, boolean initBuffer) {
		this.bufferSize = bufferSize;

		if (initBuffer)
			initBuffer();
	}

	private void initBuffer() {
		if (cbuf == null)
			cbuf = new char[bufferSize];
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public int writeStream(Reader in, Writer out) throws IOException {
		initBuffer();

		int read;
		int count = 0;

		while (true) {
			read = in.read(cbuf);
			if (read == -1)
				return count;

			out.write(cbuf, 0, read);
			count += read;
		}
	}

	public int writeStreamLimited(Reader in, Writer out, int len)
			throws IOException {
		initBuffer();

		int count = 0;
		int read;

		while (count < len) {
			read = in.read(cbuf, 0, Math.min(bufferSize, len - count));
			if (read == -1)
				break;

			out.write(cbuf, 0, read);
			count += read;
		}

		return count;
	}

	public void flush(Reader in) throws IOException {
		initBuffer();

		while (in.read(cbuf, 0, bufferSize) != -1)
			;
	}

}