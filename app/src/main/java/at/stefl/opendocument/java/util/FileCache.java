package at.stefl.opendocument.java.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.channels.FileChannel;

public interface FileCache {
    
    public abstract boolean exists(String name);
    
    public abstract File getFile(String name) throws FileNotFoundException;
    
    public abstract RandomAccessFile getRandomAccessFile(String name,
            String mode) throws FileNotFoundException;
    
    public abstract URI getURI(String name) throws FileNotFoundException;
    
    public abstract InputStream getInputStream(String name)
            throws FileNotFoundException;
    
    public abstract OutputStream getOutputStream(String name)
            throws FileNotFoundException;
    
    public abstract FileChannel getChannel(String name, String mode)
            throws FileNotFoundException;
    
    public abstract String create() throws IOException;
    
    public abstract String create(InputStream in) throws IOException;
    
    public abstract File create(String name) throws IOException;
    
    public abstract File create(String name, InputStream in) throws IOException;
    
    public abstract File move(String from, String to)
            throws FileNotFoundException;
    
    public abstract void delete(String name) throws FileNotFoundException;
    
    public abstract void clear();
    
}