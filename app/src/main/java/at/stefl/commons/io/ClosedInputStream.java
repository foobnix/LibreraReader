package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class ClosedInputStream extends InputStream {
    
    public static final ClosedInputStream CLOSED_INPUT_STREAM = new ClosedInputStream();
    
    private ClosedInputStream() {}
    
    @Override
    public int read() throws IOException {
        return -1;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return -1;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return -1;
    }
    
    @Override
    public long skip(long n) throws IOException {
        return 0;
    }
    
    @Override
    public int available() throws IOException {
        return 0;
    }
    
}