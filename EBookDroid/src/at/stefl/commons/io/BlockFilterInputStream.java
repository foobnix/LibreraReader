package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class BlockFilterInputStream extends DelegationInputStream {
    
    private final byte[] singleByte = new byte[1];
    
    public BlockFilterInputStream(InputStream in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        return (ByteStreamUtil.readTireless(in, singleByte, 0, 1) == -1) ? -1
                : singleByte[0];
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }
    
    @Override
    public abstract int read(byte[] b, int off, int len) throws IOException;
    
}