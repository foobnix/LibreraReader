package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class TabTranslator extends DefaultElementTranslator<TranslationContext> {
    
    public TabTranslator() {
        super(null);
    }
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        out.writeCharacters("\t");
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        LWXMLUtil.flushEmptyElement(in);
    }
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        throw new LWXMLIllegalEventException(in);
    }
    
}