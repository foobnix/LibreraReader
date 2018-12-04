package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class ClosedReader extends Reader {
    
    public static final ClosedReader CLOSED_READER = new ClosedReader();
    
    private ClosedReader() {}
    
    @Override
    public int read() throws IOException {
        return -1;
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return -1;
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return -1;
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        return -1;
    }
    
    @Override
    public long skip(long n) throws IOException {
        return 0;
    }
    
    @Override
    public boolean ready() throws IOException {
        return true;
    }
    
    @Override
    public void close() throws IOException {}
    
}