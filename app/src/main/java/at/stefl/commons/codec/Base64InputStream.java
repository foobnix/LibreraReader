package at.stefl.commons.codec;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import at.stefl.commons.io.CharStreamUtil;

// TODO: use bytes?
public class Base64InputStream extends InputStream {
    
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    private final Reader in;
    private boolean closed;
    
    private final Base64Settings settings;
    
    private final char[] inBuffer;
    private final byte[] outBuffer = new byte[3];
    private int outIndex = 0;
    private int outBuffered = 0;
    
    public Base64InputStream(Reader in, Base64Settings settings) {
        this(in, DEFAULT_BUFFER_SIZE, settings);
    }
    
    public Base64InputStream(Reader in, int bufferSize, Base64Settings settings) {
        this.in = in;
        this.inBuffer = new char[bufferSize];
        this.settings = settings;
    }
    
    @Override
    public int read() throws IOException {
        if (closed) return -1;
        
        if (outIndex >= outBuffered) {
            int read = CharStreamUtil.readTireless(in, inBuffer, 0, 4);
            if (read == -1) {
                closed = true;
                return -1;
            }
            
            outIndex = 0;
            outBuffered = Base64.decode3Byte(inBuffer, outBuffer, settings);
        }
        
        return outBuffer[outIndex++];
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) return -1;
        if (len == 0) return 0;
        
        int result = 0;
        
        int maxRead = Math.min(outBuffered - outIndex, len);
        System.arraycopy(outBuffer, outIndex, b, off, maxRead);
        outIndex += maxRead;
        off += maxRead;
        len -= maxRead;
        result += maxRead;
        
        while (len > 0) {
            int lenMultiple3 = len + (((len % 3) != 0) ? 3 : 0);
            int inLeft = (lenMultiple3 / 3) * 4;
            maxRead = Math.min(inBuffer.length, inLeft);
            int read = CharStreamUtil.readTireless(in, inBuffer, 0, maxRead);
            if (read == -1) {
                closed = true;
                return -1;
            }
            Base64.decodeChars(inBuffer, 0, read, b, off, settings);
            int decoded = settings.decodedSize(inBuffer);
            result += decoded;
            if (read < maxRead) break;
            off += decoded;
            len -= decoded;
        }
        
        if (result == 0) return -1;
        return result;
    }
    
    @Override
    public int available() {
        return outBuffered - outIndex;
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        in.close();
    }
    
}