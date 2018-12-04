package at.stefl.opendocument.java.translator.style;

import java.io.EOFException;
import java.io.IOException;

import at.stefl.commons.io.StreamableStringMap;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLElementDelegationReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;
import at.stefl.commons.util.collection.OrderedPair;
import at.stefl.opendocument.java.css.StyleSheetWriter;
import at.stefl.opendocument.java.odf.OpenDocument;

public abstract class DocumentStyleTranslator<T extends DocumentStyle> {
    
    private static final String GENERAL_STYLE_NAME = "style:style";
    
    private static final String DOCUMENT_STYLE_ELEMENT_NAME = "office:styles";
    
    private StreamableStringMap<StyleElementTranslator<? super T>> elementTranslatorMap = new StreamableStringMap<StyleElementTranslator<? super T>>();
    
    public DocumentStyleTranslator() {
        addElementTranslator(GENERAL_STYLE_NAME,
                new GeneralStyleElementTranslator());
    }
    
    public void addElementTranslator(String name,
            StyleElementTranslator<? super T> elementTranslator) {
        if (name == null) throw new NullPointerException();
        if (elementTranslator == null) throw new NullPointerException();
        
        elementTranslatorMap.put(name, elementTranslator);
    }
    
    public void removeElementTranslator(String name) {
        elementTranslatorMap.remove(name);
    }
    
    public abstract T newDocumentStyle(StyleSheetWriter styleOut)
            throws IOException;
    
    public void translate(OpenDocument document, T out) throws IOException {
        LWXMLReader in = new LWXMLStreamReader(document.getStyles());
        
        try {
            LWXMLUtil.flushUntilStartElement(in, DOCUMENT_STYLE_ELEMENT_NAME);
            translate(in, out);
        } catch (EOFException e) {
            // TODO: log - no styles
        } finally {
            in.close();
        }
    }
    
    public void translate(LWXMLReader in, T out) throws IOException {
        LWXMLElementDelegationReader din = new LWXMLElementDelegationReader(in);
        
        while (true) {
            LWXMLEvent event = din.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                OrderedPair<String, StyleElementTranslator<? super T>> match = elementTranslatorMap
                        .match(din);
                
                if (match != null) {
                    LWXMLReader ein = din.getElementReader();
                    
                    StyleElementTranslator<? super T> translator = match
                            .getElement2();
                    translator.translate(ein, out);
                }
            default:
                break;
            case END_DOCUMENT:
                return;
            }
        }
    }
}