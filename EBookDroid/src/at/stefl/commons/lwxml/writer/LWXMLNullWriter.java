package at.stefl.commons.lwxml.writer;

import at.stefl.commons.lwxml.LWXMLEvent;

public class LWXMLNullWriter extends LWXMLWriter {
    
    public static final LWXMLNullWriter NULL = new LWXMLNullWriter();
    
    private LWXMLEvent currentEvent;
    private long eventNumber = -1;
    
    public LWXMLNullWriter() {}
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        return currentEvent;
    }
    
    @Override
    public long getCurrentEventNumber() {
        return eventNumber;
    }
    
    @Override
    public boolean isCurrentEventWritten() {
        return true;
    }
    
    @Override
    public void writeEvent(LWXMLEvent event) {
        currentEvent = event;
        eventNumber++;
    }
    
    @Override
    public void write(int c) {}
    
    @Override
    public void write(char[] cbuf, int off, int len) {}
    
}