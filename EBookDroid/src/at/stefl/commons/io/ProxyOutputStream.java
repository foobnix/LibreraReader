package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

// TODO: make use of
public abstract class ProxyOutputStream extends DelegationOutputStream {
    
    public ProxyOutputStream(OutputStream out) {
        super(out);
    }
    
    protected void beforeWrite(int n) throws IOException {}
    
    protected void afterWrite(int n) throws IOException {}
    
    protected void handleIOException(IOException e) throws IOException {
        throw e;
    }
    
    @Override
    public void write(int b) throws IOException {
        try {
            beforeWrite(1);
            out.write(b);
            afterWrite(1);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        try {
            beforeWrite(b.length);
            out.write(b);
            afterWrite(b.length);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            beforeWrite(len);
            out.write(b, off, len);
            afterWrite(len);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void flush() throws IOException {
        try {
            out.flush();
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void close() throws IOException {
        try {
            out.flush();
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
}