package at.stefl.commons.lwxml;

import java.util.Collections;
import java.util.Set;

import at.stefl.commons.util.array.ArrayUtil;

public class LWXMLConstants {
    
    public static final String NEW_LINE = "\n";
    
    // http://www.w3.org/TR/REC-xml/#NT-S
    public static final Set<Character> WHITESPACE_SET = Collections
            .unmodifiableSet(ArrayUtil.toHashSet(new char[] { ' ', '\t', '\r',
                    '\n' }));
    
    private LWXMLConstants() {}
    
    // http://www.w3.org/TR/REC-xml/#NT-S
    public static boolean isWhitespace(char c) {
        switch (c) {
        case ' ':
        case '\t':
        case '\r':
        case '\n':
            return true;
        }
        
        return false;
    }
    
    public static boolean isWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!isWhitespace(c)) return false;
        }
        
        return true;
    }
    
    // http://www.w3.org/TR/REC-xml/#NT-Char
    public static boolean isCharacter(char c) {
        if (isWhitespace(c)) return true;
        
        if ((c > 0x20) && (c <= 0xd7ff)) return true;
        if ((c >= 0xe000) && (c <= 0xfffd)) return true;
        if ((c >= 0x10000) && (c <= 0x10ffff)) return true;
        
        return false;
    }
    
}