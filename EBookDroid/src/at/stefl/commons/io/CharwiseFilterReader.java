package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public abstract class CharwiseFilterReader extends FilterReader {
    
    public CharwiseFilterReader(Reader in) {
        super(in);
    }
    
    @Override
    public abstract int read() throws IOException;
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return CharStreamUtil.readCharwise(this, cbuf, 0, cbuf.length);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return CharStreamUtil.readCharwise(this, cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        return CharStreamUtil.readCharwise(this, target);
    }
    
    @Override
    public long skip(long n) throws IOException {
        return CharStreamUtil.skipCharwise(this, n);
    }
    
}