package at.stefl.commons.xml.namespace;

import java.util.Iterator;

public interface NamespaceContext {
    
    public String getNamespaceURI(String prefix);
    
    public String getPrefix(String namespaceURI);
    
    public Iterator<String> getPrefixes(String namespaceURI);
    
}