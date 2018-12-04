package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public class TableElementTranslator<C extends TranslationContext> extends
        DefaultStyledElementTranslator<C> {
    
    public TableElementTranslator(String elementName) {
        super(elementName);
    }
    
}