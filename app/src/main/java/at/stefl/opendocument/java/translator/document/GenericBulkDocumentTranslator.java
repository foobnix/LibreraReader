package at.stefl.opendocument.java.translator.document;

import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLElementDelegationReader;
import at.stefl.commons.lwxml.reader.LWXMLElementReader;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.writer.LWXMLMultiWriter;
import at.stefl.commons.lwxml.writer.LWXMLNullWriter;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.translator.context.GenericTranslationContext;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;
import at.stefl.opendocument.java.translator.style.DocumentStyle;

public abstract class GenericBulkDocumentTranslator<D extends OpenDocument, S extends DocumentStyle, C extends GenericTranslationContext<D, S>>
        extends GenericDocumentTranslator<D, S, C> {
    
    private final GenericDocumentTranslator<D, S, C> translator;
    
    private final String contentElement;
    private final String subContentElement;
    
    public GenericBulkDocumentTranslator(
            GenericDocumentTranslator<D, S, C> translator,
            String contentElement, String subContentElement) {
        super(translator);
        
        this.translator = translator;
        
        this.contentElement = contentElement;
        this.subContentElement = subContentElement;
    }
    
    public GenericDocumentTranslator<D, S, C> getTranslator() {
        return translator;
    }
    
    @Override
    public void translate(OpenDocument document, LWXMLWriter out,
            TranslationSettings settings) throws IOException {
        if (!(out instanceof LWXMLMultiWriter)) throw new IllegalArgumentException(
                "out is not an instance of LWXMLMultiWriter");
        
        super.translate(document, out, settings);
    }
    
    @Override
    protected void translateContent(LWXMLReader in, LWXMLWriter out, C context)
            throws IOException {
        LWXMLUtil.flushUntilStartElement(in, contentElement);
        
        LWXMLPushbackReader pin = new LWXMLPushbackReader(in);
        LWXMLElementDelegationReader din = new LWXMLElementDelegationReader(pin);
        
        for (LWXMLWriter singleOut : (LWXMLMultiWriter) out) {
            LWXMLUtil.flushUntilStartElement(din, subContentElement);
            LWXMLElementReader ein = din.getElementReader();
            if (singleOut instanceof LWXMLNullWriter) continue;
            pin.unreadEvent(subContentElement);
            contentTranslator.translate(ein, singleOut, context);
        }
    }
    
    @Override
    protected C createContext() {
        return translator.createContext();
    }
    
}