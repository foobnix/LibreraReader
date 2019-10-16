package at.stefl.commons.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

public class ByteStreamUtil {
    
    public static final int DEFAULT_BUFFER_SIZE = 8192;
    
    public static int readTireless(InputStream in, byte[] b) throws IOException {
        if (b.length == 0) return 0;
        
        int result;
        int read;
        
        for (result = 0; result < b.length; result += read) {
            read = in.read(b, result, b.length - result);
            if (read == -1) break;
        }
        
        return (result == 0) ? -1 : result;
    }
    
    public static int readTireless(InputStream in, byte[] b, int off, int len)
            throws IOException {
        if (len == 0) return 0;
        
        int result;
        int read;
        
        for (result = 0; result < len; result += read) {
            read = in.read(b, off + result, len - result);
            if (read == -1) break;
        }
        
        return (result == 0) ? -1 : result;
    }
    
    public static int readBytewise(InputStream in, byte[] b) throws IOException {
        if (b.length == 0) return 0;
        
        int result;
        int read;
        
        for (result = 0; result < b.length; result++) {
            read = in.read();
            if (read == -1) break;
            
            b[result] = (byte) read;
        }
        
        return (result == 0) ? -1 : result;
    }
    
    public static int readBytewise(InputStream in, byte[] b, int off, int len)
            throws IOException {
        if (len == 0) return 0;
        
        int result;
        int read;
        
        for (result = 0; result < len; result++) {
            read = in.read();
            if (read == -1) break;
            
            b[off + result] = (byte) read;
        }
        
        return (result == 0) ? -1 : result;
    }
    
    public static byte readFully(InputStream in) throws IOException {
        int read = in.read();
        if (read == -1) throw new EOFException();
        return (byte) read;
    }
    
    public static byte[] readFully(InputStream in, int len) throws IOException {
        byte[] b = new byte[len];
        int read = readFully(in, b);
        if (read < len) throw new EOFException();
        return b;
    }
    
    public static int readFully(InputStream in, byte[] b) throws IOException {
        int read = readTireless(in, b);
        if (read < b.length) throw new EOFException();
        return read;
    }
    
    public static int readFully(InputStream in, byte[] b, int off, int len)
            throws IOException {
        int read = readTireless(in, b, off, len);
        if (read < len) throw new EOFException();
        return read;
    }
    
    public static byte[] readBytes(InputStream in) throws IOException {
        DividedByteArrayOutputStream out = new DividedByteArrayOutputStream();
        out.write(in);
        out.close();
        return out.toByteArray();
    }
    
    public static void writeBytewise(OutputStream out, byte[] b)
            throws IOException {
        for (int i = 0; i < b.length; i++) {
            out.write(b[i]);
        }
    }
    
    public static void writeBytewise(OutputStream out, byte[] b, int off)
            throws IOException {
        for (int i = off; i < b.length; i++) {
            out.write(b[i]);
        }
    }
    
    public static void writeBytewise(OutputStream out, byte[] b, int off,
            int len) throws IOException {
        for (int i = off; i < len; i++) {
            out.write(b[i]);
        }
    }
    
    public static void writeStreamBytewise(InputStream in, OutputStream out)
            throws IOException {
        for (int read; (read = in.read()) != -1;)
            out.write(read);
    }
    
    public static int writeStreamBytewiseLimited(InputStream in,
            OutputStream out, int len) throws IOException {
        int read;
        int count = 0;
        
        while (true) {
            read = in.read();
            if (read == -1) return count;
            
            out.write(read);
            count++;
        }
    }
    
    public static int writeStreamBuffered(InputStream in, OutputStream out)
            throws IOException {
        return writeStreamBuffered(in, out, DEFAULT_BUFFER_SIZE);
    }
    
    public static int writeStreamBuffered(InputStream in, OutputStream out,
            int bufferSize) throws IOException {
        byte[] b = new byte[bufferSize];
        return writeStreamBuffered(in, out, b);
    }
    
    public static int writeStreamBuffered(InputStream in, OutputStream out,
            byte[] b) throws IOException {
        int read;
        int count = 0;
        
        while (true) {
            read = in.read(b);
            if (read == -1) return count;
            
            out.write(b, 0, read);
            count += read;
        }
    }
    
    public static void flushBytewise(InputStream in) throws IOException {
        while (in.read() != -1)
            ;
    }
    
    public static void flushBuffered(InputStream in) throws IOException {
        flushBuffered(in, DEFAULT_BUFFER_SIZE);
    }
    
    public static void flushBuffered(InputStream in, int bufferSize)
            throws IOException {
        byte[] b = new byte[bufferSize];
        while (in.read(b, 0, bufferSize) != -1)
            ;
    }
    
    public static int flushBytewiseCount(InputStream in) throws IOException {
        int result = 0;
        while (in.read() != -1)
            result++;
        return result;
    }
    
    public static int flushBufferedCount(InputStream in) throws IOException {
        return flushBufferedCount(in, DEFAULT_BUFFER_SIZE);
    }
    
    public static int flushBufferedCount(InputStream in, int bufferSize)
            throws IOException {
        int result = 0;
        int read;
        byte[] b = new byte[bufferSize];
        while ((read = in.read(b, 0, bufferSize)) != -1)
            result += read;
        return result;
    }
    
    public static long skipBytewise(InputStream in, long n) throws IOException {
        long i = 0;
        
        while ((i < n) && (in.read() != -1))
            i++;
        
        return i;
    }
    
    public static boolean skipIfByte(PushbackInputStream in, byte c)
            throws IOException {
        int read = readFully(in);
        if (read == c) return true;
        in.unread(read);
        return true;
    }
    
    // TODO: buffered version
    public static boolean matchBytes(InputStream in, byte[] array)
            throws IOException {
        int read;
        
        for (int i = 0; i < array.length; i++) {
            read = in.read();
            if (read != array[i]) return false;
            if (read == -1) return false;
        }
        
        return true;
    }
    
    // TODO: buffered version
    public static boolean matchBytes(InputStream in, byte[] array, int off,
            int len) throws IOException {
        int end = off + len;
        int read;
        
        for (int i = off; i < end; i++) {
            read = in.read();
            if (read != array[i]) return false;
            if (read == -1) return false;
        }
        
        return true;
    }
    
    private final int bufferSize;
    private byte[] b;
    
    public ByteStreamUtil() {
        this(DEFAULT_BUFFER_SIZE, false);
    }
    
    public ByteStreamUtil(boolean initBuffer) {
        this(DEFAULT_BUFFER_SIZE, initBuffer);
    }
    
    public ByteStreamUtil(int bufferSize) {
        this(bufferSize, false);
    }
    
    public ByteStreamUtil(int bufferSize, boolean initBuffer) {
        this.bufferSize = bufferSize;
        
        if (initBuffer) initBuffer();
    }
    
    private void initBuffer() {
        if (b == null) b = new byte[bufferSize];
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    public int writeStream(InputStream in, OutputStream out) throws IOException {
        initBuffer();
        
        return writeStreamBuffered(in, out, b);
    }
    
    public int writeStreamLimited(InputStream in, OutputStream out, int len)
            throws IOException {
        initBuffer();
        
        int count = 0;
        int read;
        
        while (count < len) {
            read = in.read(b, 0, Math.min(bufferSize, len - count));
            if (read == -1) break;
            
            out.write(b, 0, read);
            count += read;
        }
        
        return count;
    }
    
    public void flush(InputStream in) throws IOException {
        initBuffer();
        
        while (in.read(b, 0, bufferSize) != -1)
            ;
    }
    
}