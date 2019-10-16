package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public abstract class CharwiseFilterWriter extends FilterWriter {
    
    public CharwiseFilterWriter(Writer out) {
        super(out);
    }
    
    @Override
    public abstract void write(int c) throws IOException;
    
    @Override
    public void write(char[] cbuf) throws IOException {
        CharStreamUtil.writeCharwise(this, cbuf);
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        CharStreamUtil.writeCharwise(this, cbuf, off, len);
    }
    
    @Override
    public void write(String str) throws IOException {
        CharStreamUtil.writeCharwise(this, str);
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        CharStreamUtil.writeCharwise(this, str, off, len);
    }
    
    @Override
    public Writer append(char c) throws IOException {
        write(c);
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq) throws IOException {
        CharStreamUtil.appendCharwise(this, csq);
        return this;
    }
    
    @Override
    public Writer append(CharSequence csq, int start, int end)
            throws IOException {
        CharStreamUtil.appendCharwise(this, csq, start, end);
        return this;
    }
    
}