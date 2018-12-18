package at.stefl.svm.object.basic;

import java.io.IOException;

import at.stefl.commons.util.PrimitiveUtil;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.SVMVersionObject;

public class LineInfo extends SVMVersionObject {
    
    private int lineStyle;
    private int width;
    
    private int dashCount;
    private int dashLength;
    private int dotCount;
    private int dotLength;
    private int distance;
    
    private int lineJoin;
    
    // TODO: implement version 4
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LineInfo [lineStyle=");
        builder.append(lineStyle);
        builder.append(", width=");
        builder.append(width);
        builder.append(", dashCount=");
        builder.append(dashCount);
        builder.append(", dashLength=");
        builder.append(dashLength);
        builder.append(", dotCount=");
        builder.append(dotCount);
        builder.append(", dotLength=");
        builder.append(dotLength);
        builder.append(", distance=");
        builder.append(distance);
        builder.append(", lineJoin=");
        builder.append(lineJoin);
        builder.append("]");
        return builder.toString();
    }
    
    public int getLineStyle() {
        return lineStyle;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getDashCount() {
        return dashCount;
    }
    
    public int getDashLength() {
        return dashLength;
    }
    
    public int getDotCount() {
        return dotCount;
    }
    
    public int getDotLength() {
        return dotLength;
    }
    
    public int getDistance() {
        return distance;
    }
    
    public int getLineJoin() {
        return lineJoin;
    }
    
    public void setLineStyle(int lineStyle) {
        PrimitiveUtil.checkUnsignedShort(lineStyle);
        this.lineStyle = lineStyle;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public void setDashCount(int dashCount) {
        PrimitiveUtil.checkUnsignedShort(dashCount);
        this.dashCount = dashCount;
    }
    
    public void setDashLength(int dashLength) {
        this.dashLength = dashLength;
    }
    
    public void setDotCount(int dotCount) {
        PrimitiveUtil.checkUnsignedShort(dotCount);
        this.dotCount = dotCount;
    }
    
    public void setDotLength(int dotLength) {
        this.dotLength = dotLength;
    }
    
    public void setDistance(int distance) {
        this.distance = distance;
    }
    
    public void setLineJoin(int lineJoin) {
        PrimitiveUtil.checkUnsignedShort(lineJoin);
        this.lineJoin = lineJoin;
    }
    
    @Override
    public int getVersion() {
        return 3;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShort(lineStyle);
        out.writeInt(width);
        
        // version 2
        out.writeUnsignedShort(dashCount);
        out.writeInt(dashLength);
        out.writeUnsignedShort(dotCount);
        out.writeInt(dotLength);
        out.writeInt(distance);
        
        // version 3
        out.writeUnsignedShort(lineJoin);
        
        // TODO: implement version 4
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        lineStyle = in.readUnsignedShort();
        width = in.readInt();
        
        if (version >= 2) {
            dashCount = in.readUnsignedShort();
            dashLength = in.readInt();
            dotCount = in.readUnsignedShort();
            dotLength = in.readInt();
            distance = in.readInt();
            
            if (version >= 3) {
                lineJoin = in.readUnsignedShort();
            }
            
            // TODO: implement version 4
        }
    }
    
}