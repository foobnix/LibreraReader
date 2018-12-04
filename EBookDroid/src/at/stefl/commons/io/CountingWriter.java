package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public class CountingWriter extends FilterWriter {
    
    private long count;
    
    public CountingWriter(Writer out) {
        super(out);
    }
    
    public long count() {
        return count;
    }
    
    @Override
    public void write(int c) throws IOException {
        out.write(c);
        count++;
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        out.write(cbuf);
        count += cbuf.length;
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
        count += len;
    }
    
    @Override
    public void write(String str) throws IOException {
        out.write(str);
        count += str.length();
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        out.write(str, off, len);
        count += str.length();
    }
    
    @Override
    public Writer append(char c) throws IOException {
        out.append(c);
        count++;
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        if (csq == null) csq = "null";
        out.append(csq);
        count += csq.length();
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        out.append(csq, start, end);
        count += end - start;
        return this;
    }
    
}