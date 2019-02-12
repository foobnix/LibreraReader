package at.stefl.commons.util.string;

public class CharSequenceUtil {
    
    public static final CharSequence NULL = StringUtil.NULL;
    
    public static int hashCode(CharSequence charSequence) {
        int result = 0;
        int len = charSequence.length();
        
        for (int i = 0; i < len; i++) {
            result = 31 * result + charSequence.charAt(i);
        }
        
        return result;
    }
    
    public static boolean equals(CharSequence a, CharSequence b) {
        int len = a.length();
        if (len != b.length()) return false;
        
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b.charAt(i)) return false;
        }
        
        return true;
    }
    
    public static boolean equals(CharSequence a, int aOff, CharSequence b,
            int bOff, int len) {
        for (int i = 0; i < len; i++) {
            if (a.charAt(aOff + i) != b.charAt(bOff + i)) return false;
        }
        
        return true;
    }
    
    public static boolean equals(CharSequence a, char[] b) {
        int len = a.length();
        if (len != b.length) return false;
        
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b[i]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(CharSequence a, int aOff, char[] b, int bOff,
            int len) {
        for (int i = 0; i < len; i++) {
            if (a.charAt(aOff + i) != b[bOff + i]) return false;
        }
        
        return true;
    }
    
    public static String toString(CharSequence charSequence) {
        return new String(getAsCharArray(charSequence));
    }
    
    public static boolean isEmpty(CharSequence charSequence) {
        return charSequence.length() == 0;
    }
    
    public static boolean statsWith(CharSequence a, CharSequence b) {
        int len = b.length();
        if (len > a.length()) return false;
        
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b.charAt(i)) return false;
        }
        
        return true;
    }
    
    public static boolean statsWith(CharSequence a, int off, CharSequence b) {
        if (off < 0) return false;
        
        int length = b.length();
        if (length > (a.length() - off)) return false;
        
        for (int i = 0; i < length; i++) {
            if (a.charAt(off + i) != b.charAt(i)) return false;
        }
        
        return true;
    }
    
    public static boolean endsWith(CharSequence a, CharSequence b) {
        int len = b.length();
        int off = len - a.length();
        if (off < 0) return false;
        
        for (int i = 0; i < len; i++) {
            if (a.charAt(off + i) != b.charAt(i)) return false;
        }
        
        return true;
    }
    
    public static CharSequence trim(CharSequence charSequence) {
        int start = 0;
        int end = charSequence.length();
        
        for (; (start < end) && (charSequence.charAt(start) <= ' '); start++)
            ;
        for (; (end > 0) && (charSequence.charAt(end - 1) <= ' '); end--)
            ;
        
        return charSequence.subSequence(start, end);
    }
    
    public static CharSequence trimLeft(CharSequence charSequence) {
        int start = 0;
        int end = charSequence.length();
        
        for (; (start < end) && (charSequence.charAt(start) <= ' '); start++)
            ;
        
        return charSequence.subSequence(start, end);
    }
    
    public static CharSequence trimRight(CharSequence charSequence) {
        int length = charSequence.length();
        
        for (; (length > 0) && (charSequence.charAt(length - 1) <= ' '); length--)
            ;
        
        return charSequence.subSequence(0, length);
    }
    
    public static void copy(CharSequence source, char[] destiantion) {
        copy(source, destiantion, 0);
    }
    
    public static void copy(CharSequence src, char[] dst, int off) {
        copy(src, dst, off, src.length());
    }
    
    public static void copy(CharSequence src, char[] dst, int off, int len) {
        for (int i = 0; i < len; i++) {
            dst[off + i] = src.charAt(i);
        }
    }
    
    public static char[] getAsCharArray(CharSequence charSequence) {
        int length = charSequence.length();
        char[] result = new char[length];
        
        for (int i = 0; i < length; i++) {
            result[i] = charSequence.charAt(i);
        }
        
        return result;
    }
    
    private CharSequenceUtil() {}
    
}