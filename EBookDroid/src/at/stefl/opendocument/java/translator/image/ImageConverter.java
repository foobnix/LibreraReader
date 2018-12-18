package at.stefl.opendocument.java.translator.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ImageConverter {
    
    public String getFromMimetype();
    
    public String getToMimetype();
    
    public void convert(InputStream in, OutputStream out) throws IOException;
    
    public String convertName(String from);
    
}