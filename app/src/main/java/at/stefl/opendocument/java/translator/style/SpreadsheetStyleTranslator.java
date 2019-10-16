package at.stefl.opendocument.java.translator.style;

import java.io.IOException;

import at.stefl.opendocument.java.css.StyleSheetWriter;

public class SpreadsheetStyleTranslator extends
        DocumentStyleTranslator<SpreadsheetStyle> {
    
    @Override
    public SpreadsheetStyle newDocumentStyle(StyleSheetWriter styleOut)
            throws IOException {
        return new SpreadsheetStyle(styleOut);
    }
    
}