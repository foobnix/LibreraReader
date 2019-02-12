package at.stefl.commons.lwxml.reader;

import java.io.IOException;
import java.nio.CharBuffer;

import at.stefl.commons.lwxml.LWXMLEvent;

public class LWXMLFlatReader extends LWXMLFilterReader {
    
    private LWXMLEvent lastEvent;
    private int depth;
    private boolean endEmptyElement;
    
    public LWXMLFlatReader(LWXMLReader in) {
        super(in);
    }
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        if (endEmptyElement) return LWXMLEvent.END_EMPTY_ELEMENT;
        return lastEvent;
    }
    
    @Override
    public LWXMLEvent readEvent() throws IOException {
        endEmptyElement = false;
        
        boolean collapseEndElement = true;
        LWXMLEvent event;
        
        loop:
        while (true) {
            event = in.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                depth++;
            case ATTRIBUTE_NAME:
            case ATTRIBUTE_VALUE:
            case END_ATTRIBUTE_LIST:
                if (depth == 1) break loop;
                break;
            case END_EMPTY_ELEMENT:
            case END_ELEMENT:
                depth--;
                
                if (depth < 0) {
                    depth = 0;
                    collapseEndElement = false;
                }
                
                break;
            default:
                break;
            }
            
            if (depth <= 0) break;
            collapseEndElement = false;
        }
        
        if (collapseEndElement && (event == LWXMLEvent.END_ELEMENT)) {
            endEmptyElement = true;
            return LWXMLEvent.END_EMPTY_ELEMENT;
        }
        
        return lastEvent = event;
    }
    
    @Override
    public int read() throws IOException {
        if (endEmptyElement) return -1;
        return in.read();
    }
    
    @Override
    public int read(char[] cbuf) throws IOException {
        if (endEmptyElement) return -1;
        return in.read(cbuf);
    }
    
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (endEmptyElement) return -1;
        return in.read(cbuf, off, len);
    }
    
    @Override
    public int read(CharBuffer target) throws IOException {
        if (endEmptyElement) return -1;
        return in.read(target);
    }
    
    @Override
    public long skip(long n) throws IOException {
        if (endEmptyElement) return 0;
        return in.skip(n);
    }
    
}