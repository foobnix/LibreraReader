package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public abstract class DelegationReader extends Reader {
    
    protected Reader in;
    
    public DelegationReader(Reader in) {
        this.in = in;
    }
    
    @Override
    public int read() throws IOException {
        return in.read();
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return in.read(cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return in.read(cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        return in.read(target);
    }
    
    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }
    
    @Override
    public boolean ready() throws IOException {
        return in.ready();
    }
    
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }
    
    @Override
    public void mark(int readAheadLimit) throws IOException {
        in.mark(readAheadLimit);
    }
    
    @Override
    public void reset() throws IOException {
        in.reset();
    }
    
    @Override
    public void close() throws IOException {
        in.close();
    }
    
}