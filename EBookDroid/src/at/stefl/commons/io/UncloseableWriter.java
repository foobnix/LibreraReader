package at.stefl.commons.io;

import java.io.IOException;
import java.io.Writer;

public class UncloseableWriter extends FilterWriter {
    
    public UncloseableWriter(Writer out) {
        super(out);
    }
    
    @Override
    public void close() throws IOException {
        out = ClosedWriter.CLOSED_WRITER;
    }
    
}