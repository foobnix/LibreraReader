package at.stefl.opendocument.java.translator;

import java.io.IOException;
import java.io.Writer;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public interface ScriptGenerator<C extends TranslationContext> {
    
    // TODO: use script writer
    public void generateScript(Writer out, C context) throws IOException;
    
}