package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.translator.LWXMLDefaultElementTranslator;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.NumberUtil;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class SpaceTranslator extends
        LWXMLDefaultElementTranslator<TranslationContext> {
    
    private static final String COUNT_ATTRIBUTE_NAME = "text:c";
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        int count = NumberUtil.parseInt(
                LWXMLUtil.parseSingleAttribute(in, COUNT_ATTRIBUTE_NAME), 1);
        out.writeCharacters("");
        for (int i = 0; i < count; i++) {
            out.write(' ');
        }
        LWXMLUtil.flushEmptyElement(in);
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        throw new LWXMLIllegalEventException(in);
    }
    
}