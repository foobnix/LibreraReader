package at.stefl.opendocument.java.translator.content;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import at.stefl.commons.lwxml.LWXMLAttribute;
import at.stefl.commons.lwxml.translator.LWXMLComplexAttributeTranslator;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;
import at.stefl.opendocument.java.translator.style.DocumentStyle;

public class StyleAttributeTranslator implements
        LWXMLComplexAttributeTranslator<TranslationContext> {
    
    public static LWXMLAttribute translate(String styleName, DocumentStyle style) {
        String reference = style.getStyleReference(styleName);
        // TODO: log
        if (reference == null) return null;
        return new LWXMLAttribute("class", reference);
    }
    
    @Override
    public void translate(Map<String, String> in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        if (in.isEmpty()) return;
        DocumentStyle style = context.getStyle();
        
        out.writeAttribute("class", "");
        
        Iterator<Map.Entry<String, String>> iterator = in.entrySet().iterator();
        while (true) {
            Map.Entry<String, String> attribute = iterator.next();
            String reference = style.getStyleReference(attribute.getValue());
            
            if (reference == null) {
                // TODO: log
                if (!iterator.hasNext()) break;
            } else {
                out.write(reference);
                if (!iterator.hasNext()) break;
                out.write(" ");
            }
        }
    }
    
}