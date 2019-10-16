package at.stefl.opendocument.java.translator.style.property;

import at.stefl.opendocument.java.css.StyleProperty;

public interface PropertyTranslator {
    
    public StyleProperty translate(String name, String value);
    
}