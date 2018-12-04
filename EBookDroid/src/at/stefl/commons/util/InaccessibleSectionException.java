package at.stefl.commons.util;

public class InaccessibleSectionException extends RuntimeException {
    
    private static final long serialVersionUID = 6515283105006148955L;
    
    public InaccessibleSectionException() {
        super("inaccessible section");
    }
    
    public InaccessibleSectionException(String message) {
        super("inaccessible section: " + message);
    }
    
}