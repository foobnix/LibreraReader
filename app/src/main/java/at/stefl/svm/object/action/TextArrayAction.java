package at.stefl.svm.object.action;

import java.io.IOException;
import java.util.Arrays;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class TextArrayAction extends SVMAction {
    
    private Vector2i startPoint;
    private String string;
    private int[] dxArray;
    private int index;
    private int length;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextArrayAction [startPoint=");
        builder.append(startPoint);
        builder.append(", string=");
        builder.append(string);
        builder.append(", dxArray=");
        builder.append(Arrays.toString(dxArray));
        builder.append(", index=");
        builder.append(index);
        builder.append(", length=");
        builder.append(length);
        builder.append("]");
        return builder.toString();
    }
    
    public Vector2i getStartPoint() {
        return startPoint;
    }
    
    public String getString() {
        return string;
    }
    
    public int[] getDxArray() {
        return dxArray;
    }
    
    public int getIndex() {
        return index;
    }
    
    public int getLength() {
        return length;
    }
    
    public void setStartPoint(Vector2i startPoint) {
        this.startPoint = startPoint;
    }
    
    public void setString(String string) {
        this.string = string;
    }
    
    public void setDxArray(int[] dxArray) {
        this.dxArray = dxArray;
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
        out.writePoint(startPoint);
        out.writeUnicodeOrAsciiString(string);
        out.writeUnsignedShort(index);
        out.writeUnsignedShort(length);
        
        // TODO: unsigned
        out.writeInt(dxArray.length);
        for (int i = 0; i < dxArray.length; i++) {
            out.writeInt(dxArray[i]);
        }
        
        // version 2
        out.writeUnsignedShortPrefixedUTF16String(string);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        startPoint = in.readPoint();
        string = in.readUnicodeOrAsciiString();
        index = in.readUnsignedShort();
        this.length = in.readUnsignedShort();
        
        // TODO: unsigned
        int dxArrayLength = in.readInt();
        dxArray = new int[dxArrayLength];
        for (int i = 0; i < dxArrayLength; i++) {
            dxArray[i] = in.readInt();
        }
        
        if (version >= 2) {
            string = in.readUnsignedShortPrefixedUTF16String();
        }
    }
    
}