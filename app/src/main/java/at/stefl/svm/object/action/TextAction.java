package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class TextAction extends SVMAction {
    
    private Vector2i point;
    private String string;
    private int index;
    private int length;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextAction [point=");
        builder.append(point);
        builder.append(", string=");
        builder.append(string);
        builder.append(", index=");
        builder.append(index);
        builder.append(", length=");
        builder.append(length);
        builder.append("]");
        return builder.toString();
    }
    
    public Vector2i getPoint() {
        return point;
    }
    
    public String getString() {
        return string;
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setPoint(Vector2i point) {
        this.point = point;
    }
    
    public void setString(String string) {
        this.string = string;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public void setLength(int length) {
        this.length = length;
    }
    
    @Override
    protected int getVersion() {
        return 2;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writePoint(point);
        out.writeUnicodeOrAsciiString(string);
        out.writeUnsignedShort(index);
        out.writeUnsignedShort(length);
        
        // version 2
        out.writeUnsignedShortPrefixedUTF16String(string);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        point = in.readPoint();
        string = in.readUnicodeOrAsciiString();
        index = in.readUnsignedShort();
        this.length = in.readUnsignedShort();
        
        if (version >= 2) {
            string = in.readUnsignedShortPrefixedUTF16String();
        }
    }
    
}