package at.stefl.opendocument.java.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import at.stefl.opendocument.java.translator.File2URITranslator;

public class DefaultFileCache extends AbstractFileCache {
    
    private final File directory;
    
    public DefaultFileCache(String directory) {
        this(new File(directory), File2URITranslator.DEFAULT);
    }
    
    public DefaultFileCache(String directory, File2URITranslator uriTranslator) {
        this(new File(directory), uriTranslator);
    }
    
    public DefaultFileCache(File directory, File2URITranslator uriTranslator) {
        super(uriTranslator);
        
        if (!directory.exists() && !directory.mkdir()) throw new IllegalStateException();
        if (!directory.isDirectory()) throw new IllegalArgumentException();
        this.directory = directory;
    }
    
    public File getDirectory() {
        return directory;
    }
    
    private File file(String name) {
        return new File(directory, name);
    }
    
    @Override
    public boolean exists(String name) {
        return file(name).exists();
    }
    
    @Override
    public File getFile(String name) throws FileNotFoundException {
        File file = file(name);
        if (!file.exists()) throw new FileNotFoundException();
        return file;
    }
    
    @Override
    public RandomAccessFile getRandomAccessFile(String name, String mode)
            throws FileNotFoundException {
        return new RandomAccessFile(getFile(name), mode);
    }
    
    @Override
    public InputStream getInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(getFile(name));
    }
    
    @Override
    public OutputStream getOutputStream(String name)
            throws FileNotFoundException {
        return new FileOutputStream(getFile(name));
    }
    
    @Override
    public FileChannel getChannel(String name, String mode)
            throws FileNotFoundException {
        return getRandomAccessFile(name, mode).getChannel();
    }
    
    @Override
    public File create(String name) throws IOException {
        File file = file(name);
        new FileOutputStream(file).close();
        return file;
    }
    
    @Override
    public File move(String from, String to) throws FileNotFoundException {
        File file = file(to);
        getFile(from).renameTo(file(to));
        return file;
    }
    
    @Override
    public void delete(String name) throws FileNotFoundException {
        File file = getFile(name);
        if (file == null) throw new FileNotFoundException();
        getFile(name).delete();
    }
    
    @Override
    public void clear() {
        directory.delete();
    }
    
}