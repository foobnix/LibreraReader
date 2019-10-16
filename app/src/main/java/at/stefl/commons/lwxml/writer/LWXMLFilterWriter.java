package at.stefl.commons.lwxml.writer;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLEvent;

public abstract class LWXMLFilterWriter extends LWXMLWriter {
    
    protected final LWXMLWriter out;
    
    public LWXMLFilterWriter(LWXMLWriter out) {
        if (out == null) throw new NullPointerException();
        
        this.out = out;
    }
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        return out.getCurrentEvent();
    }
    
    @Override
    public long getCurrentEventNumber() {
        return out.getCurrentEventNumber();
    }
    
    @Override
    public boolean isCurrentEventWritten() {
        return out.isCurrentEventWritten();
    }
    
    @Override
    public void flush() throws IOException {
        out.flush();
    }
    
    @Override
    public void close() throws IOException {
        out.close();
    }
    
}