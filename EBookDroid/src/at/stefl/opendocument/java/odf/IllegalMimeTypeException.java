package at.stefl.opendocument.java.odf;

public class IllegalMimeTypeException extends RuntimeException {
    
    private static final long serialVersionUID = 1436393976168401887L;
    
    public IllegalMimeTypeException(String mimeType) {
        super(mimeType);
    }
    
}