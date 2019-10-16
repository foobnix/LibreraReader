package at.stefl.opendocument.java.css;

public final class StyleProperty {
    
    private final String name;
    private final String value;
    
    public StyleProperty(String name, String value) {
        if (name == null) throw new NullPointerException();
        if (value == null) throw new NullPointerException();
        
        this.name = name;
        this.value = value;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(name);
        builder.append(":");
        builder.append(value);
        
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof StyleProperty)) return false;
        StyleProperty other = (StyleProperty) obj;
        
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