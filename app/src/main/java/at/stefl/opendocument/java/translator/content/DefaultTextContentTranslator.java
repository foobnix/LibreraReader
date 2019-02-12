package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public class DefaultTextContentTranslator<C extends TranslationContext> extends
        DefaultContentTranslator<C> {
    
    public DefaultTextContentTranslator() {
        // TODO: translate list style
        addElementTranslator("text:list", "ul");
        addElementTranslator("text:list-item", "li");
        
        addElementTranslator("table:table", new SimpleTableTranslator());
        addElementTranslator("table:table-column",
                new SimpleTableElementTranslator("col"));
        addElementTranslator("table:table-row",
                new SimpleTableElementTranslator("tr"));
        addElementTranslator("table:table-cell",
                new SimpleTableCellTranslator());
        
        DrawingTranslator drawingTranslator = new DrawingTranslator();
        addElementTranslator("draw:rect", drawingTranslator);
        addElementTranslator("draw:ellipse", drawingTranslator);
        // addElementTranslator("draw:path", drawingTranslator);
        // addElementTranslator("draw:custom-shape", drawingTranslator);
    }
    
}