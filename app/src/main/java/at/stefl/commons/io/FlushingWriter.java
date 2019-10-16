package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public class FlushingWriter extends FilterWriter {
    
    public FlushingWriter(Writer out) {
        super(out);
    }
    
    @Override
    public void write(int c) throws IOException {
        out.write(c);
        out.flush();
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        out.write(cbuf);
        out.flush();
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
        out.flush();
    }
    
    @Override
    public void write(String str) throws IOException {
        out.write(str);
        out.flush();
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        out.write(str, off, len);
        out.flush();
    }
    
    @Override
    public Writer append(char c) throws IOException {
        out.append(c);
        out.flush();
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        out.append(csq);
        out.flush();
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        out.append(csq, start, end);
        out.flush();
        return this;
    }
    
}