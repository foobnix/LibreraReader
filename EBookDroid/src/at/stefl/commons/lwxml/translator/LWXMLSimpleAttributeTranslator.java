package at.stefl.commons.lwxml.translator;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLAttribute;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public interface LWXMLSimpleAttributeTranslator<C> {
    
    public void translate(LWXMLAttribute in, LWXMLWriter out, C context)
            throws IOException;
    
}