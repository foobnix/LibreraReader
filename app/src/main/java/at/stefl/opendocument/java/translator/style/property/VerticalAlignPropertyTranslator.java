package at.stefl.opendocument.java.translator.style.property;

import at.stefl.opendocument.java.css.StyleProperty;

public class VerticalAlignPropertyTranslator implements PropertyTranslator {
    
    private static final String STYLE_NAME = "vertical-align";
    
    @Override
    public StyleProperty translate(String name, String value) {
        int spaceIndex = value.indexOf(' ');
        if (spaceIndex != -1) value = value.substring(0, spaceIndex);
        return new StyleProperty(STYLE_NAME, value);
    }
    
}