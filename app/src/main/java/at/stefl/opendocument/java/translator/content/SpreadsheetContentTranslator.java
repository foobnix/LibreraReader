package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.SpreadsheetTranslationContext;

public class SpreadsheetContentTranslator extends
        DefaultContentTranslator<SpreadsheetTranslationContext> {
    
    public SpreadsheetContentTranslator() {
        addElementTranslator("table:tracked-changes",
                new DefaultNothingTranslator());
        addElementTranslator("table:table",
                new SpreadsheetTableTranslator(this));
    }
    
}