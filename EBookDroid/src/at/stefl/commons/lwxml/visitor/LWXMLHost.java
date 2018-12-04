package at.stefl.commons.lwxml.visitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.reader.LWXMLReader;

public abstract class LWXMLHost {
    
    private final List<LWXMLVisitor> visitors = new ArrayList<LWXMLVisitor>();
    
    private boolean hosting;
    
    public boolean isHosting() {
        return hosting;
    }
    
    public synchronized boolean hasVisitors() {
        return !visitors.isEmpty();
    }
    
    public synchronized void addVisitor(LWXMLVisitor visitor) {
        visitor.visitHost(this);
        visitors.add(visitor);
    }
    
    public synchronized void removeVisitor(LWXMLVisitor visitor) {
        visitors.remove(visitor);
    }
    
    public synchronized void host(LWXMLReader reader) throws IOException {
        hosting = true;
        
        try {
            while (hasVisitors()) {
                LWXMLEvent event = reader.readEvent();
                
                if (event == null) {
                    fireVisitEnd();
                    return;
                }
                
                fireEvent(event, reader);
            }
        } finally {
            hosting = false;
        }
    }
    
    protected synchronized void fireEvent(LWXMLEvent event, LWXMLReader reader)
            throws IOException {
        String value1 = null;
        String value2 = null;
        
        if (event.hasValue()) value1 = reader.readValue();
        if (event.hasFollowingValue()) value2 = reader.readFollowingValue();
        
        switch (event) {
        case PROCESSING_INSTRUCTION_TARGET:
            fireVisitProcessingInstruction(value1, value2);
            break;
        case COMMENT:
            fireVisitComment(value1);
            break;
        case START_ELEMENT:
            fireVisitStartElement(value1);
            break;
        case END_ATTRIBUTE_LIST:
            fireVisitEndAttributeList();
            break;
        case END_ELEMENT:
            fireVisitEndElement(value1);
            break;
        case ATTRIBUTE_NAME:
            fireVisitAttribute(value1, value2);
            break;
        case CHARACTERS:
            fireVisitCharacters(value1);
            break;
        case CDATA:
            fireVisitCDATA(value1);
            break;
        default:
            throw new IllegalStateException("unsupported event");
        }
    }
    
    protected synchronized void fireVisitEnd() {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitEnd();
        }
    }
    
    protected synchronized void fireVisitProcessingInstruction(String target,
            String data) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitProcessingInstruction(target, data);
        }
    }
    
    protected synchronized void fireVisitComment(String text) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitComment(text);
        }
    }
    
    protected synchronized void fireVisitStartElement(String name) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitStartElement(name);
        }
    }
    
    protected synchronized void fireVisitEndAttributeList() {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitEndAttributeList();
        }
    }
    
    protected synchronized void fireVisitEndElement(String name) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitEndElement(name);
        }
    }
    
    protected synchronized void fireVisitAttribute(String name, String value) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitAttribute(name, value);
        }
    }
    
    protected synchronized void fireVisitCharacters(String text) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitCharacters(text);
        }
    }
    
    protected synchronized void fireVisitCDATA(String text) {
        for (LWXMLVisitor visitor : visitors) {
            visitor.visitCDATA(text);
        }
    }
    
}