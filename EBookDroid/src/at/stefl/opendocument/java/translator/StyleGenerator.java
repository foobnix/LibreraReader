package at.stefl.opendocument.java.translator;

import java.io.IOException;
import java.io.Writer;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public interface StyleGenerator<C extends TranslationContext> {
    
    // TODO: use style writer
    public void generateStyle(Writer out, C context) throws IOException;
    
}