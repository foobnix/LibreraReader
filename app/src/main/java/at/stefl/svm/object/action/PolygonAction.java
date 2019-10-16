package at.stefl.svm.object.action;

import java.io.IOException;
import java.util.List;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class PolygonAction extends SVMAction {
    
    private List<Vector2i> simplePolygon;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PolygonAction [simplePolygon=");
        builder.append(simplePolygon);
        builder.append("]");
        return builder.toString();
    }
    
    public List<Vector2i> getSimplePolygon() {
        return simplePolygon;
    }
    
    public void setSimplePolygon(List<Vector2i> simplePolygon) {
        this.simplePolygon = simplePolygon;
    }
    
    @Override
    protected int getVersion() {
        return 2;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writePolygon(simplePolygon);
        
        // version 2
        // TODO: write polygon with flags
        out.writeBoolean(false);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        simplePolygon = in.readPolygon();
        
        if (version >= 2) {
            boolean hasFlags = in.readBoolean();
            if (hasFlags) {
                // TODO: read polygon with flags
            }
        }
    }
    
}