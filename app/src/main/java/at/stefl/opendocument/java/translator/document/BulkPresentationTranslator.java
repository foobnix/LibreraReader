package at.stefl.opendocument.java.translator.document;

import at.stefl.opendocument.java.odf.OpenDocumentPresentation;
import at.stefl.opendocument.java.translator.context.PresentationTranslationContext;
import at.stefl.opendocument.java.translator.style.PresentationStyle;

public class BulkPresentationTranslator
        extends
        GenericBulkDocumentTranslator<OpenDocumentPresentation, PresentationStyle, PresentationTranslationContext> {
    
    private static final String CONTENT_ELEMENT = "office:presentation";
    private static final String PAGE_ELEMENT = "draw:page";
    
    public BulkPresentationTranslator() {
        this(new PresentationTranslator());
    }
    
    public BulkPresentationTranslator(PresentationTranslator translator) {
        super(translator, CONTENT_ELEMENT, PAGE_ELEMENT);
    }
    
}