package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class LinkTranslator extends
        DefaultStyledContentElementTranslator<TranslationContext> {
    
    private static final String HREF_ATTRIBUTE = "xlink:href";
    
    public LinkTranslator() {
        super("a");
        
        addAttributeTranslator(HREF_ATTRIBUTE, "href");
        addParseAttribute(HREF_ATTRIBUTE);
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        super.translateAttributeList(in, out, context);
        
        String link = getCurrentParsedAttribute(HREF_ATTRIBUTE);
        if (link == null) return;
        if (!link.trim().startsWith("#")) return;
        out.writeAttribute("target", "_self");
    }
    
}