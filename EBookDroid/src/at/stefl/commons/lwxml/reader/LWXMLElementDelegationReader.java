package at.stefl.commons.lwxml.reader;

import java.io.IOException;
import java.nio.CharBuffer;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLUtil;

public class LWXMLElementDelegationReader extends LWXMLFilterReader {
    
    private LWXMLEvent lastEvent;
    
    private LWXMLElementReader ein;
    
    public LWXMLElementDelegationReader(LWXMLReader in) {
        super(in);
    }
    
    public LWXMLElementReader getElementReader() {
        if (ein == null) ein = new LWXMLElementReader(in);
        return ein;
    }
    
    @Override
    public LWXMLEvent readEvent() throws IOException {
        if (ein != null) {
            LWXMLUtil.flush(ein);
            ein = null;
        }
        
        return lastEvent = in.readEvent();
    }
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        return lastEvent;
    }
    
    @Override
    public int read() throws IOException {
        if (ein != null) throw new IllegalStateException();
        return in.read();
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        if (ein != null) throw new IllegalStateException();
        return in.read(cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (ein != null) throw new IllegalStateException();
        return in.read(cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        if (ein != null) throw new IllegalStateException();
        return in.read(target);
    }
    
    @Override
    public long skip(long n) throws IOException {
        if (ein != null) throw new IllegalStateException();
        return in.skip(n);
    }
    
    @Override
    public void close() throws IOException {
        in.close();
        
        if (ein != null) {
            ein.close();
            ein = null;
        }
    }
    
}