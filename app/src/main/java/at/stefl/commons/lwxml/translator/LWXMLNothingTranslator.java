package at.stefl.commons.lwxml.translator;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;

public class LWXMLNothingTranslator<C> extends LWXMLDefaultElementTranslator<C> {
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {
        LWXMLUtil.flushElement(in);
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {}
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, C context) throws IOException {}
    
    @Override
    public void translateChildren(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {}
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {
        throw new LWXMLIllegalEventException(in);
    }
    
}