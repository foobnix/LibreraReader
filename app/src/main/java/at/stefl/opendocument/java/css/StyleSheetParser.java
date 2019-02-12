package at.stefl.opendocument.java.css;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import at.stefl.commons.io.CharStreamUtil;
import at.stefl.commons.io.UntilCharacterReader;
import at.stefl.commons.util.string.StringUtil;

public class StyleSheetParser {
    
    public StyleSheet parse(File file) throws IOException {
        return parse(new FileReader(file));
    }
    
    public StyleSheet parse(InputStream in) throws IOException {
        return parse(new BufferedReader(new InputStreamReader(in)));
    }
    
    public StyleSheet parse(Reader in) throws IOException {
        StyleSheet result = new StyleSheet();
        
        while (true) {
            String selector = parseSelector(in);
            if (selector == null) return result;
            
            result.addDefinition(selector, parseDefinition(in));
        }
    }
    
    private String parseSelector(Reader in) throws IOException {
        int c = CharStreamUtil.flushWhitespace(in);
        if (c == -1) return null;
        
        return ((char) c)
                + StringUtil.trimRight(CharStreamUtil.readUntilChar(in, '{'));
    }
    
    private StyleDefinition parseDefinition(Reader in) throws IOException {
        StyleDefinition result = new StyleDefinition();
        
        in = new UntilCharacterReader(in, '}');
        
        while (true) {
            StyleProperty property = parseProperty(in);
            if (property == null) break;
            result.addProperty(property);
        }
        
        return result;
    }
    
    private StyleProperty parseProperty(Reader in) throws IOException {
        int c = CharStreamUtil.flushWhitespace(in);
        if (c == -1) return null;
        
        String name = ((char) c)
                + StringUtil.trimRight(CharStreamUtil.readUntilChar(in, ':'));
        
        c = CharStreamUtil.flushWhitespace(in);
        
        String value = ((char) c)
                + StringUtil.trimRight(CharStreamUtil.readUntilChar(in, ';'));
        
        return new StyleProperty(name, value);
    }
    
}