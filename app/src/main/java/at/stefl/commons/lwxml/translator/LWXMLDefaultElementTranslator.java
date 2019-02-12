package at.stefl.commons.lwxml.translator;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import at.stefl.commons.io.StreamableStringMap;
import at.stefl.commons.lwxml.LWXMLAttribute;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.collection.OrderedPair;

// TODO: thread-safe
public abstract class LWXMLDefaultElementTranslator<C> implements
        LWXMLElementTranslator<LWXMLPushbackReader, LWXMLWriter, C> {
    
    private final StreamableStringMap<LWXMLSimpleAttributeTranslator<? super C>> attributeTranslatorMap = new StreamableStringMap<LWXMLSimpleAttributeTranslator<? super C>>();
    private final Map<LWXMLComplexAttributeTranslator<? super C>, String[]> complexAttributeTranslatorMap = new HashMap<LWXMLComplexAttributeTranslator<? super C>, String[]>();
    
    private final Set<String> parseAttributes = new HashSet<String>();
    private final Map<String, String> currentParsedAttributes = new HashMap<String, String>();
    
    private final Map<String, String> newAttributes = new HashMap<String, String>();
    
    public Map<String, String> getCurrentParsedAttributes() {
        return currentParsedAttributes;
    }
    
    public String getCurrentParsedAttribute(String attribute) {
        return currentParsedAttributes.get(attribute);
    }
    
    public boolean addAttributeTranslator(String attribute, String newAttribute) {
        return addAttributeTranslator(attribute,
                new LWXMLStaticAttributeTranslator<C>(newAttribute));
    }
    
    public boolean addAttributeTranslator(String attribute,
            LWXMLSimpleAttributeTranslator<? super C> translator) {
        if (attribute == null) throw new NullPointerException();
        if (translator == null) throw new NullPointerException();
        
        attributeTranslatorMap.put(attribute, translator);
        return true;
    }
    
    public void addComplexAttributeTranslator(
            LWXMLComplexAttributeTranslator<? super C> translator,
            String... attributes) {
        complexAttributeTranslatorMap.put(translator, attributes.clone());
        
        for (String attribute : attributes)
            addParseAttribute(attribute);
    }
    
    public void addParseAttribute(String attribute) {
        if (attribute == null) throw new NullPointerException();
        
        parseAttributes.add(attribute);
        if (!attributeTranslatorMap.containsKey(attribute)) attributeTranslatorMap
                .put(attribute, null);
    }
    
    public void addNewAttribute(LWXMLAttribute attribute) {
        if (attribute == null) throw new NullPointerException();
        
        newAttributes.put(attribute.getName(), attribute.getValue());
    }
    
    public void addNewAttribute(String name, String value) {
        addNewAttribute(new LWXMLAttribute(name, value));
    }
    
    public void removeAttributeTranslator(String attribute) {
        attributeTranslatorMap.remove(attribute);
    }
    
    public void removeParseAttribute(String attribute) {
        parseAttributes.remove(attribute);
    }
    
    public void removeNewAttribute(String attribute) {
        newAttributes.remove(attribute);
    }
    
    @Override
    public final void translate(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {
        LWXMLEvent event = in.readEvent();
        
        switch (event) {
        case START_ELEMENT:
            translateStartElement(in, out, context);
            translateAttributeList(in, out, context);
            translateEndAttributeList(in, out, context);
            translateChildren(in, out, context);
            currentParsedAttributes.clear();
            break;
        case END_EMPTY_ELEMENT:
        case END_ELEMENT:
            translateEndElement(in, out, context);
            break;
        default:
            throw new LWXMLIllegalEventException(event);
        }
    }
    
    @Override
    public abstract void translateStartElement(LWXMLPushbackReader in,
            LWXMLWriter out, C context) throws IOException;
    
    @Override
    public void translateAttribute(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {
        OrderedPair<String, LWXMLSimpleAttributeTranslator<? super C>> match = attributeTranslatorMap
                .match(in);
        if (match == null) return;
        
        String attributeName = match.getElement1();
        String attributeValue = in.readFollowingValue();
        
        if (parseAttributes.contains(attributeName)) currentParsedAttributes
                .put(attributeName, attributeValue);
        
        LWXMLSimpleAttributeTranslator<? super C> attributeTranslator = match
                .getElement2();
        if (attributeTranslator == null) return;
        attributeTranslator.translate(new LWXMLAttribute(attributeName,
                attributeValue), out, context);
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {
        for (Map.Entry<String, String> attribute : newAttributes.entrySet())
            out.writeAttribute(attribute.getKey(), attribute.getValue());
        
        loop:
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case ATTRIBUTE_NAME:
                translateAttribute(in, out, context);
            case ATTRIBUTE_VALUE:
                break;
            case END_ATTRIBUTE_LIST:
                break loop;
            default:
                throw new LWXMLIllegalEventException(event);
            }
        }
        
        for (Map.Entry<LWXMLComplexAttributeTranslator<? super C>, String[]> entry : complexAttributeTranslatorMap
                .entrySet()) {
            // TODO: improve? read only?
            Map<String, String> attributes = new HashMap<String, String>(
                    entry.getValue().length);
            
            for (String name : entry.getValue()) {
                String value = currentParsedAttributes.get(name);
                if (value == null) continue;
                attributes.put(name, value);
            }
            
            entry.getKey().translate(attributes, out, context);
        }
    }
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, C context) throws IOException {
        out.writeEvent(LWXMLEvent.END_ATTRIBUTE_LIST);
    }
    
    @Override
    public void translateChildren(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {}
    
    @Override
    public void translateContent(LWXMLPushbackReader in, LWXMLWriter out,
            C context) throws IOException {
        out.writeEvent(LWXMLEvent.CHARACTERS);
        out.write(in);
    }
    
    @Override
    public abstract void translateEndElement(LWXMLPushbackReader in,
            LWXMLWriter out, C context) throws IOException;
    
}