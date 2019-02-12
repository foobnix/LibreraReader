package at.stefl.opendocument.java.translator.context;

import at.stefl.opendocument.java.odf.OpenDocumentPresentation;
import at.stefl.opendocument.java.translator.style.PresentationStyle;

public class PresentationTranslationContext extends
        GenericTranslationContext<OpenDocumentPresentation, PresentationStyle> {
    
    public PresentationTranslationContext() {
        super(OpenDocumentPresentation.class, PresentationStyle.class);
    }
    
}