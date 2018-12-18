package at.stefl.svm.object.action;

import java.io.IOException;
import java.util.List;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.basic.LineInfo;

public class PolyLineAction extends SVMAction {
    
    private List<Vector2i> simplePolygon;
    
    private LineInfo lineInfo;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PolyLineAction [simplePolygon=");
        builder.append(simplePolygon);
        builder.append(", lineInfo=");
        builder.append(lineInfo);
        builder.append("]");
        return builder.toString();
    }
    
    public List<Vector2i> getSimplePolygon() {
        return simplePolygon;
    }
    
    public LineInfo getLineInfo() {
        return lineInfo;
    }
    
    public void setSimplePolygon(List<Vector2i> simplePolygon) {
        this.simplePolygon = simplePolygon;
    }
    
    public void setLineInfo(LineInfo lineInfo) {
        this.lineInfo = lineInfo;
    }
    
    @Override
    protected int getVersion() {
        return 3;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writePolygon(simplePolygon);
        
        // version 2
        lineInfo.serialize(out);
        
        // version 3
        // TODO: write polygon with flags
        out.writeBoolean(false);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        simplePolygon = in.readPolygon();
        
        if (version >= 2) {
            lineInfo = new LineInfo();
            lineInfo.deserialize(in);
            
            if (version >= 3) {
                boolean hasFlags = in.readBoolean();
                if (hasFlags) {
                    // TODO: read polygon with flags
                }
            }
        }
    }
    
}