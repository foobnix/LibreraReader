package at.stefl.commons.io;

import java.io.IOException;
import java.io.OutputStream;

public class ClosedOutputStream extends OutputStream {
    
    public static final ClosedOutputStream CLOSED_OUTPUT_STREAM = new ClosedOutputStream();
    
    private ClosedOutputStream() {}
    
    @Override
    public void write(int b) throws IOException {
        throw new IOException("stream already closed");
    }
    
}