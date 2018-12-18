package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

// TODO: make use of
public class ProxyReader extends DelegationReader {
    
    public ProxyReader(Reader in) {
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
    public int read(char[] cbuf) throws IOException {
        try {
            beforeRead(cbuf.length);
            int result = in.read(cbuf);
            afterRead(result);
            return result;
        } catch (IOException e) {
            handleIOException(e);
            return -1;
        }
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        try {
            beforeRead(len);
            int result = in.read(cbuf, off, len);
            afterRead(result);
            return result;
        } catch (IOException e) {
            handleIOException(e);
            return -1;
        }
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        try {
            beforeRead(target.remaining());
            int result = in.read(target);
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
    public boolean ready() throws IOException {
        try {
            return in.ready();
        } catch (IOException e) {
            handleIOException(e);
            return false;
        }
    }
    
    @Override
    public void mark(int readAheadLimit) throws IOException {
        try {
            in.mark(readAheadLimit);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void reset() throws IOException {
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