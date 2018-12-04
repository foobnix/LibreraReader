package at.stefl.svm.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import at.stefl.commons.io.ByteDataInputStream;
import at.stefl.commons.math.RectangleI;
import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.enumeration.SVMConstants;
import at.stefl.svm.enumeration.TextEncoding;
import at.stefl.svm.object.Color;
import at.stefl.svm.object.Fraction;

public class SVMDataInputStream extends ByteDataInputStream {
    
    private TextEncoding defaultEncoding = TextEncoding.ASCII_US;
    
    public SVMDataInputStream(InputStream in) {
        super(in, SVMConstants.ENDIANNESS);
    }
    
    public SVMDataInputStream(InputStream in, SVMDataInputStream state) {
        this(in);
        
        this.defaultEncoding = state.defaultEncoding;
    }
    
    public TextEncoding getDefaultEncoding() {
        return defaultEncoding;
    }
    
    public void setDefaultEncoding(TextEncoding defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }
    
    private Vector2i readVector2i() throws IOException {
        return new Vector2i(readInt(), readInt());
    }
    
    public Vector2i readPoint() throws IOException {
        return readVector2i();
    }
    
    public RectangleI readRectangleI() throws IOException {
        return new RectangleI(readInt(), readInt(), readInt(), readInt());
    }
    
    public List<Vector2i> readPolygon() throws IOException {
        int pointCount = readUnsignedShort();
        List<Vector2i> result = new ArrayList<Vector2i>(pointCount);
        
        for (int i = 0; i < pointCount; i++) {
            Vector2i point = readPoint();
            result.add(point);
        }
        
        return result;
    }
    
    public List<List<Vector2i>> readPolyPolygon() throws IOException {
        int polygonCount = readUnsignedShort();
        List<List<Vector2i>> result = new ArrayList<List<Vector2i>>();
        
        for (int i = 0; i < polygonCount; i++) {
            List<Vector2i> polygon = readPolygon();
            result.add(polygon);
        }
        
        return result;
    }
    
    public Fraction readFraction() throws IOException {
        Fraction result = new Fraction();
        
        result.setNumeratior(readInt());
        result.setDenominator(readInt());
        
        return result;
    }
    
    public Vector2i readSize() throws IOException {
        return readVector2i();
    }
    
    public Color readColor() throws IOException {
        int name = readUnsignedShort();
        if ((name & 0x8000) != 0) throw new IllegalStateException();
        
        int red = readUnsignedShort() >> 8;
        int green = readUnsignedShort() >> 8;
        int blue = readUnsignedShort() >> 8;
        
        return new Color(red, green, blue);
    }
    
    public Color readColorInt() throws IOException {
        return new Color(readInt());
    }
    
    public String readUnsignedShortPrefixedAsciiString() throws IOException {
        int size = readUnsignedShort();
        byte[] bytes = new byte[size];
        readFully(bytes);
        return new String(bytes, "US-ASCII");
    }
    
    public String readIntPrefixedUTF16String() throws IOException {
        int size = readInt();
        byte[] bytes = new byte[size * 2];
        readUnits(2, bytes);
        return new String(bytes, "UTF-16");
    }
    
    public String readUnsignedShortPrefixedUTF16String() throws IOException {
        int size = readUnsignedShort();
        byte[] bytes = new byte[size * 2];
        readUnits(2, bytes);
        return new String(bytes, "UTF-16");
    }
    
    public String readUnicodeOrAsciiString() throws IOException {
        if (defaultEncoding == TextEncoding.UCS2) return readIntPrefixedUTF16String();
        return readUnsignedShortPrefixedAsciiString();
    }
    
}