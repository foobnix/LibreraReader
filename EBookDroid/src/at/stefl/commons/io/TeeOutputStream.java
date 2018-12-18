package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStream extends FilterOutputStream {
    
    private final OutputStream tee;
    private final boolean autoFlushTee;
    
    public TeeOutputStream(OutputStream out, OutputStream tee) {
        this(out, tee, false);
    }
    
    public TeeOutputStream(OutputStream out, OutputStream tee,
            boolean autoFlushTee) {
        super(out);
        
        this.tee = tee;
        this.autoFlushTee = autoFlushTee;
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b);
        tee.write(b);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        tee.write(b);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        tee.write(b, off, len);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void flush() throws IOException {
        out.flush();
        tee.flush();
    }
    
}