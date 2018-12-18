package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class FlushingOutputStream extends FilterOutputStream {
    
    public FlushingOutputStream(OutputStream out) {
        super(out);
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b);
        out.flush();
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        out.flush();
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        out.flush();
    }
    
}