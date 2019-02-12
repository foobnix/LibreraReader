package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public abstract class DelegationWriter extends Writer {
    
    protected Writer out;
    
    public DelegationWriter(Writer out) {
        this.out = out;
    }
    
    @Override
    public void write(int c) throws IOException {
        out.write(c);
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        out.write(cbuf);
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
    }
    
    @Override
    public void write(String str) throws IOException {
        out.write(str);
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        out.write(str, off, len);
    }
    
    @Override
    public Writer append(char c) throws IOException {
        return out.append(c);
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        return out.append(csq);
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        return out.append(csq, start, end);
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