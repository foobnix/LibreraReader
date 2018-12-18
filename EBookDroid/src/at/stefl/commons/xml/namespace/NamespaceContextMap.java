package at.stefl.commons.xml.namespace;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.stefl.commons.util.iterator.EmptyIterator;
import at.stefl.commons.util.iterator.SingleElementIterator;
import at.stefl.commons.xml.XMLConstants;

public class NamespaceContextMap implements NamespaceContext {
    
    private final Map<String, String> namespaceURIMap;
    private final Map<String, List<String>> prefixesMap;
    
    public NamespaceContextMap() {
        namespaceURIMap = new HashMap<String, String>();
        prefixesMap = new HashMap<String, List<String>>();
    }
    
    public NamespaceContextMap(NamespaceContextMap contextMap) {
        namespaceURIMap = new HashMap<String, String>(
                contextMap.namespaceURIMap);
        prefixesMap = new HashMap<String, List<String>>(contextMap.prefixesMap);
    }
    
    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) throw new IllegalArgumentException(
                "prefix cannot be null");
        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) return XMLConstants.XML_NS_URI;
        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        
        String result = namespaceURIMap.get(prefix);
        if (result == null) return XMLConstants.NULL_NS_URI;
        return result;
    }
    
    @Override
    public String getPrefix(String namespaceURI) {
        Iterator<String> prefixes = getPrefixes(namespaceURI);
        if (!prefixes.hasNext()) return null;
        return prefixes.next();
    }
    
    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        if (namespaceURI == null) throw new IllegalArgumentException(
                "namespaceURI cannot be null");
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) return new SingleElementIterator<String>(
                XMLConstants.XML_NS_PREFIX);
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) return new SingleElementIterator<String>(
                XMLConstants.XMLNS_ATTRIBUTE);
        
        List<String> result = prefixesMap.get(namespaceURI);
        if (result == null) return new EmptyIterator<String>();
        return result.iterator();
    }
    
    public void putNamespace(String prefix, String namespaceURI) {
        namespaceURIMap.put(prefix, namespaceURI);
        
        List<String> prefixes = prefixesMap.get(namespaceURI);
        if (prefixes == null) {
            prefixes = new LinkedList<String>();
            prefixesMap.put(namespaceURI, prefixes);
        }
        prefixes.add(prefix);
    }
    
    public void removeNamespaceURI(String namespaceURI) {
        List<String> prefixes = prefixesMap.remove(namespaceURI);
        if (prefixes == null) return;
        
        for (String prefix : prefixes) {
            namespaceURIMap.remove(prefix);
        }
    }
    
    public void removePrefix(String prefix) {
        String namespaceURI = namespaceURIMap.remove(prefix);
        if (namespaceURI == null) return;
        
        List<String> prefixes = prefixesMap.get(namespaceURI);
        prefixes.remove(prefix);
        if (prefixes.isEmpty()) prefixesMap.remove(namespaceURI);
    }
    
}