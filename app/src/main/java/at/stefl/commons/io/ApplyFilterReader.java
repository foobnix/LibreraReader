package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;

public class ApplyFilterReader extends CharwiseFilterReader {
    
    private final CharFilter filter;
    
    public ApplyFilterReader(Reader in, CharFilter filter) {
        super(in);
        
        this.filter = filter;
    }
    
    @Override
    public int read() throws IOException {
        int read;
        
        do {
            read = in.read();
            if (read == -1) return -1;
        } while (!filter.accept((char) read));
        
        return read;
    }
    
}