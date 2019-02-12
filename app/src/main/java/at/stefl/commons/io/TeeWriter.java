package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public class TeeWriter extends FilterWriter {
    
    private final Writer tee;
    private final boolean autoFlushTee;
    
    public TeeWriter(Writer out, Writer tee) {
        this(out, tee, false);
    }
    
    public TeeWriter(Writer out, Writer tee, boolean autoFlushTee) {
        super(out);
        
        this.tee = tee;
        this.autoFlushTee = autoFlushTee;
    }
    
    @Override
    public void write(int c) throws IOException {
        out.write(c);
        tee.write(c);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        out.write(cbuf);
        tee.write(cbuf);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        out.write(cbuf, off, len);
        tee.write(cbuf, off, len);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void write(String str) throws IOException {
        out.write(str);
        tee.write(str);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        out.write(str, off, len);
        tee.write(str, off, len);
        if (autoFlushTee) tee.flush();
    }
    
    @Override
    public Writer append(char c) throws IOException {
        out.append(c);
        tee.append(c);
        if (autoFlushTee) tee.flush();
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        out.append(csq);
        tee.append(csq);
        if (autoFlushTee) tee.flush();
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        out.append(csq, start, end);
        tee.append(csq, start, end);
        if (autoFlushTee) tee.flush();
        return this;
    }
    
    @Override
    public void flush() throws IOException {
        out.flush();
        tee.flush();
    }
    
}