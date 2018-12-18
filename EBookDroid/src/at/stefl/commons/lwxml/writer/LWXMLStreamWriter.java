package at.stefl.commons.lwxml.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLIllegalEventException;

// TODO: use xml escaping encoder
public class LWXMLStreamWriter extends LWXMLWriter {
    
    // TODO: remove
    private static final String DEFAULT_CHARSET = "UTF-8";
    
    private boolean closed;
    private Writer out;
    
    private LWXMLEvent lastEvent;
    private long eventNumber = -1;
    private boolean eventWritten;
    
    // TODO: remove
    public LWXMLStreamWriter(OutputStream out) {
        this(out, Charset.forName(DEFAULT_CHARSET));
    }
    
    // TODO: remove
    public LWXMLStreamWriter(OutputStream out, Charset charset) {
        this(new BufferedWriter(new OutputStreamWriter(out, charset)));
    }
    
    public LWXMLStreamWriter(Writer out) {
        this.out = out;
    }
    
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
    
    private void finishLastEvent() throws IOException {
        finishLastEvent(null);
    }
    
    private void finishLastEvent(LWXMLEvent nextEvent) throws IOException {
        if (lastEvent == null) return;
        if (eventWritten) return;
        
        switch (lastEvent) {
        case PROCESSING_INSTRUCTION_DATA:
            out.write("?>");
            break;
        case COMMENT:
            out.write("-->");
            break;
        case END_ELEMENT:
            out.write(">");
            break;
        case ATTRIBUTE_VALUE:
            out.write("\"");
            break;
        case END_ATTRIBUTE_LIST:
            if ((nextEvent != null)
                    && (nextEvent != LWXMLEvent.END_EMPTY_ELEMENT)) out
                    .write(">");
            break;
        case CDATA:
            out.write("]]>");
            break;
        default:
            break;
        }
        
        eventWritten = true;
    }
    
    @Override
    public void writeEvent(LWXMLEvent event) throws IOException {
        if (closed) throw new LWXMLWriterException("already closed");
        
        if (event == null) throw new NullPointerException();
        if (event == LWXMLEvent.END_DOCUMENT) throw new LWXMLWriterException(
                "cannot write event (" + event + ")");
        
        if ((lastEvent != null) && !lastEvent.isFollowingEvent(event)) throw new LWXMLWriterException(
                "given event (" + event + ") cannot follow last event ("
                        + lastEvent + ")");
        
        finishLastEvent(event);
        
        eventWritten = false;
        
        switch (event) {
        case PROCESSING_INSTRUCTION_TARGET:
            out.write("<?");
            break;
        case PROCESSING_INSTRUCTION_DATA:
            out.write(" ");
            break;
        case COMMENT:
            out.write("<!--");
            break;
        case START_ELEMENT:
            out.write("<");
            break;
        case END_EMPTY_ELEMENT:
            out.write("/>");
            eventWritten = true;
            break;
        case END_ELEMENT:
            out.write("</");
            break;
        case ATTRIBUTE_NAME:
            out.write(" ");
            break;
        case ATTRIBUTE_VALUE:
            out.write("=\"");
            break;
        case END_ATTRIBUTE_LIST:
            break;
        case CHARACTERS:
            break;
        case CDATA:
            out.write("<![CDATA[");
            break;
        default:
            throw new LWXMLIllegalEventException(event);
        }
        
        lastEvent = event;
        eventNumber++;
    }
    
    private void checkWrite() {
        if (closed) throw new LWXMLWriterException("already closed");
        if (lastEvent == null) throw new LWXMLWriterException(
                "no current event");
        if (!lastEvent.hasValue()) throw new LWXMLWriterException(
                "current event has no value");
        if (eventWritten) throw new LWXMLWriterException(
                "value already written");
    }
    
    @Override
    public void write(int c) throws IOException {
        checkWrite();
        out.write(c);
    }
    
    @Override
    public void write(char[] cbuf) throws IOException {
        checkWrite();
        out.write(cbuf);
    }
    
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        checkWrite();
        out.write(cbuf, off, len);
    }
    
    @Override
    public void write(String str) throws IOException {
        checkWrite();
        out.write(str);
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        checkWrite();
        out.write(str, off, len);
    }
    
    @Override
    public LWXMLWriter append(char c) throws IOException {
        checkWrite();
        out.append(c);
        return this;
    }
    
    @Override
    public LWXMLWriter append(CharSequence csq) throws IOException {
        checkWrite();
        out.append(csq);
        return this;
    }
    
    @Override
    public LWXMLWriter append(CharSequence csq, int start, int end)
            throws IOException {
        checkWrite();
        out.append(csq, start, end);
        return this;
    }
    
    @Override
    public void flush() throws IOException {
        if (closed) return;
        
        out.flush();
    }
    
    @Override
    public void close() throws IOException {
        if (closed) return;
        
        try {
            finishLastEvent();
            out.flush();
            out.close();
        } finally {
            closed = true;
        }
    }
    
}