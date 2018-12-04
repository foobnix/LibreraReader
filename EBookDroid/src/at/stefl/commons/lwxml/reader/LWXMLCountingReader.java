package at.stefl.commons.lwxml.reader;

import java.io.IOException;
import java.nio.CharBuffer;

import at.stefl.commons.lwxml.LWXMLEvent;

public class LWXMLCountingReader extends LWXMLFilterReader {
    
    private final int[] counts = new int[LWXMLEvent.values().length];
    
    public LWXMLCountingReader(LWXMLReader in) {
        super(in);
    }
    
    public int getCount(LWXMLEvent event) {
        return counts[event.ordinal()];
    }
    
    @Override
    public LWXMLEvent readEvent() throws IOException {
        LWXMLEvent event = in.readEvent();
        counts[event.ordinal()]++;
        return event;
    }
    
    @Override
    public int read() throws IOException {
        return in.read();
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        return in.read(cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return in.read(cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        return in.read(target);
    }
    
}