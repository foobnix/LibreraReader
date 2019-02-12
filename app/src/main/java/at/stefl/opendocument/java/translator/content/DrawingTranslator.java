package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class DrawingTranslator extends
        DefaultStyledElementTranslator<TranslationContext> {
    
    public DrawingTranslator() {
        super("svg");
        
        addAttributeTranslator("svg:x", "x");
        addAttributeTranslator("svg:y", "y");
        addAttributeTranslator("svg:width", "width");
        addAttributeTranslator("svg:height", "height");
    }
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        super.translateStartElement(in, out, context);
        out.writeAttribute("xmlns", "http://www.w3.org/2000/svg");
        out.writeAttribute("version", "1.1");
        
        String element = in.readValue();
        if (element.equals("draw:rect")) {
            out.writeStartElement("rect");
        } else if (element.equals("draw:ellipse")) {
            out.writeStartElement("ellipse");
        }
    }
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        out.writeEndElement("rect");
        super.translateEndElement(in, out, context);
    }
    
}