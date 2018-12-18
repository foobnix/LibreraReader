package at.stefl.opendocument.java.translator.document;

import java.io.IOException;

import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.odf.OpenDocumentPresentation;
import at.stefl.opendocument.java.translator.content.PresentationContentTranslator;
import at.stefl.opendocument.java.translator.context.PresentationTranslationContext;
import at.stefl.opendocument.java.translator.style.PresentationStyle;
import at.stefl.opendocument.java.translator.style.PresentationStyleTranslator;

public class PresentationTranslator
        extends
        GenericDocumentTranslator<OpenDocumentPresentation, PresentationStyle, PresentationTranslationContext> {
    
    public PresentationTranslator() {
        super(new PresentationStyleTranslator(),
                new PresentationContentTranslator());
    }
    
    @Override
    protected void translateMeta(LWXMLWriter out) throws IOException {
        super.translateMeta(out);
        
        out.writeStartElement("meta");
        out.writeAttribute("name", "viewport");
        out.writeAttribute("content",
                "width=device-width; initial-scale=1.0; user-scalable=yes");
        out.writeEndElement("meta");
    }
    
    @Override
    protected PresentationTranslationContext createContext() {
        return new PresentationTranslationContext();
    }
    
}