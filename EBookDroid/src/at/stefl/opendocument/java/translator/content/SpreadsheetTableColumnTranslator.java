package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.NumberUtil;
import at.stefl.opendocument.java.translator.context.SpreadsheetTranslationContext;

public class SpreadsheetTableColumnTranslator extends
        SpreadsheetTableElementTranslator {
    
    private static final String COLUMNS_REPEATED_ATTRIBUTE_NAME = "table:number-columns-repeated";
    private static final String DEFAULT_CELL_STYLE_ATTRIBUTE_NAME = "table:default-cell-style-name";
    
    private int currentSpan;
    private String currentDefaultCellStyle;
    
    public SpreadsheetTableColumnTranslator() {
        super("col");
        
        addAttributeTranslator(COLUMNS_REPEATED_ATTRIBUTE_NAME, "span");
        
        addParseAttribute(COLUMNS_REPEATED_ATTRIBUTE_NAME);
        addParseAttribute(DEFAULT_CELL_STYLE_ATTRIBUTE_NAME);
    }
    
    public int getCurrentSpan() {
        return currentSpan;
    }
    
    public String getCurrentDefaultCellStyle() {
        return currentDefaultCellStyle;
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        super.translateAttributeList(in, out, context);
        
        currentSpan = NumberUtil.parseInt(
                getCurrentParsedAttribute(COLUMNS_REPEATED_ATTRIBUTE_NAME), 1);
        
        currentDefaultCellStyle = getCurrentParsedAttribute(DEFAULT_CELL_STYLE_ATTRIBUTE_NAME);
    }
    
}