package at.stefl.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class LimitedReader extends DelegationReader {
    
    private long left;
    
    public LimitedReader(Reader in, long limit) {
        super(in);
        
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        this.left = limit;
    }
    
    public long left() {
        return left;
    }
    
    @Override
    public int read() throws IOException {
        if (left <= 0) return -1;
        left--;
        return in.read();
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (left <= 0) return -1;
        int result = in.read(cbuf, off, (int) Math.min(left, len));
        left -= result;
        return result;
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        if (left <= 0) return -1;
        CharBuffer newTarget = target.duplicate();
        target.limit((int) Math.min(target.position() + left, target.limit()));
        int result = in.read(newTarget);
        target.position(target.position() + result);
        left -= result;
        return result;
    }
    
    @Override
    public boolean ready() throws IOException {
        return (left > 0) && in.ready();
    }
    
}