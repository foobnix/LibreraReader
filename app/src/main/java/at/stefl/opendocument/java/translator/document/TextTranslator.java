package at.stefl.opendocument.java.translator.document;

import at.stefl.opendocument.java.odf.OpenDocumentText;
import at.stefl.opendocument.java.translator.content.TextContentTranslator;
import at.stefl.opendocument.java.translator.context.TextTranslationContext;
import at.stefl.opendocument.java.translator.style.TextStyle;
import at.stefl.opendocument.java.translator.style.TextStyleTranslator;

public class TextTranslator
        extends
        GenericDocumentTranslator<OpenDocumentText, TextStyle, TextTranslationContext> {
    
    public TextTranslator() {
        super(new TextStyleTranslator(), new TextContentTranslator());
    }
    
    @Override
    protected TextTranslationContext createContext() {
        return new TextTranslationContext();
    }
    
}