package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public class BookmarkTranslator extends
        DefaultElementTranslator<TranslationContext> {
    
    public static final String START = "text:bookmark-start";
    public static final String END = "text:bookmark-end";
    
    public BookmarkTranslator() {
        super("a");
        
        addAttributeTranslator("text:name", "id");
    }
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {
        String element = in.readValue();
        if (element.equals(START)) {
            super.translateStartElement(in, out, context);
            super.translateAttributeList(in, out, context);
            super.translateEndAttributeList(in, out, context);
            LWXMLUtil.flushEmptyElement(in);
            super.translateEndElement(in, out, context);
        } else if (element.equals(END)) {
            super.translateEndElement(in, out, context);
        } else {
            throw new IllegalStateException();
        }
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, TranslationContext context) throws IOException {}
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            TranslationContext context) throws IOException {}
    
}