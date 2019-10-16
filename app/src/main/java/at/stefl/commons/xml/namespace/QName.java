package at.stefl.commons.xml.namespace;

import at.stefl.commons.xml.XMLConstants;

public class QName {
    
    private final String localPart;
    private final String prefix;
    private final String namespaceURI;
    
    public QName(String localName) {
        this(localName, XMLConstants.DEFAULT_NS_PREFIX,
                XMLConstants.NULL_NS_URI);
    }
    
    public QName(String localName, String namespaceURI) {
        this(localName, XMLConstants.DEFAULT_NS_PREFIX, namespaceURI);
    }
    
    public QName(String localPart, String prefix, String namespaceURI) {
        if (localPart == null) throw new IllegalArgumentException(
                "localPart cannot be null");
        if (prefix == null) throw new IllegalArgumentException(
                "prefix cannot be null");
        
        this.localPart = localPart;
        this.prefix = prefix;
        this.namespaceURI = namespaceURI;
    }
    
    @Override
    public int hashCode() {
        return localPart.hashCode() ^ namespaceURI.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof QName)) return false;
        QName qname = (QName) obj;
        
        return localPart.equals(qname.localPart)
                && namespaceURI.equals(qname.namespaceURI);
    }
    
    @Override
    public String toString() {
        String result = localPart;
        
        if (!prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) result = prefix
                + ":" + result;
        if (!namespaceURI.equals(XMLConstants.NULL_NS_URI)) result = "{"
                + namespaceURI + "} " + result;
        
        return result;
    }
    
    public String getLocalPart() {
        return localPart;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public String getNamespaceURI() {
        return namespaceURI;
    }
    
}