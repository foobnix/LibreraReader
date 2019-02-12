package at.stefl.commons.lwxml.writer;

import java.io.IOException;
import java.util.Iterator;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.util.iterator.ArrayIterator;

public class LWXMLMultiWriter extends LWXMLWriter implements
        Iterable<LWXMLWriter> {
    
    private final LWXMLWriter[] outs;
    
    public LWXMLMultiWriter(LWXMLWriter[] outs) {
        int len = outs.length;
        if (len == 0) throw new IllegalArgumentException("empty array");
        
        this.outs = new LWXMLWriter[len];
        System.arraycopy(outs, 0, this.outs, 0, len);
    }
    
    @Override
    public Iterator<LWXMLWriter> iterator() {
        return new ArrayIterator<LWXMLWriter>(outs);
    }
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        return outs[0].getCurrentEvent();
    }
    
    @Override
    public long getCurrentEventNumber() {
        return outs[0].getCurrentEventNumber();
    }
    
    @Override
    public boolean isCurrentEventWritten() {
        return outs[0].isCurrentEventWritten();
    }
    
    @Override
    public void writeEvent(LWXMLEvent event) throws IOException {
        for (int i = 0; i < outs.length; i++) {
            outs[i].writeEvent(event);
        }
    }
    
    @Override
    public void write(int c) throws IOException {
        for (int i = 0; i < outs.length; i++) {
            outs[i].write(c);
        }
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = 0; i < outs.length; i++) {
            outs[i].write(cbuf, off, len);
        }
    }
    
    @Override
    public void flush() throws IOException {
        for (int i = 0; i < outs.length; i++) {
            outs[i].flush();
        }
    }
    
    @Override
    public void close() throws IOException {
        for (int i = 0; i < outs.length; i++) {
            outs[i].close();
        }
    }
    
}