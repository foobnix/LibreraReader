package at.stefl.opendocument.java.translator.context;

import at.stefl.opendocument.java.odf.OpenDocumentSpreadsheet;
import at.stefl.opendocument.java.translator.style.SpreadsheetStyle;

public class SpreadsheetTranslationContext extends
        GenericTranslationContext<OpenDocumentSpreadsheet, SpreadsheetStyle> {
    
    public SpreadsheetTranslationContext() {
        super(OpenDocumentSpreadsheet.class, SpreadsheetStyle.class);
    }
    
}