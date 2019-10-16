package at.stefl.commons.lwxml.writer;

import java.io.IOException;
import java.io.Reader;

import at.stefl.commons.io.TeeReader;
import at.stefl.commons.lwxml.LWXMLEvent;

// TODO: implement specific methods?
public class LWXMLTeeWriter extends LWXMLFilterWriter {
    
    private final LWXMLWriter tee;
    private final boolean autoFlushTee;
    
    public LWXMLTeeWriter(LWXMLWriter out, LWXMLWriter tee) {
        this(out, tee, false);
    }
    
    public LWXMLTeeWriter(LWXMLWriter out, LWXMLWriter tee, boolean autoFlushTee) {
        super(out);
        
        this.tee = tee;
        this.autoFlushTee = autoFlushTee;
    }
    
    @Override
    public void writeEvent(LWXMLEvent event) throws IOException {
        out.writeEvent(event);
        tee.writeEvent(event);
        if (autoFlushTee) tee.flush();
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
    public int write(Reader in) throws IOException {
        int result = out.write(new TeeReader(in, tee));
        if (autoFlushTee) tee.flush();
        return result;
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
    public LWXMLWriter append(char c) throws IOException {
        out.append(c);
        tee.append(c);
        if (autoFlushTee) tee.flush();
        return this;
    }
    
    @Override
    public LWXMLWriter append(CharSequence csq) throws IOException {
        out.append(csq);
        tee.append(csq);
        if (autoFlushTee) tee.flush();
        return this;
    }
    
    @Override
    public LWXMLWriter append(CharSequence csq, int start, int end)
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