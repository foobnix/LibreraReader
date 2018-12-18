package at.stefl.opendocument.java.odf;

public class UnsupportedEncryptionException extends RuntimeException {
    
    private static final long serialVersionUID = -7187929739114486495L;
    
    public UnsupportedEncryptionException() {
        super();
    }
    
    public UnsupportedEncryptionException(String message) {
        super(message);
    }
    
    public UnsupportedEncryptionException(Throwable cause) {
        super(cause);
    }
    
    public UnsupportedEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
    
}