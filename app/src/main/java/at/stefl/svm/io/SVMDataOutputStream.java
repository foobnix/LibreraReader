package at.stefl.svm.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.RandomAccess;

import at.stefl.commons.io.ByteDataOutputStream;
import at.stefl.commons.math.RectangleI;
import at.stefl.commons.math.vector.Vector2i;
import at.stefl.commons.util.PrimitiveUtil;
import at.stefl.svm.enumeration.SVMConstants;
import at.stefl.svm.enumeration.TextEncoding;
import at.stefl.svm.object.Color;
import at.stefl.svm.object.Fraction;

public class SVMDataOutputStream extends ByteDataOutputStream {
    
    private TextEncoding defaultEncoding = TextEncoding.ASCII_US;
    
    public SVMDataOutputStream(OutputStream out) {
        super(out, SVMConstants.ENDIANNESS);
    }
    
    public SVMDataOutputStream(OutputStream out, SVMDataOutputStream state) {
        this(out);
        
        this.defaultEncoding = state.defaultEncoding;
    }
    
    public TextEncoding getDefaultEncoding() {
        return defaultEncoding;
    }
    
    public void setDefaultEncoding(TextEncoding defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }
    
    private void writeVector2i(Vector2i vector) throws IOException {
        writeUnsignedInt(vector.getX());
        writeUnsignedInt(vector.getY());
    }
    
    public void writePoint(Vector2i point) throws IOException {
        writeVector2i(point);
    }
    
    public void writeRectangleI(RectangleI rectangle) throws IOException {
        writeInt(rectangle.getLeft());
        writeInt(rectangle.getTop());
        writeInt(rectangle.getRight());
        writeInt(rectangle.getBottom());
    }
    
    public void writePolygon(List<Vector2i> polygon) throws IOException {
        int pointCount = polygon.size();
        if ((pointCount & 0xffff0000) != 0) throw new IllegalArgumentException(
                "too much points");
        
        writeUnsignedShort(pointCount);
        
        if (polygon instanceof RandomAccess) {
            for (int i = 0; i < pointCount; i++) {
                writePoint(polygon.get(i));
            }
        } else {
            for (Vector2i point : polygon) {
                writePoint(point);
            }
        }
    }
    
    public void writePolyPolygon(List<List<Vector2i>> polyPolygon)
            throws IOException {
        int polygonCount = polyPolygon.size();
        PrimitiveUtil.checkUnsignedShort(polygonCount);
        
        writeUnsignedShort(polygonCount);
        
        if (polyPolygon instanceof RandomAccess) {
            for (int i = 0; i < polygonCount; i++) {
                writePolygon(polyPolygon.get(i));
            }
        } else {
            for (List<Vector2i> polygon : polyPolygon) {
                writePolygon(polygon);
            }
        }
    }
    
    public void writeFraction(Fraction fraction) throws IOException {
        writeInt(fraction.getNumeratior());
        writeInt(fraction.getDenominator());
    }
    
    public void writeSize(Vector2i size) throws IOException {
        writeVector2i(size);
    }
    
    public void writeColor(Color color) throws IOException {
        writeUnsignedShort(0x8000);
        writeUnsignedShort((color.getRed() << 8) | color.getRed());
        writeUnsignedShort((color.getGreen() << 8) | color.getGreen());
        writeUnsignedShort((color.getBlue() << 8) | color.getBlue());
    }
    
    public void writeColorInt(Color color) throws IOException {
        writeInt(color.getARGB());
    }
    
    public void writeUnsignedShortPrefixedAsciiString(String string)
            throws IOException {
        writeUnsignedShort(string.length());
        write(string.getBytes("US-ASCII"));
    }
    
    public void writeIntPrefixedUTF16String(String string) throws IOException {
        writeInt(string.length());
        writeUnits(2, string.getBytes("UTF-16"));
    }
    
    public void writeUnsignedShortPrefixedUTF16String(String string)
            throws IOException {
        writeUnsignedShort(string.length());
        writeUnits(2, string.getBytes("UTF-16"));
    }
    
    public void writeUnicodeOrAsciiString(String string) throws IOException {
        if (defaultEncoding == TextEncoding.UCS2) writeIntPrefixedUTF16String(string);
        else writeUnsignedShortPrefixedAsciiString(string);
    }
    
}