package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;

public abstract class BlockFilterReader extends DelegationReader {
    
    private final char[] singleByte = new char[1];
    
    public BlockFilterReader(Reader in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        return (read(singleByte, 0, 1) == -1) ? -1 : singleByte[0];
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }
    
    @Override
    public abstract int read(char[] cbuf, int off, int len) throws IOException;
    
}