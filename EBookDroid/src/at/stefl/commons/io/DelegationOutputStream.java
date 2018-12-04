package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public abstract class DelegationOutputStream extends OutputStream {
    
    protected OutputStream out;
    
    public DelegationOutputStream(OutputStream out) {
        this.out = out;
    }
    
    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException {
        out.flush();
    }
    
    @Override
    public void close() throws IOException {
        out.close();
    }
    
}