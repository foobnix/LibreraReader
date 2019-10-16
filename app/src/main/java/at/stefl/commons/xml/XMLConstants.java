package at.stefl.commons.xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class XMLConstants {
    
    public static final String NULL_NS_URI = "";
    
    public static final String DEFAULT_NS_PREFIX = "";
    
    public static final String XML_NS_URI = "http://www.w3.org/XML/1998/namespace";
    
    public static final String XML_NS_PREFIX = "xml";
    
    public static final String XMLNS_ATTRIBUTE_NS_URI = "http://www.w3.org/2000/xmlns/";
    
    public static final String XMLNS_ATTRIBUTE = "xmlns";
    
    public static final String NEW_LINE = "\n";
    
    // http://www.w3.org/TR/REC-xml/#NT-S
    public static final Set<Character> SPACE_SET = Collections
            .unmodifiableSet(new HashSet<Character>(Arrays.asList(' ', '\t',
                    '\r', '\n')));
    
    private XMLConstants() {}
    
    // http://www.w3.org/TR/REC-xml/#NT-S
    public static boolean isSpace(char c) {
        switch (c) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
            return true;
        }
        
        return false;
    }
    
    public static boolean isSpace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isSpace(c)) return false;
        }
        
        return true;
    }
    
    // http://www.w3.org/TR/REC-xml/#NT-Char
    public static boolean isChar(char c) {
        if (isSpace(c)) return true;
        
        if ((c > 0x20) && (c <= 0xd7ff)) return true;
        if ((c >= 0xe000) && (c <= 0xfffd)) return true;
        if ((c >= 0x10000) && (c <= 0x10ffff)) return true;
        
        return false;
    }
    
}