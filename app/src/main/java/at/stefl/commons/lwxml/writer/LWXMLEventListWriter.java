package at.stefl.commons.lwxml.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.io.DividedCharArrayWriter;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.reader.LWXMLReader;

// TODO: implement reader
public class LWXMLEventListWriter extends LWXMLWriter {
    
    // removed Deque because of Android 1.6
    // private Deque<LWXMLEvent> eventList = new LinkedList<LWXMLEvent>();
    // private Deque<String> valueList = new LinkedList<String>();
    private LinkedList<LWXMLEvent> eventList = new LinkedList<LWXMLEvent>();
    private LinkedList<String> valueList = new LinkedList<String>();
    private DividedCharArrayWriter valueWriter = new DividedCharArrayWriter();
    
    private LWXMLEvent lastEvent;
    private long eventNumber;
    private boolean eventWritten;
    
    @Override
    public LWXMLEvent getCurrentEvent() {
        return lastEvent;
    }
    
    @Override
    public long getCurrentEventNumber() {
        return eventNumber;
    }
    
    @Override
    public boolean isCurrentEventWritten() {
        return eventWritten;
    }
    
    public int getEventCount() {
        return eventList.size();
    }
    
    public int getValueCount() {
        return valueList.size();
    }
    
    public List<LWXMLEvent> toEventList() {
        return new ArrayList<LWXMLEvent>(eventList);
    }
    
    public List<String> toValueList() {
        return new ArrayList<String>(valueList);
    }
    
    private void finishLastEvent() {
        if (eventWritten) return;
        
        if (LWXMLEvent.hasValue(lastEvent)) {
            valueList.addLast(valueWriter.toString());
            valueWriter.reset();
        }
        
        eventWritten = true;
    }
    
    @Override
    public void writeEvent(LWXMLEvent event) {
        if (event == null) throw new NullPointerException();
        if (event == LWXMLEvent.END_DOCUMENT) throw new LWXMLWriterException(
                "cannot write event (" + event + ")");
        
        if ((lastEvent != null) && !lastEvent.isFollowingEvent(event)) throw new LWXMLWriterException(
                "given event (" + event + ") cannot follow last event ("
                        + lastEvent + ")");
        
        finishLastEvent();
        eventList.addLast(event);
        
        lastEvent = event;
        eventNumber++;
        eventWritten = !event.hasValue();
    }
    
    private void checkWrite() {
        if (lastEvent == null) throw new LWXMLWriterException(
                "no current event");
        if (!lastEvent.hasValue()) throw new LWXMLWriterException(
                "current event has no value");
        if (eventWritten) throw new LWXMLWriterException(
                "value is already written");
    }
    
    @Override
    public void write(int c) {
        checkWrite();
        valueWriter.write(c);
    }
    
    @Override
    public void write(char[] cbuf) {
        checkWrite();
        valueWriter.write(cbuf);
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) {
        checkWrite();
        valueWriter.write(cbuf, off, len);
    }
    
    @Override
    public void write(String str) {
        checkWrite();
        valueWriter.write(str);
    }
    
    @Override
    public void write(String str, int off, int len) {
        checkWrite();
        valueWriter.write(str, off, len);
    }
    
    @Override
    public LWXMLEventListWriter append(char c) {
        checkWrite();
        valueWriter.append(c);
        return this;
    }
    
    @Override
    public LWXMLEventListWriter append(CharSequence csq) {
        checkWrite();
        valueWriter.append(csq);
        return this;
    }
    
    @Override
    public LWXMLEventListWriter append(CharSequence csq, int start, int end) {
        checkWrite();
        valueWriter.append(csq, start, end);
        return this;
    }
    
    public void write(LWXMLReader in) throws IOException {
        while (true) {
            LWXMLEvent event = in.readEvent();
            if (event == LWXMLEvent.END_DOCUMENT) return;
            
            CharStreamUtil.writeStreamBuffered(in, this);
        }
    }
    
    public void writeTo(LWXMLWriter out) throws IOException {
        finishLastEvent();
        
        Iterator<String> valueIterator = valueList.iterator();
        
        for (LWXMLEvent event : eventList) {
            out.writeEvent(event);
            if (event.hasValue()) out.write(valueIterator.next());
        }
    }
    
    public void reset() {
        eventList.clear();
        valueList.clear();
        valueWriter.reset();
        
        lastEvent = null;
        eventWritten = false;
    }
    
    @Override
    public void flush() {
        finishLastEvent();
    }
    
    @Override
    public void close() {
        flush();
    }
    
}