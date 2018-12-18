package at.stefl.opendocument.java.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.opendocument.java.translator.File2URITranslator;

public abstract class AbstractFileCache implements FileCache {
    
    private static String iterateName(int n) {
        return "" + n;
    }
    
    private File2URITranslator uriTranslator;
    
    private int counter;
    
    public AbstractFileCache(File2URITranslator uriTranslator) {
        this.uriTranslator = uriTranslator;
    }
    
    public File2URITranslator getURITranslator() {
        return uriTranslator;
    }
    
    @Override
    public URI getURI(String name) throws FileNotFoundException {
        File file = getFile(name);
        return uriTranslator.translate(file);
    }
    
    public void setURITranslator(File2URITranslator uriTranslator) {
        this.uriTranslator = uriTranslator;
    }
    
    @Override
    public String create() throws IOException {
        String name;
        while (exists(name = iterateName(counter++)))
            ;
        create(name);
        return name;
    }
    
    @Override
    public String create(InputStream in) throws IOException {
        String name = create();
        ByteStreamUtil.writeStreamBuffered(in, getOutputStream(name));
        return name;
    }
    
    @Override
    public File create(String name, InputStream in) throws IOException {
        File file = create(name);
        ByteStreamUtil.writeStreamBuffered(in, getOutputStream(name));
        return file;
    }
    
}