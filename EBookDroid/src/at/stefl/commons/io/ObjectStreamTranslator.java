package at.stefl.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ObjectStreamTranslator<T> {
    
    public T read(InputStream in) throws IOException;
    
    public void write(T object, OutputStream out) throws IOException;
    
}