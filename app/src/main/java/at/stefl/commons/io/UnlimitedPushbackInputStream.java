package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.LinkedList;

// TODO: optimize
public class UnlimitedPushbackInputStream extends PushbackInputStream {
    
    // removed Deque because of Android 1.6
    // private Deque<Byte> buffer = new LinkedList<Byte>();
    private LinkedList<Byte> buffer = new LinkedList<Byte>();
    
    public UnlimitedPushbackInputStream(InputStream in) {
        super(in);
    }
    
    @Override
    public int read() throws IOException {
        if (buffer.isEmpty()) return super.read();
        else return buffer.removeFirst();
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;
        
        while (!buffer.isEmpty()) {
            int read = read();
            if (read == -1) return count;
            b[off] = (byte) read;
            off++;
            len--;
            count++;
        }
        
        if ((off < b.length) & (len > 0)) count += super.read(b, off, len);
        
        return count;
    }
    
    @Override
    public void unread(int b) throws IOException {
        buffer.addLast((byte) b);
    }
    
    @Override
    public void unread(byte[] b) throws IOException {
        for (byte i : b) {
            buffer.addLast(i);
        }
    }
    
    @Override
    public void unread(byte[] b, int off, int len) throws IOException {
        for (int i = off; i < (off + len); i++) {
            buffer.addLast(b[i]);
        }
    }
    
    @Override
    public int available() throws IOException {
        return buffer.size() + super.available();
    }
    
    @Override
    public void close() throws IOException {
        buffer = null;
        super.close();
    }
    
}