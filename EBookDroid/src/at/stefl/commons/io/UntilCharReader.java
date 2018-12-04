package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;

import at.stefl.commons.util.StateMachine;

public class UntilCharReader extends CharwiseFilterReader implements
        StateMachine {
    
    private boolean found;
    
    private char c;
    
    public UntilCharReader(Reader in, char c) {
        super(in);
        
        this.c = c;
    }
    
    @Override
    public int read() throws IOException {
        if (found) return -1;
        
        int read = in.read();
        
        if ((read == -1) || (read == c)) {
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