package at.stefl.commons.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

// TODO: encode block-wise
// TODO: use bytes?
public class Base64OutputStream extends OutputStream {
    
    private final Writer out;
    private boolean closed;
    
    private final Base64Settings settings;
    
    private byte[] inBuffer = new byte[3];
    private char[] outBuffer = new char[4];
    private int index;
    
    public Base64OutputStream(Writer out, Base64Settings settings) {
        this.out = out;
        this.settings = settings;
    }
    
    @Override
    public void write(int b) throws IOException {
        if (closed) throw new IOException("stream is already closed");
        
        inBuffer[index++] = (byte) b;
        
        if (index >= 3) {
            Base64.encode3Byte(inBuffer, outBuffer, settings);
            out.write(outBuffer);
            index = 0;
        }
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException {
        out.flush();
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        
        if (index > 0) {
            Base64.encode3Byte(inBuffer, index, outBuffer, settings);
            out.write(outBuffer);
            out.flush();
        }
        
        out.close();
    }
    
}