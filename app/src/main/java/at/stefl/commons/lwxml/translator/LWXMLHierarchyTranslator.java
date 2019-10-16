package at.stefl.commons.lwxml.translator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;

import at.stefl.commons.io.StreamableStringMap;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.collection.OrderedPair;

public class LWXMLHierarchyTranslator<C> implements
        LWXMLTranslator<LWXMLReader, LWXMLWriter, C> {
    
    private StreamableStringMap<LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C>> elementTranslators = new StreamableStringMap<LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C>>();
    
    public void addElementTranslator(
            String element,
            LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C> translator) {
        if (element == null) throw new NullPointerException();
        elementTranslators.put(element, translator);
    }
    
    public void addElementTranslator(String element, String newElement) {
        addElementTranslator(element,
                new LWXMLElementReplacement<C>(newElement));
    }
    
    public void removeElementTranslator(String element) {
        elementTranslators.remove(element);
    }
    
    public Collection<LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C>> elementTranslators() {
        return Collections.unmodifiableCollection(elementTranslators.values());
    }
    
    @Override
    public void translate(LWXMLReader in, LWXMLWriter out, C context)
            throws IOException {
        LWXMLPushbackReader pin = new LWXMLPushbackReader(in);
        
        OrderedPair<String, LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C>> translatorMatch = null;
        Deque<LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C>> translatorStack = new LinkedList<LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C>>();
        
        LWXMLEvent event;
        while (true) {
            event = pin.readEvent();
            if (event == LWXMLEvent.END_DOCUMENT) break;
            
            switch (event) {
            case START_ELEMENT:
            case END_ELEMENT:
                translatorMatch = elementTranslators.match(pin);
            case END_EMPTY_ELEMENT:
                if (translatorMatch == null) break;
                
                switch (event) {
                case START_ELEMENT:
                    translatorStack.push((translatorMatch == null) ? null
                            : translatorMatch.getElement2());
                    break;
                case END_EMPTY_ELEMENT:
                case END_ELEMENT:
                    translatorStack.pop();
                    break;
                default:
                    break;
                }
                
                String elementName = translatorMatch.getElement1();
                LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C> translator = translatorMatch
                        .getElement2();
                
                pin.unreadEvent(elementName);
                translator.translate(pin, out, context);
                
                break;
            case CHARACTERS:
                LWXMLElementTranslator<? super LWXMLPushbackReader, ? super LWXMLWriter, ? super C> contentTranslator = translatorStack
                        .peek();
                if (contentTranslator == null) break;
                contentTranslator.translateContent(pin, out, context);
                
                break;
            default:
                break;
            }
        }
    }
    
}