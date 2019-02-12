package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

// TODO: make use of
public abstract class ProxyWriter extends DelegationWriter {
    
    public ProxyWriter(Writer out) {
        super(out);
    }
    
    protected void beforeWrite(int n) throws IOException {}
    
    protected void afterWrite(int n) throws IOException {}
    
    protected void handleIOException(IOException e) throws IOException {
        throw e;
    }
    
    @Override
    public void write(int c) throws IOException {
        try {
            beforeWrite(1);
            out.write(c);
            afterWrite(1);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        try {
            beforeWrite(cbuf.length);
            out.write(cbuf);
            afterWrite(cbuf.length);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        try {
            beforeWrite(len);
            out.write(cbuf, off, len);
            afterWrite(len);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void write(String str) throws IOException {
        try {
            beforeWrite(str.length());
            out.write(str);
            afterWrite(str.length());
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        try {
            beforeWrite(len);
            out.write(str, off, len);
            afterWrite(len);
        } catch (IOException e) {
            handleIOException(e);
        }
    }
    
    @Override
    public Writer append(char c) throws IOException {
        try {
            beforeWrite(1);
            out.append(c);
            afterWrite(1);
        } catch (IOException e) {
            handleIOException(e);
        }
        
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        try {
            beforeWrite(csq.length());
            out.append(csq);
            afterWrite(csq.length());
        } catch (IOException e) {
            handleIOException(e);
        }
        
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        try {
            beforeWrite(end - start);
            out.append(csq, start, end);
            afterWrite(end - start);
        } catch (IOException e) {
            handleIOException(e);
        }
        
        return this;
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