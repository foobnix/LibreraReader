package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;

// TODO: improve
public class IgnoreWhitespaceReader extends CharwiseFilterReader {
    
    public IgnoreWhitespaceReader(Reader in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        int read;
        
        do {
            read = in.read();
            if (read == -1) return -1;
        } while (Character.isWhitespace((char) read));
        
        return read;
    }
    
}