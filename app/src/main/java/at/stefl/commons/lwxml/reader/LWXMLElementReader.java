package at.stefl.commons.lwxml.reader;

import java.io.IOException;
import java.nio.CharBuffer;

import at.stefl.commons.lwxml.LWXMLEvent;

public class LWXMLElementReader extends LWXMLFilterReader {
    
    private boolean closed;
    
    private boolean first = true;
    private int depth;
    
    public LWXMLElementReader(LWXMLReader in) {
        super(in);
    }
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        if (closed) return LWXMLEvent.END_DOCUMENT;
        return in.getCurrentEvent();
    }
    
    @Override
    public LWXMLEvent readEvent() throws IOException {
        if (closed) return LWXMLEvent.END_DOCUMENT;
        
        if (depth < 0) {
            closed = true;
            return LWXMLEvent.END_DOCUMENT;
        }
        
        LWXMLEvent event = in.readEvent();
        
        switch (event) {
        case START_ELEMENT:
            if (!first) depth++;
            break;
        case END_EMPTY_ELEMENT:
        case END_ELEMENT:
            depth--;
            break;
        case END_DOCUMENT:
            closed = true;
            return LWXMLEvent.END_DOCUMENT;
        default:
            break;
        }
        
        first = false;
        return event;
    }
    
    @Override
    public int read() throws IOException {
        if (closed) return -1;
        return in.read();
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        if (closed) return -1;
        return in.read(cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (closed) return -1;
        return in.read(cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        if (closed) return -1;
        return in.read(target);
    }
    
    @Override
    public long skip(long n) throws IOException {
        if (closed) return 0;
        return in.skip(n);
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        in.close();
    }
    
}