package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public class SpanTranslator extends
        DefaultStyledContentElementTranslator<TranslationContext> {
    
    public SpanTranslator() {
        super("span");
    }
    
}