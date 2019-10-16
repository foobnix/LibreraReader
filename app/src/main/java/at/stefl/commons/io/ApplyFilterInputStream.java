package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class ApplyFilterInputStream extends BytewiseFilterInputStream {
    
    private final ByteFilter filter;
    
    public ApplyFilterInputStream(InputStream in, ByteFilter filter) {
        super(in);
        
        this.filter = filter;
    }
    
    @Override
    public int read() throws IOException {
        int read;
        
        do {
            read = in.read();
            if (read == -1) return -1;
        } while (!filter.accept((byte) read));
        
        return read;
    }
    
}