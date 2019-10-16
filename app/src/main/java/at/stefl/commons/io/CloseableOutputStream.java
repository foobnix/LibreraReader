package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class CloseableOutputStream extends FilterOutputStream {
    
    private boolean closed;
    
    public CloseableOutputStream(OutputStream out) {
        super(out);
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public void write(int c) throws IOException {
        if (closed) throw new StreamClosedException();
        out.write(c);
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        if (closed) throw new StreamClosedException();
        out.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new StreamClosedException();
        out.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException {
        if (closed) throw new StreamClosedException();
        out.flush();
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        out.flush();
    }
    
}