package at.stefl.opendocument.java.translator.style;

public abstract class DefaultStyleTranslator<T extends DocumentStyle> extends
        DocumentStyleTranslator<T> {
    
    private static final String DEFAULT_STYLE_ELEMENT_NAME = "style:default-style";
    private static final String GENERAL_STYLE_ELEMENT_NAME = "style:style";
    private static final String PAGE_LAYOUT_ELEMENT_NAME = "style:page-layout";
    
    public DefaultStyleTranslator() {
        GeneralStyleElementTranslator generalStyleElementTranslator = new GeneralStyleElementTranslator();
        
        addElementTranslator(DEFAULT_STYLE_ELEMENT_NAME,
                generalStyleElementTranslator);
        addElementTranslator(GENERAL_STYLE_ELEMENT_NAME,
                generalStyleElementTranslator);
        addElementTranslator(PAGE_LAYOUT_ELEMENT_NAME,
                generalStyleElementTranslator);
    }
    
}