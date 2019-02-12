package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public abstract class BytewiseFilterInputStream extends FilterInputStream {
    
    public BytewiseFilterInputStream(InputStream in) {
        super(in);
    }
    
    @Override
    public abstract int read() throws IOException;
    
    @Override
    public int read(byte[] b) throws IOException {
        return ByteStreamUtil.readBytewise(this, b, 0, b.length);
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return ByteStreamUtil.readBytewise(this, b, off, len);
    }
    
    @Override
    public long skip(long n) throws IOException {
        return ByteStreamUtil.skipBytewise(this, n);
    }
    
}