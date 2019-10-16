package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class TeeReader extends FilterReader {
    
    private final Writer tee;
    
    public TeeReader(Reader in, Writer tee) {
        super(in);
        
        this.tee = tee;
    }
    
    @Override
    public int read() throws IOException {
        int read = in.read();
        
        if (read != -1) {
            tee.write(read);
            tee.flush();
        }
        
        return read;
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        int read = in.read(cbuf);
        
        if (read != -1) {
            tee.write(cbuf, 0, read);
            tee.flush();
        }
        
        return read;
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = in.read(cbuf, off, len);
        
        if (read != -1) {
            tee.write(cbuf, off, read);
            tee.flush();
        }
        
        return read;
    }
    
    // TODO: implement
    // @Override
    // public int read(CharBuffer target) throws IOException {}
    
}