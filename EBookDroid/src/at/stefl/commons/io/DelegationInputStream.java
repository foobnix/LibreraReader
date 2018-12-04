package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class DelegationInputStream extends InputStream {
    
    protected InputStream in;
    
    public DelegationInputStream(InputStream in) {
        this.in = in;
    }
    
    @Override
    public int read() throws IOException {
        return in.read();
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }
    
    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }
    
    @Override
    public int available() throws IOException {
        return in.available();
    }
    
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
    
    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }
    
    @Override
    public synchronized void reset() throws IOException {
        in.reset();
    }
    
    @Override
    public void close() throws IOException {
        in.close();
    }
    
}