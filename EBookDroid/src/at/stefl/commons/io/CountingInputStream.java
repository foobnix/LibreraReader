package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {
    
    private long count;
    
    public CountingInputStream(InputStream in) {
        super(in);
    }
    
    public long count() {
        return count;
    }
    
    @Override
    public int read() throws IOException {
        int read = in.read();
        if (read != -1) count++;
        return read;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        int read = in.read(b);
        if (read != -1) count += read;
        return read;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read != -1) count += read;
        return read;
    }
    
    @Override
    public long skip(long n) throws IOException {
        long skipped = in.skip(n);
        count += skipped;
        return skipped;
    }
    
}