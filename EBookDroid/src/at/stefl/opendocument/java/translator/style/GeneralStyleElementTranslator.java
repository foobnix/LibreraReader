package at.stefl.opendocument.java.translator.style;

import at.stefl.opendocument.java.translator.style.property.BorderPropertyTranslator;
import at.stefl.opendocument.java.translator.style.property.LineThroughPropertyTranslator;
import at.stefl.opendocument.java.translator.style.property.MarginPropertyTranslator;
import at.stefl.opendocument.java.translator.style.property.UnderlinePropertyTranslator;
import at.stefl.opendocument.java.translator.style.property.VerticalAlignPropertyTranslator;

public class GeneralStyleElementTranslator extends
        DefaultStyleElementTranslator {
    
    public GeneralStyleElementTranslator() {
        addPropertyTranslator("fo:text-align");
        addDirectionPropertyTranslator("fo:margin");
        addDirectionPropertyTranslator("fo:padding");
        addDirectionPropertyTranslator("fo:border");
        addPropertyTranslator("style:column-width", "width");
        addPropertyTranslator("style:row-height", "height");
        addDirectionPropertyTranslator("fo:border",
                new BorderPropertyTranslator());
        addPropertyTranslator("fo:margin", new MarginPropertyTranslator());
        
        addPropertyTranslator("fo:font-size");
        addPropertyTranslator("style:font-name", "font-family");
        addPropertyTranslator("fo:font-weight");
        addPropertyTranslator("fo:font-style");
        addPropertyTranslator("fo:font-size");
        addPropertyTranslator("fo:text-shadow");
        addPropertyTranslator("fo:color");
        addPropertyTranslator("fo:background-color");
        addPropertyTranslator("style:vertical-align");
        addPropertyTranslator("style:text-underline-style",
                new UnderlinePropertyTranslator());
        addPropertyTranslator("style:text-line-through-style",
                new LineThroughPropertyTranslator());
        addPropertyTranslator("style:text-position",
                new VerticalAlignPropertyTranslator());
        
        addPropertyTranslator("style:width");
        addPropertyTranslator("style:height");
        
        // odp
        // addPropertyTranslator("draw:fill-color", "background-color");
        
        // svg translation
        addPropertyTranslator("draw:fill-color", "fill");
        addPropertyTranslator("svg:stroke-color", "stroke");
        
        addPropertyTranslator("fo:page-width", "width");
        addPropertyTranslator("fo:page-height", "height");
    }
    
}