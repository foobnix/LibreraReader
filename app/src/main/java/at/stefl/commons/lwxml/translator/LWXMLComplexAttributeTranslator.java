package at.stefl.commons.lwxml.translator;

import java.io.IOException;
import java.util.Map;

import at.stefl.commons.lwxml.writer.LWXMLWriter;

public interface LWXMLComplexAttributeTranslator<C> {
    
    public void translate(Map<String, String> in, LWXMLWriter out, C context)
            throws IOException;
    
}