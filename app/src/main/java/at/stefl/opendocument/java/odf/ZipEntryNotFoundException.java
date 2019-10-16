package at.stefl.opendocument.java.odf;

import java.io.FileNotFoundException;

public class ZipEntryNotFoundException extends FileNotFoundException {
    
    private static final long serialVersionUID = 3481758992640534280L;
    
    public ZipEntryNotFoundException() {}
    
    public ZipEntryNotFoundException(String message) {
        super(message);
    }
    
}