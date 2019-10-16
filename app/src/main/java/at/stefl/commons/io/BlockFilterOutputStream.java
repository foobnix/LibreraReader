package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public abstract class BlockFilterOutputStream extends DelegationOutputStream {
    
    private final byte[] singleByte = new byte[1];
    
    public BlockFilterOutputStream(OutputStream in) {
        super(in);
    }
    
    @Override
    public void write(int b) throws IOException {
        singleByte[0] = (byte) b;
        write(singleByte, 0, 1);
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }
    
    @Override
    public abstract void write(byte[] b, int off, int len) throws IOException;
    
}