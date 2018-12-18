package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TeeInputStream extends FilterInputStream {
    
    private final OutputStream tee;
    
    public TeeInputStream(InputStream in, OutputStream tee) {
        super(in);
        
        this.tee = tee;
    }
    
    @Override
    public int read() throws IOException {
        int read = in.read();
        
        if (read != -1) {
            tee.write(read);
            tee.flush();
        }
        
        return read;
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        int read = in.read(b);
        
        if (read != -1) {
            tee.write(b, 0, read);
            tee.flush();
        }
        
        return read;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        
        if (read != -1) {
            tee.write(b, off, read);
            tee.flush();
        }
        
        return read;
    }
    
}