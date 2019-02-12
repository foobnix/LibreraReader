package at.stefl.commons.lwxml;

import at.stefl.commons.lwxml.reader.LWXMLReader;

public class LWXMLIllegalEventException extends LWXMLException {
    
    private static final long serialVersionUID = -3888861098441580428L;
    
    public LWXMLIllegalEventException(LWXMLEvent event) {
        super(event.toString());
    }
    
    public LWXMLIllegalEventException(LWXMLReader in) {
        this(in.getCurrentEvent());
    }
    
}