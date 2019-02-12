package at.stefl.commons.lwxml.translator;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public interface LWXMLElementTranslator<I extends LWXMLReader, O extends LWXMLWriter, C>
        extends LWXMLTranslator<I, O, C> {
    
    public abstract void translateStartElement(I in, O out, C context)
            throws IOException;
    
    public void translateAttribute(I in, O out, C context) throws IOException;
    
    public void translateAttributeList(I in, O out, C context)
            throws IOException;
    
    public void translateEndAttributeList(I in, O out, C context)
            throws IOException;
    
    public void translateChildren(I in, O out, C context) throws IOException;
    
    public void translateContent(I in, O out, C context) throws IOException;
    
    public abstract void translateEndElement(I in, O out, C context)
            throws IOException;
    
}