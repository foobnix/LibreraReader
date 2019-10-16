package at.stefl.opendocument.java.translator.document;

import at.stefl.opendocument.java.odf.OpenDocumentSpreadsheet;
import at.stefl.opendocument.java.translator.content.SpreadsheetContentTranslator;
import at.stefl.opendocument.java.translator.context.SpreadsheetTranslationContext;
import at.stefl.opendocument.java.translator.style.SpreadsheetStyle;
import at.stefl.opendocument.java.translator.style.SpreadsheetStyleTranslator;

public class SpreadsheetTranslator
        extends
        GenericDocumentTranslator<OpenDocumentSpreadsheet, SpreadsheetStyle, SpreadsheetTranslationContext> {
    
    public SpreadsheetTranslator() {
        super(new SpreadsheetStyleTranslator(),
                new SpreadsheetContentTranslator());
    }
    
    @Override
    protected SpreadsheetTranslationContext createContext() {
        return new SpreadsheetTranslationContext();
    }
    
}