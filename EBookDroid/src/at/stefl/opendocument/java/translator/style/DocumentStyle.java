package at.stefl.opendocument.java.translator.style;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import at.stefl.opendocument.java.css.StyleProperty;
import at.stefl.opendocument.java.css.StyleSheet;
import at.stefl.opendocument.java.css.StyleSheetParser;
import at.stefl.opendocument.java.css.StyleSheetWriter;

public class DocumentStyle {
    
    protected static StyleSheet loadStyleSheet(String name, Class<?> location)
            throws IOException {
        InputStream in = location.getResourceAsStream(name);
        
        try {
            StyleSheetParser styleSheetParser = new StyleSheetParser();
            return styleSheetParser.parse(in);
        } finally {
            in.close();
        }
    }
    
    private static String escapeStyleName(String name) {
        return name.replaceAll("\\.", "_");
    }
    
    private Map<String, Set<String>> styleInheritance = new HashMap<String, Set<String>>();
    private final StyleSheetWriter styleOut;
    
    public DocumentStyle(StyleSheetWriter styleOut) throws IOException {
        if (styleOut == null) throw new NullPointerException();
        this.styleOut = styleOut;
    }
    
    public List<String> getStyleParents(String name) {
        Set<String> parents = styleInheritance.get(name);
        if (parents == null) return null;
        return new ArrayList<String>(parents);
    }
    
    public String getStyleReference(String name) {
        Set<String> parents = styleInheritance.get(name);
        if (parents == null) return null;
        
        StringBuilder builder = new StringBuilder();
        
        builder.append(escapeStyleName(name));
        for (String parent : parents) {
            builder.append(' ');
            builder.append(escapeStyleName(parent));
        }
        
        return builder.toString();
    }
    
    public void addStyleInheritance(String name, Set<String> parents) {
        Set<String> parentSet = new HashSet<String>();
        
        for (String parent : parents) {
            parentSet.add(parent);
            
            Set<String> parentsParentSet = styleInheritance.get(parent);
            // TODO: log null
            if (parentsParentSet != null) parentSet.addAll(parentsParentSet);
        }
        
        styleInheritance.put(name, parentSet);
    }
    
    public void writeClass(String name) throws IOException {
        if (styleOut.isDefinitionStarted()) styleOut.writeEndDefinition();
        styleOut.writeStartDefinition("." + escapeStyleName(name));
    }
    
    public void writeProperty(StyleProperty property) throws IOException {
        styleOut.writeProperty(property);
    }
    
    public void writeProperty(String name, String value) throws IOException {
        styleOut.writeProperty(name, value);
    }
    
    public void close() throws IOException {
        if (styleOut.isDefinitionStarted()) styleOut.writeEndDefinition();
    }
    
}