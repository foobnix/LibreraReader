package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

import at.stefl.commons.util.StateMachine;

public class UntilFilterInputStream extends BytewiseFilterInputStream implements
        StateMachine {
    
    private boolean found;
    
    private final ByteFilter filter;
    
    public UntilFilterInputStream(InputStream in, ByteFilter filter) {
        super(in);
        
        this.filter = filter;
    }
    
    @Override
    public int read() throws IOException {
        if (found) return -1;
        
        int read = in.read();
        
        if ((read == -1) || !filter.accept((byte) read)) {
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