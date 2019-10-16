package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class ApplyFilterOutputStream extends BytewiseFilterOutputStream {
    
    private final ByteFilter filter;
    
    public ApplyFilterOutputStream(OutputStream out, ByteFilter filter) {
        super(out);
        
        this.filter = filter;
    }
    
    @Override
    public void write(int b) throws IOException {
        if (filter.accept((byte) b)) out.write(b);
    }
    
}