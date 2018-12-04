package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.TranslationContext;

public abstract class DefaultStyledElementTranslator<C extends TranslationContext>
        extends DefaultElementTranslator<C> {
    
    public DefaultStyledElementTranslator(String elementName,
            StyleAttribute... attributes) {
        super(elementName);
        
        if (attributes.length == 0) attributes = StyleAttribute.values();
        
        String[] names = new String[attributes.length];
        for (int i = 0; i < names.length; i++)
            names[i] = attributes[i].getName();
        
        addComplexAttributeTranslator(createStyleAttributeTranslator(), names);
    }
    
    protected StyleAttributeTranslator createStyleAttributeTranslator() {
        return new StyleAttributeTranslator();
    }
    
}