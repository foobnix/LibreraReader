package at.stefl.commons.lwxml.writer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.lwxml.LWXMLAttribute;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLIllegalEventException;

public abstract class LWXMLWriter extends Writer {
    
    protected final CharStreamUtil streamUtil = new CharStreamUtil();
    
    public abstract LWXMLEvent getCurrentEvent();
    
    public abstract long getCurrentEventNumber();
    
    public abstract boolean isCurrentEventWritten();
    
    public abstract void writeEvent(LWXMLEvent event) throws IOException;
    
    private void checkEndAttributeList() throws IOException {
        if (getCurrentEvent() == null) return;
        
        switch (getCurrentEvent()) {
        case START_ELEMENT:
        case ATTRIBUTE_VALUE:
            writeEvent(LWXMLEvent.END_ATTRIBUTE_LIST);
        default:
            break;
        }
    }
    
    public void writeProcessingInstruction(String target, String data)
            throws IOException {
        if (target == null) throw new NullPointerException();
        if (data == null) throw new NullPointerException();
        
        checkEndAttributeList();
        
        writeEvent(LWXMLEvent.PROCESSING_INSTRUCTION_TARGET);
        write(target);
        writeEvent(LWXMLEvent.PROCESSING_INSTRUCTION_DATA);
        write(data);
    }
    
    public void writeStartElement(String name) throws IOException {
        if (name == null) throw new NullPointerException();
        
        checkEndAttributeList();
        
        writeEvent(LWXMLEvent.START_ELEMENT);
        write(name);
    }
    
    public void writeEmptyStartElement(String name) throws IOException {
        writeStartElement(name);
        writeEvent(LWXMLEvent.END_ATTRIBUTE_LIST);
    }
    
    public void writeEmptyElement(String name) throws IOException {
        writeEmptyStartElement(name);
        writeEndEmptyElement();
    }
    
    public void writeAttribute(LWXMLAttribute attribute) throws IOException {
        if (attribute == null) throw new NullPointerException();
        
        writeAttribute(attribute.getName(), attribute.getValue());
    }
    
    public void writeAttribute(String name, String value) throws IOException {
        if (name == null) throw new NullPointerException();
        if (value == null) throw new NullPointerException();
        
        writeEvent(LWXMLEvent.ATTRIBUTE_NAME);
        write(name);
        writeEvent(LWXMLEvent.ATTRIBUTE_VALUE);
        write(value);
    }
    
    public void writeEndEmptyElement() throws IOException {
        checkEndAttributeList();
        
        if (getCurrentEvent() != LWXMLEvent.END_ATTRIBUTE_LIST) throw new LWXMLIllegalEventException(
                LWXMLEvent.END_EMPTY_ELEMENT);
        writeEvent(LWXMLEvent.END_EMPTY_ELEMENT);
    }
    
    public void writeEndElement(String name) throws IOException {
        if (name == null) throw new NullPointerException();
        
        checkEndAttributeList();
        
        if (getCurrentEvent() == LWXMLEvent.END_ATTRIBUTE_LIST) {
            writeEvent(LWXMLEvent.END_EMPTY_ELEMENT);
        } else {
            writeEvent(LWXMLEvent.END_ELEMENT);
            write(name);
        }
    }
    
    public void writeCharacters(String value) throws IOException {
        if (value == null) throw new NullPointerException();
        
        checkEndAttributeList();
        
        writeEvent(LWXMLEvent.CHARACTERS);
        write(value);
    }
    
    public void writeCDATA(String value) throws IOException {
        if (value == null) throw new NullPointerException();
        
        checkEndAttributeList();
        
        writeEvent(LWXMLEvent.CDATA);
        write(value);
    }
    
    public void writeComment(String value) throws IOException {
        if (value == null) throw new NullPointerException();
        
        checkEndAttributeList();
        
        writeEvent(LWXMLEvent.COMMENT);
        write(value);
    }
    
    @Override
    public abstract void write(int c) throws IOException;
    
    @Override
    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }
    
    @Override
    public abstract void write(char[] cbuf, int off, int len)
            throws IOException;
    
    @Override
    public void write(String str) throws IOException {
        CharStreamUtil.writeCharwise(this, str);
    }
    
    @Override
    public void write(String str, int off, int len) throws IOException {
        CharStreamUtil.writeCharwise(this, str, off, len);
    }
    
    public int write(Reader in) throws IOException {
        return streamUtil.writeStream(in, this);
    }
    
    @Override
    public LWXMLWriter append(char c) throws IOException {
        write(c);
        return this;
    }
    
    @Override
    public LWXMLWriter append(CharSequence csq) throws IOException {
        CharStreamUtil.appendCharwise(this, csq);
        return this;
    }
    
    @Override
    public LWXMLWriter append(CharSequence csq, int start, int end)
            throws IOException {
        CharStreamUtil.appendCharwise(this, csq, start, end);
        return this;
    }
    
    @Override
    public void flush() throws IOException {}
    
    @Override
    public void close() throws IOException {}
    
}