package at.stefl.opendocument.java.translator.context;

import at.stefl.opendocument.java.odf.OpenDocumentText;
import at.stefl.opendocument.java.translator.style.TextStyle;

public class TextTranslationContext extends
        GenericTranslationContext<OpenDocumentText, TextStyle> {
    
    public TextTranslationContext() {
        super(OpenDocumentText.class, TextStyle.class);
    }
    
}