package at.stefl.opendocument.java.odf;

public class UnsupportedMimeTypeException extends RuntimeException {
    
    private static final long serialVersionUID = -7510148676762929160L;
    
    public UnsupportedMimeTypeException(String mimeType) {
        super(mimeType);
    }
    
}