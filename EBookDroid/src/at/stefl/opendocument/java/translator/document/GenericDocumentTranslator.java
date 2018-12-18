package at.stefl.opendocument.java.translator.document;

import java.io.IOException;

import at.stefl.commons.io.CountingInputStream;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLElementReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.css.StyleSheetWriter;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.translator.content.ContentTranslator;
import at.stefl.opendocument.java.translator.context.GenericTranslationContext;
import at.stefl.opendocument.java.translator.context.TranslationContext;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;
import at.stefl.opendocument.java.translator.style.DocumentStyle;
import at.stefl.opendocument.java.translator.style.DocumentStyleTranslator;

public abstract class GenericDocumentTranslator<D extends OpenDocument, S extends DocumentStyle, C extends GenericTranslationContext<D, S>>
        extends DocumentTranslator {
    
    private static final String AUTOMATIC_STYLES_ELEMENT_NAME = "office:automatic-styles";
    
    protected final DocumentStyleTranslator<S> styleTranslator;
    protected final ContentTranslator<C> contentTranslator;
    
    // TODO: kick me
    private C currentContext;
    
    public GenericDocumentTranslator(
            DocumentStyleTranslator<S> styleTranslator,
            ContentTranslator<C> contentTranslator) {
        this.styleTranslator = styleTranslator;
        this.contentTranslator = contentTranslator;
    }
    
    public GenericDocumentTranslator(
            GenericDocumentTranslator<D, S, C> translator) {
        this(translator.styleTranslator, translator.contentTranslator);
    }
    
    // TODO: kick me
    @Override
    public TranslationContext getCurrentContext() {
        return currentContext;
    }
    
    protected void translateStyle(LWXMLReader in, StyleSheetWriter out,
            C context) throws IOException {
        S style = styleTranslator.newDocumentStyle(out);
        context.setStyle(style);
        
        styleTranslator.translate(context.getDocument(), context.getStyle());
        
        LWXMLUtil.flushUntilStartElement(in, AUTOMATIC_STYLES_ELEMENT_NAME);
        styleTranslator.translate(new LWXMLElementReader(in),
                context.getStyle());
        
        style.close();
    }
    
    protected void translateHead(LWXMLReader in, LWXMLWriter out, C context)
            throws IOException {
        out.writeStartElement("base");
        out.writeAttribute("target", "_blank");
        out.writeEndElement("base");
        
        translateMeta(out);
        
        out.writeStartElement("title");
        out.writeCharacters("odf2html");
        out.writeEndElement("title");
        
        out.writeStartElement("style");
        out.writeAttribute("type", "text/css");
        out.writeCharacters("");
        contentTranslator.generateStyle(out, context);
        translateStyle(in, new StyleSheetWriter(out), context);
        out.writeEndElement("style");
        
        out.writeStartElement("script");
        out.writeAttribute("type", "text/javascript");
        out.writeCharacters("");
        contentTranslator.generateScript(out, context);
        out.writeEndElement("script");
    }
    
    // TODO: outsource?
    protected void translateMeta(LWXMLWriter out) throws IOException {
        out.writeStartElement("meta");
        out.writeAttribute("http-equiv", "Content-Type");
        out.writeAttribute("content", "text/html; charset=UTF-8");
        out.writeEndElement("meta");
    }
    
    protected void translateContent(LWXMLReader in, LWXMLWriter out, C context)
            throws IOException {
        contentTranslator.translate(in, out, context);
    }
    
    protected abstract C createContext();
    
    @Override
    @SuppressWarnings("unchecked")
    public void translate(OpenDocument document, LWXMLWriter out,
            TranslationSettings settings) throws IOException {
        translateGeneric((D) document, out, settings);
    }
    
    private void translateGeneric(D document, LWXMLWriter out,
            TranslationSettings settings) throws IOException {
        C context = currentContext = createContext();
        context.setDocument(document);
        context.setSettings(settings);
        
        CountingInputStream counter = new CountingInputStream(
                document.getContent());
        LWXMLReader in = new LWXMLStreamReader(counter);
        context.setCounter(counter);
        
        out.writeStartElement("html");
        out.writeStartElement("head");
        
        translateHead(in, out, context);
        
        out.writeEndElement("head");
        out.writeEmptyStartElement("body");
        
        translateContent(in, out, context);
        
        out.writeEndElement("body");
        out.writeEndElement("html");
        
        counter.close();
    }
    
}