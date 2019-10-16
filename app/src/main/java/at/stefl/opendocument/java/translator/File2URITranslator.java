package at.stefl.opendocument.java.translator;

import java.io.File;
import java.net.URI;

public interface File2URITranslator {
    
    public static final File2URITranslator DEFAULT = new File2URITranslator() {
        
        @Override
        public URI translate(File file) {
            return file.toURI();
        }
    };
    
    public URI translate(File file);
    
}