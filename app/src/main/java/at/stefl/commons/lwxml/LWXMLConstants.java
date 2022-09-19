package at.stefl.commons.lwxml;

public class LWXMLConstants {
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
}