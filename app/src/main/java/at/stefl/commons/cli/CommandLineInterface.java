package at.stefl.commons.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface CommandLineInterface {
    
    public InputStream getInputStream() throws IOException;
    
    public OutputStream getOutputStream() throws IOException;
    
    public void close() throws IOException;
    
}