package at.stefl.commons.lwxml.translator;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public interface LWXMLTranslator<I extends LWXMLReader, O extends LWXMLWriter, C> {
    
    public void translate(I in, O out, C context) throws IOException;
    
}