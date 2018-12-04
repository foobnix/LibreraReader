package at.stefl.opendocument.java.css;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

// TODO: implement selector
public class StyleSheet implements Iterable<Entry<String, StyleDefinition>> {
    
    private final Map<String, StyleDefinition> definitionMap = new LinkedHashMap<String, StyleDefinition>();
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        for (Map.Entry<String, StyleDefinition> entry : definitionMap
                .entrySet()) {
            builder.append(entry.getKey());
            builder.append(entry.getValue());
        }
        
        return builder.toString();
    }
    
    public Map<String, StyleDefinition> getDefinitionMap() {
        return definitionMap;
    }
    
    public void addDefinition(String selector, StyleDefinition definition) {
        definitionMap.put(selector, definition);
    }
    
    public void removeDefinition(String selector) {
        definitionMap.remove(selector);
    }
    
    @Override
    public Iterator<Entry<String, StyleDefinition>> iterator() {
        return definitionMap.entrySet().iterator();
    }
    
}