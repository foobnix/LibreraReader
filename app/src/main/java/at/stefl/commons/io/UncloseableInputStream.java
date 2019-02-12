package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;

public class UncloseableInputStream extends FilterInputStream {
    
    public UncloseableInputStream(InputStream in) {
        super(in);
    }
    
    @Override
    public void close() throws IOException {
        in = ClosedInputStream.CLOSED_INPUT_STREAM;
    }
    
}