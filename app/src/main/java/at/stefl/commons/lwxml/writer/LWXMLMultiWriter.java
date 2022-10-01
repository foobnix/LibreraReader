package at.stefl.commons.lwxml.writer;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import at.stefl.commons.lwxml.LWXMLEvent;

public class LWXMLMultiWriter extends LWXMLWriter implements
        Iterable<LWXMLWriter> {
    
    private final LWXMLWriter[] outs;
    
    public LWXMLMultiWriter(LWXMLWriter[] outs) {
        int len = outs.length;
        if (len == 0) throw new IllegalArgumentException("empty array");
        
        this.outs = new LWXMLWriter[len];
        System.arraycopy(outs, 0, this.outs, 0, len);
    }
    
    @NonNull
    @Override
    public Iterator<LWXMLWriter> iterator() {
        return Arrays.asList(outs).iterator();
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