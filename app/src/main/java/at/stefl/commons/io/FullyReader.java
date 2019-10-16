package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class FullyReader extends DelegationReader {
    
    public FullyReader(Reader in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        return CharStreamUtil.readFully(in);
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return CharStreamUtil.readFully(in, cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return CharStreamUtil.readFully(in, cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        return CharStreamUtil.readFully(in, target);
    }
    
}