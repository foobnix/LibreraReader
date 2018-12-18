package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class CountingOutputStream extends FilterOutputStream {
    
    private long count;
    
    public CountingOutputStream(OutputStream out) {
        super(out);
    }
    
    public long count() {
        return count;
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b);
        count++;
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        count += b.length;
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }
    
}