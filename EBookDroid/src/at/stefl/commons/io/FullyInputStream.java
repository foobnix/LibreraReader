package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class FullyInputStream extends DelegationInputStream {
    
    public FullyInputStream(InputStream in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        return ByteStreamUtil.readFully(in);
    }
    
    @Override
    public int read(byte[] b) throws IOException {
        return ByteStreamUtil.readFully(in, b);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return ByteStreamUtil.readFully(in, b, off, len);
    }
    
}