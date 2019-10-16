package at.stefl.opendocument.java.css;

import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;

public class StyleSheetWriter {
    
    private final Writer out;
    
    private boolean isDefinitionStarted;
    
    public StyleSheetWriter(Writer out) {
        this.out = out;
    }
    
    public boolean isDefinitionStarted() {
        return isDefinitionStarted;
    }
    
    public void writeStartDefinition(String selector) throws IOException {
        if (isDefinitionStarted) throw new IllegalStateException();
        
        out.write(selector);
        out.write("{");
        
        isDefinitionStarted = true;
    }
    
    public void writeEndDefinition() throws IOException {
        if (!isDefinitionStarted) throw new IllegalStateException();
        
        out.write("}");
        
        isDefinitionStarted = false;
    }
    
    public void writeProperty(StyleProperty property) throws IOException {
        if (!isDefinitionStarted) throw new IllegalStateException();
        if (property == null) throw new NullPointerException();
        
        out.write(property.getName());
        out.write(":");
        out.write(property.getValue());
        out.write(";");
    }
    
    public void writeProperty(String name, String value) throws IOException {
        writeProperty(new StyleProperty(name, value));
    }
    
    public void writeDefinition(StyleDefinition definition) throws IOException {
        for (StyleProperty property : definition) {
            writeProperty(property);
        }
    }
    
    public void writeSheet(StyleSheet sheet) throws IOException {
        for (Entry<String, StyleDefinition> entry : sheet) {
            writeStartDefinition(entry.getKey());
            writeDefinition(entry.getValue());
            writeEndDefinition();
        }
    }
    
    public void flush() throws IOException {
        out.flush();
    }
    
    public void close() throws IOException {
        out.close();
    }
    
}