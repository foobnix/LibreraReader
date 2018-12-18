package at.stefl.commons.lwxml.writer;

import at.stefl.commons.lwxml.LWXMLException;

public class LWXMLWriterException extends LWXMLException {
    
    private static final long serialVersionUID = -5114969261011687333L;
    
    public LWXMLWriterException() {
        super();
    }
    
    public LWXMLWriterException(String message) {
        super(message);
    }
    
    public LWXMLWriterException(Throwable cause) {
        super(cause);
    }
    
    public LWXMLWriterException(String message, Throwable cause) {
        super(message, cause);
    }
    
}