package at.stefl.commons.lwxml;

public class LWXMLException extends RuntimeException {
    
    private static final long serialVersionUID = -3390546202583727574L;
    
    public LWXMLException() {}
    
    public LWXMLException(String message) {
        super(message);
    }
    
    public LWXMLException(Throwable cause) {
        super(cause);
    }
    
    public LWXMLException(String message, Throwable cause) {
        super(message, cause);
    }
    
}