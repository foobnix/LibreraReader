package at.stefl.commons.lwxml;

public class LWXMLAttribute {
    
    private final String name;
    private final String value;
    
    public LWXMLAttribute(String name, String value) {
        if (name == null) throw new NullPointerException();
        if (value == null) throw new NullPointerException();
        
        this.name = name;
        this.value = value;
    }
    
    @Override
    public String toString() {
        return name + "=" + value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof LWXMLAttribute)) return false;
        LWXMLAttribute other = (LWXMLAttribute) obj;
        
        return name.equals(other.name) && value.equals(other.value);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() + 31 * value.hashCode();
    }
    
    public String getName() {
        return name;
    }
    
    public String getValue() {
        return value;
    }
    
}