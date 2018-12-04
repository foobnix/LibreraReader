package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.NumberUtil;
import at.stefl.opendocument.java.translator.context.SpreadsheetTranslationContext;

public class SpreadsheetTableRowTranslator extends
        SpreadsheetTableElementTranslator {
    
    private static final String ROWS_REPEATED_ATTRIBUTE_NAME = "table:number-rows-repeated";
    
    private int currentRepeated;
    
    public SpreadsheetTableRowTranslator() {
        super("tr");
        
        addParseAttribute(ROWS_REPEATED_ATTRIBUTE_NAME);
    }
    
    public int getCurrentRepeated() {
        return currentRepeated;
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        super.translateAttributeList(in, out, context);
        
        currentRepeated = NumberUtil.parseInt(
                getCurrentParsedAttribute(ROWS_REPEATED_ATTRIBUTE_NAME), 1);
    }
    
}