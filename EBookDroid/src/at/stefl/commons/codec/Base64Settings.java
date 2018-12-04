package at.stefl.commons.codec;

// TODO: use bytes?
public class Base64Settings {
    
    private static final char[] ENCODE_TABLE = { 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's',
            't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 0, 0 };
    
    private static final byte[] DECODE_TABLE = { -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
            -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1,
            -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
            40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };
    
    public static final char PADDING_CHAR = '=';
    
    public static final Base64Settings ORIGINAL = new Base64Settings('+', '/',
            true);
    public static final Base64Settings FILENAME = new Base64Settings('+', '-',
            false);
    public static final Base64Settings URL = new Base64Settings('-', '_', false);
    
    final char[] encodeTable;
    final byte[] decodeTable;
    final boolean padding;
    
    public Base64Settings(char index62, char index63, boolean padding) {
        encodeTable = ENCODE_TABLE.clone();
        decodeTable = DECODE_TABLE.clone();
        
        encodeTable[62] = index62;
        encodeTable[63] = index63;
        decodeTable[index62] = 62;
        decodeTable[index63] = 63;
        
        this.padding = padding;
    }
    
    public char[] getEncodeTable() {
        return encodeTable.clone();
    }
    
    public byte[] getDecodeTable() {
        return decodeTable.clone();
    }
    
    public boolean isPadding() {
        return padding;
    }
    
    public boolean canEncode(byte b) {
        return (b >= 0) & (b < 64);
    }
    
    public boolean canDecode(char c) {
        return decodeTable[c] != -1;
    }
    
    public char encode(byte b) {
        if (!canEncode(b)) throw new IllegalArgumentException("unmapped byte: "
                + b);
        return encodeTable[b];
    }
    
    public byte decode(char c) {
        if (!canDecode(c)) throw new IllegalArgumentException("unmapped char: "
                + c);
        return decodeTable[c];
    }
    
    public int encodedSize(int size) {
        int result = (size / 3) * 4;
        if ((size % 3) > 0) result += padding ? 4 : ((size % 3) + 1);
        return result;
    }
    
    public int decodedSize(char[] in) {
        return decodedSize(in, 0, in.length);
    }
    
    public int decodedSize(char[] in, int off, int len) {
        int end = off + len;
        return decodedSize(len, in[end - 2], in[end - 1]);
    }
    
    public int decodedSize(CharSequence in) {
        return decodedSize(in, 0, in.length());
    }
    
    public int decodedSize(CharSequence in, int start, int end) {
        int len = end - start;
        return decodedSize(len, in.charAt(end - 2), in.charAt(end - 1));
    }
    
    public int decodedSize(int size, char lastButOne, char last) {
        int result = (size / 4) * 3;
        int lenMod4 = size % 4;
        if (lenMod4 > 0) {
            if (padding || (lenMod4 == 1)) throw new IllegalArgumentException(
                    "illegal length");
            result += lenMod4 - 1;
        }
        if (padding) {
            if (last == PADDING_CHAR) {
                result--;
                if (lastButOne == PADDING_CHAR) {
                    result--;
                }
            }
        }
        return result;
    }
    
}