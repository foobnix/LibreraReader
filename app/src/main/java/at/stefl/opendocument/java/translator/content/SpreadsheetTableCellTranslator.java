package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.NumberUtil;
import at.stefl.opendocument.java.translator.context.SpreadsheetTranslationContext;

public class SpreadsheetTableCellTranslator extends
        SpreadsheetTableElementTranslator {
    
    private static final String STYLE_ATTRIBUTE_NAME = "table:style-name";
    private static final String COLUMNS_REPEATED_ATTRIBUTE_NAME = "table:number-columns-repeated";
    private static final String COLUMNS_SPANNED_ATTRIBUTE_NAME = "table:number-columns-spanned";
    private static final String ROWS_SPANNED_ATTRIBUTE_NAME = "table:number-rows-spanned";
    
    private int currentRepeated;
    private int currentSpan;
    
    public SpreadsheetTableCellTranslator() {
        super("td");
        
        addParseAttribute(STYLE_ATTRIBUTE_NAME);
        addParseAttribute(COLUMNS_REPEATED_ATTRIBUTE_NAME);
        addParseAttribute(COLUMNS_SPANNED_ATTRIBUTE_NAME);
        addParseAttribute(ROWS_SPANNED_ATTRIBUTE_NAME);
    }
    
    public int getCurrentRepeated() {
        return currentRepeated;
    }
    
    public int getCurrentSpan() {
        return currentSpan;
    }
    
    public int getCurrentWidth() {
        return currentRepeated * currentSpan;
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        super.translateAttributeList(in, out, context);
        
        currentRepeated = NumberUtil.parseInt(
                getCurrentParsedAttribute(COLUMNS_REPEATED_ATTRIBUTE_NAME), 1);
        currentSpan = NumberUtil.parseInt(
                getCurrentParsedAttribute(COLUMNS_SPANNED_ATTRIBUTE_NAME), 1);
        int rowspan = NumberUtil.parseInt(
                getCurrentParsedAttribute(ROWS_SPANNED_ATTRIBUTE_NAME), 1);
        
        if (currentSpan > 1) out.writeAttribute("colspan", "" + currentSpan);
        if (rowspan > 1) out.writeAttribute("rowspan", "" + rowspan);
    }
    
}