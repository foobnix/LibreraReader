package at.stefl.commons.lwxml.reader;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.io.DividedCharArrayWriter;
import at.stefl.commons.lwxml.LWXMLEvent;

// TODO: provide hasNext()
// TODO: improve causing memory allocation
// TODO: improve getCurrentEvent()
// TODO: improve exception handling (reading on no-value event)
// TODO: do not extend Reader, just provide a value reader
public abstract class LWXMLReader extends Reader {
    
    private DividedCharArrayWriter tmpOut = new DividedCharArrayWriter();
    
    public abstract LWXMLEvent getCurrentEvent();
    
    // TODO: improve concept
    public abstract long getCurrentEventNumber();
    
    public abstract LWXMLEvent readEvent() throws IOException;
    
    // TODO: replace String
    // TODO: make use of a buffer pool
    public String readValue() throws IOException {
        if (!LWXMLEvent.hasValue(getCurrentEvent())) return null;
        tmpOut.write(this);
        String result = tmpOut.toString();
        tmpOut.reset();
        return result;
    }
    
    // TODO: throw exception
    public String readFollowingValue() throws IOException {
        if (!LWXMLEvent.hasFollowingValue(getCurrentEvent())) return null;
        readEvent();
        return readValue();
    }
    
    @Override
    public abstract int read() throws IOException;
    
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
    public boolean ready() throws IOException {
        return false;
    }
    
    @Override
    public boolean markSupported() {
        return false;
    }
    
    @Override
    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark/reset not supported");
    }
    
    @Override
    public void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }
    
    @Override
    public void close() throws IOException {}
    
}