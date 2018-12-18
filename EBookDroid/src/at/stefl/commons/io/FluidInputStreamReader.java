package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

// TODO: improve
public class FluidInputStreamReader extends Reader {
    
    private final InputStream in;
    
    private final Charset charset;
    private final CharsetDecoder decoder;
    
    private ByteBuffer inBuffer;
    private final CharBuffer outBuffer;
    
    private boolean closed;
    
    public FluidInputStreamReader(InputStream in) {
        this(in, Charset.defaultCharset());
    }
    
    public FluidInputStreamReader(InputStream in, Charset charset) {
        this.in = in;
        
        this.charset = charset;
        this.decoder = charset.newDecoder();
        
        inBuffer = ByteBuffer.allocate((int) Math.ceil(charset.newEncoder()
                .maxBytesPerChar()));
        outBuffer = CharBuffer.allocate((int) Math.ceil(decoder
                .maxCharsPerByte()));
    }
    
    public FluidInputStreamReader(InputStream in, String charset) {
        this(in, Charset.forName(charset));
    }
    
    public Charset getCharset() {
        return charset;
    }
    
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public int read() throws IOException {
        if (closed) return -1;
        
        if (!outBuffer.hasRemaining() || (outBuffer.position() == 0)) {
            decoder.reset();
            outBuffer.clear();
            
            int read;
            CoderResult coderResult;
            
            while (true) {
                read = in.read();
                
                if (read == -1) {
                    closed = true;
                } else {
                    inBuffer.put((byte) read);
                }
                
                inBuffer.flip();
                
                coderResult = decoder.decode(inBuffer, outBuffer, closed);
                
                if (coderResult.isUnderflow()) {
                    if (closed) break;
                    if (outBuffer.position() > 0) break;
                } else if (coderResult.isOverflow()) {
                    break;
                } else {
                    if (inBuffer.limit() == inBuffer.capacity()) coderResult
                            .throwException();
                }
                
                inBuffer.position(inBuffer.limit());
                inBuffer.limit(inBuffer.capacity());
            }
            
            inBuffer.clear();
            outBuffer.flip();
        }
        
        return (outBuffer.hasRemaining()) ? outBuffer.get() : -1;
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return CharStreamUtil.readCharwise(this, cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return CharStreamUtil.readCharwise(this, cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        return CharStreamUtil.readCharwise(this, target);
    }
    
    @Override
    public long skip(long n) throws IOException {
        return CharStreamUtil.skipCharwise(this, n);
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        
        closed = true;
        in.close();
    }
    
}