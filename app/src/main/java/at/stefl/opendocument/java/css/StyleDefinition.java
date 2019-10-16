package at.stefl.opendocument.java.css;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StyleDefinition implements Iterable<StyleProperty> {
    
    private class DefinitionIterator implements Iterator<StyleProperty> {
        
        private final Iterator<Map.Entry<String, String>> mapIterator = propertyMap
                .entrySet().iterator();
        
        @Override
        public boolean hasNext() {
            return mapIterator.hasNext();
        }
        
        @Override
        public StyleProperty next() {
            Map.Entry<String, String> entry = mapIterator.next();
            if (entry == null) return null;
            return new StyleProperty(entry.getKey(), entry.getValue());
        }
        
        @Override
        public void remove() {
            mapIterator.remove();
        }
    }
    
    private class PropertySet extends AbstractSet<StyleProperty> {
        
        @Override
        public boolean add(StyleProperty e) {
            StyleDefinition.this.addProperty(e);
            return true;
        }
        
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof StyleProperty)) throw new ClassCastException();
            StyleProperty property = (StyleProperty) o;
            return propertyMap.remove(property.getName()) != null;
        }
        
        @Override
        public int size() {
            return StyleDefinition.this.size();
        }
        
        @Override
        public Iterator<StyleProperty> iterator() {
            return StyleDefinition.this.iterator();
        }
        
        @Override
        public void clear() {
            StyleDefinition.this.clear();
        }
    }
    
    private final Map<String, String> propertyMap = new LinkedHashMap<String, String>();
    private PropertySet propertySet;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("{");
        
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            builder.append(entry.getKey());
            builder.append(":");
            builder.append(entry.getValue());
            builder.append(";");
        }
        
        builder.deleteCharAt(builder.length() - 1);
        builder.append("}");
        
        return builder.toString();
    }
    
    public int size() {
        return propertyMap.size();
    }
    
    public String getValue(String property) {
        return propertyMap.get(property);
    }
    
    public Set<StyleProperty> getPropertySet() {
        return (propertySet == null) ? (propertySet = new PropertySet())
                : propertySet;
    }
    
    public void addProperty(String name, String value) {
        if (name == null) throw new NullPointerException();
        if (value == null) throw new NullPointerException();
        
        propertyMap.put(name, value);
    }
    
    public void addProperty(StyleProperty property) {
        propertyMap.put(property.getName(), property.getValue());
    }
    
    public void removeProperty(String property) {
        propertyMap.remove(property);
    }
    
    @Override
    public Iterator<StyleProperty> iterator() {
        return new DefinitionIterator();
    }
    
    public void clear() {
        propertyMap.clear();
    }
    
}