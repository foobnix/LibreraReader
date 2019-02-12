package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

// TODO: make use of
public abstract class ProxyInputStream extends DelegationInputStream {
    
    public ProxyInputStream(InputStream in) {
        super(in);
    }
    
    protected void beforeRead(int n) throws IOException {}
    
    protected void afterRead(int n) throws IOException {}
    
    protected void handleIOException(IOException e) throws IOException {
        throw e;
    }
    
    @Override
    public int read() throws IOException {
        try {
            beforeRead(1);
            int result = in.read();
            afterRead((result != -1) ? 1 : -1);
            return result;
        } catch (IOException e) {
            handleIOException(e);
            return -1;
        }
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        try {
            beforeRead(b.length);
            int result = in.read(b);
            afterRead(result);
            return result;
        } catch (IOException e) {
            handleIOException(e);
            return -1;
        }
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            beforeRead(len);
            int result = in.read(b, off, len);
            afterRead(result);
            return result;
        } catch (IOException e) {
            handleIOException(e);
            return -1;
        }
    }
    
    @Override
    public long skip(long n) throws IOException {
        try {
            return in.skip(n);
        } catch (IOException e) {
            handleIOException(e);
            return 0;
        }
    }
    
    @Override
    public int available() throws IOException {
        try {
            return in.available();
        } catch (IOException e) {
            handleIOException(e);
            return 0;
        }
    }
    
    @Override
    public synchronized void reset() throws IOException {
        try {
            in.reset();
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void close() throws IOException {
        try {
            in.close();
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
}