package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.basic.LineInfo;

public class LineAction extends SVMAction {
    
    private LineInfo lineInfo;
    private Vector2i startPoint;
    private Vector2i endPoint;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("LineAction [lineInfo=");
        builder.append(lineInfo);
        builder.append(", startPoint=");
        builder.append(startPoint);
        builder.append(", endPoint=");
        builder.append(endPoint);
        builder.append("]");
        return builder.toString();
    }
    
    public LineInfo getLineInfo() {
        return lineInfo;
    }
    
    public Vector2i getStartPoint() {
        return startPoint;
    }
    
    public Vector2i getEndPoint() {
        return endPoint;
    }
    
    public void setLineInfo(LineInfo lineInfo) {
        this.lineInfo = lineInfo;
    }
    
    public void setStartPoint(Vector2i startPoint) {
        this.startPoint = startPoint;
    }
    
    public void setEndPoint(Vector2i endPoint) {
        this.endPoint = endPoint;
    }
    
    @Override
    protected int getVersion() {
        return 2;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writePoint(startPoint);
        out.writePoint(endPoint);
        
        // version 2
        lineInfo.serialize(out);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        startPoint = in.readPoint();
        endPoint = in.readPoint();
        
        if (version >= 2) {
            lineInfo = new LineInfo();
            lineInfo.deserialize(in);
        }
    }
    
}