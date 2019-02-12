package at.stefl.commons.lwxml.path;

public class LWXMLNodeIdentifier {
    
    private final String elementName;
    private final int index;
    
    public LWXMLNodeIdentifier(String node) {
        this(node, 0);
    }
    
    public LWXMLNodeIdentifier(String elementName, int index) {
        if (elementName == null) throw new NullPointerException(
                "elementName cannot be null");
        if (elementName.length() == 0) throw new IllegalArgumentException(
                "elementName cannot be empty");
        if (index < 0) throw new IndexOutOfBoundsException(
                "index cannot be less than 0");
        
        this.elementName = elementName;
        this.index = index;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(elementName);
        
        if (index != 0) {
            builder.append("[");
            builder.append(index);
            builder.append("]");
        }
        
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof LWXMLNodeIdentifier)) return false;
        LWXMLNodeIdentifier other = (LWXMLNodeIdentifier) obj;
        
        return elementName.equals(other.elementName) && (index == other.index);
    }
    
    @Override
    public int hashCode() {
        return elementName.hashCode() + 31 * index;
    }
    
    public String getElementName() {
        return elementName;
    }
    
    public int getIndex() {
        return index;
    }
    
}