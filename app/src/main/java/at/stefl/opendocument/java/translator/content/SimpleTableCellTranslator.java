package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public class SimpleTableCellTranslator extends
        TableElementTranslator<TranslationContext> {
    
    public SimpleTableCellTranslator() {
        super("td");
        
        addAttributeTranslator("table:number-columns-spanned", "colspan");
        addAttributeTranslator("table:number-rows-spanned", "rowspan");
    }
    
}