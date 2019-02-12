package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class CloseableReader extends FilterReader {
    
    private boolean closed;
    
    public CloseableReader(Reader in) {
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
    public int read(char[] cbuf) throws IOException {
        if (closed) throw new StreamClosedException();
        return in.read(cbuf);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        if (closed) throw new StreamClosedException();
        return in.read(target);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (closed) throw new StreamClosedException();
        return in.read(cbuf, off, len);
    }
    
    @Override
    public boolean ready() throws IOException {
        if (closed) throw new StreamClosedException();
        return in.ready();
    }
    
    @Override
    public void reset() throws IOException {
        if (closed) throw new StreamClosedException();
        in.reset();
    }
    
    @Override
    public void mark(int readAheadLimit) throws IOException {
        if (closed) throw new StreamClosedException();
        in.mark(readAheadLimit);
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