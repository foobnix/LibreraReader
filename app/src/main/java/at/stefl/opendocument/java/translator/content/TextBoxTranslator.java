package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class TextBoxTranslator extends
        DefaultBlockTranslator<TranslationContext> {
    
    public TextBoxTranslator() {
        super(null);
    }
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {}
    
}