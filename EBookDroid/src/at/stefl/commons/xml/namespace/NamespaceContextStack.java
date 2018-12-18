package at.stefl.commons.xml.namespace;

import java.util.Iterator;
import java.util.LinkedList;

// TODO: improve
public class NamespaceContextStack implements NamespaceContext {
    
    // removed Deque because of Android 1.6
    // private Deque<NamespaceContextMap> contextStack = new
    // LinkedList<NamespaceContextMap>();
    private LinkedList<NamespaceContextMap> contextStack = new LinkedList<NamespaceContextMap>();
    
    public NamespaceContextStack() {
        push();
    }
    
    private NamespaceContextMap peek() {
        return contextStack.getFirst();
    }
    
    @Override
    public String getNamespaceURI(String prefix) {
        return peek().getNamespaceURI(prefix);
    }
    
    @Override
    public String getPrefix(String namespaceURI) {
        return peek().getPrefix(namespaceURI);
    }
    
    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return peek().getPrefixes(namespaceURI);
    }
    
    public void putNamespace(String prefix, String namespaceURI) {
        peek().putNamespace(prefix, namespaceURI);
    }
    
    public void removeNamespaceURI(String namespaceURI) {
        peek().removeNamespaceURI(namespaceURI);
    }
    
    public void removePrefix(String prefix) {
        peek().removePrefix(prefix);
    }
    
    public void push() {
        contextStack.addFirst(new NamespaceContextMap(peek()));
    }
    
    public void pop() {
        if (contextStack.size() == 1) throw new IllegalStateException(
                "removing root is illegal");
        
        contextStack.removeFirst();
    }
    
}