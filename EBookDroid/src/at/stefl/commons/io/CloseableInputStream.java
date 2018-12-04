package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class CloseableInputStream extends FilterInputStream {
    
    private boolean closed;
    
    public CloseableInputStream(InputStream in) {
        super(in);
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public int read() throws IOException {
        if (closed) throw new StreamClosedException();
        return in.read();
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        if (closed) throw new StreamClosedException();
        return in.read(b);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) throw new StreamClosedException();
        return in.read(b, off, len);
    }
    
    @Override
    public int available() throws IOException {
        if (closed) throw new StreamClosedException();
        return in.available();
    }
    
    @Override
    public void reset() throws IOException {
        if (closed) throw new StreamClosedException();
        in.reset();
    }
    
    @Override
    public synchronized void mark(int readlimit) {
        if (closed) return;
        in.mark(readlimit);
    }
    
    @Override
    public long skip(long n) throws IOException {
        if (closed) throw new StreamClosedException();
        return in.skip(n);
    }
    
    @Override
    public void close() {
        closed = true;
    }
    
}