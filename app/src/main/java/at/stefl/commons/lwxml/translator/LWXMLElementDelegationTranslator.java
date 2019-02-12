package at.stefl.commons.lwxml.translator;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public abstract class LWXMLElementDelegationTranslator<I extends LWXMLReader, O extends LWXMLWriter, C>
        implements LWXMLElementTranslator<I, O, C> {
    
    private final LWXMLElementTranslator<I, O, C> elementTranslator;
    
    public LWXMLElementDelegationTranslator(
            LWXMLElementTranslator<I, O, C> elementTranslator) {
        this.elementTranslator = elementTranslator;
    }
    
    @Override
    public void translateStartElement(I in, O out, C context)
            throws IOException {
        elementTranslator.translateStartElement(in, out, context);
    }
    
    @Override
    public void translateAttribute(I in, O out, C context) throws IOException {
        elementTranslator.translateAttribute(in, out, context);
    }
    
    @Override
    public void translateAttributeList(I in, O out, C context)
            throws IOException {
        elementTranslator.translateAttributeList(in, out, context);
    }
    
    @Override
    public void translateEndAttributeList(I in, O out, C context)
            throws IOException {
        elementTranslator.translateEndAttributeList(in, out, context);
    }
    
    @Override
    public void translateChildren(I in, O out, C context) throws IOException {
        elementTranslator.translateChildren(in, out, context);
    }
    
    @Override
    public void translateContent(I in, O out, C context) throws IOException {
        elementTranslator.translateContent(in, out, context);
    }
    
    @Override
    public void translateEndElement(I in, O out, C context) throws IOException {
        elementTranslator.translateEndElement(in, out, context);
    }
    
}