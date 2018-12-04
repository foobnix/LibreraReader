package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public abstract class BlockFilterWriter extends DelegationWriter {
    
    private final char[] singleByte = new char[1];
    
    public BlockFilterWriter(Writer in) {
        super(in);
    }
    
    @Override
    public void write(int b) throws IOException {
        singleByte[0] = (char) b;
        write(singleByte, 0, 1);
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }
    
    @Override
    public abstract void write(char[] cbuf, int off, int len)
            throws IOException;
    
}