package at.stefl.commons.lwxml.translator;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLAttribute;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public class LWXMLStaticAttributeTranslator<C> implements
        LWXMLSimpleAttributeTranslator<C> {
    
    private final String newAttributeName;
    
    public LWXMLStaticAttributeTranslator(String newAttributeName) {
        if (newAttributeName == null) throw new NullPointerException();
        
        this.newAttributeName = newAttributeName;
    }
    
    @Override
    public void translate(LWXMLAttribute in, LWXMLWriter out, C context)
            throws IOException {
        out.writeAttribute(newAttributeName, in.getValue());
    }
    
}