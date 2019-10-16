package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public class ApplyFilterWriter extends CharwiseFilterWriter {
    
    private final CharFilter filter;
    
    public ApplyFilterWriter(Writer out, CharFilter filter) {
        super(out);
        
        this.filter = filter;
    }
    
    @Override
    public void write(int c) throws IOException {
        if (filter.accept((char) c)) out.write(c);
    }
    
}