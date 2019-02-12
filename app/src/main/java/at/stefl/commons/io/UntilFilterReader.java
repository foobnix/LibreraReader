package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;

import at.stefl.commons.util.StateMachine;

public class UntilFilterReader extends CharwiseFilterReader implements
        StateMachine {
    
    private boolean found;
    
    private final CharFilter filter;
    
    public UntilFilterReader(Reader in, CharFilter filter) {
        super(in);
        
        this.filter = filter;
    }
    
    @Override
    public int read() throws IOException {
        if (found) return -1;
        
        int read = in.read();
        
        if ((read == -1) || !filter.accept((char) read)) {
            found = true;
            return -1;
        }
        
        return read;
    }
    
    @Override
    public void reset() {
        found = false;
    }
    
}