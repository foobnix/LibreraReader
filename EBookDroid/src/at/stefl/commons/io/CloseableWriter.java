package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public class CloseableWriter extends FilterWriter {
    
    private boolean closed;
    
    public CloseableWriter(Writer out) {
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
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (closed) throw new StreamClosedException();
        out.write(cbuf, off, len);
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        if (closed) throw new StreamClosedException();
        out.write(str, off, len);
    }
    
    @Override
    public Writer append(char c) throws IOException {
        if (closed) throw new StreamClosedException();
        return out.append(c);
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        if (closed) throw new StreamClosedException();
        return out.append(csq);
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        if (closed) throw new StreamClosedException();
        return out.append(csq, start, end);
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