package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class PointAction extends SVMAction {
    
    private Vector2i point;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PointAction [point=");
        builder.append(point);
        builder.append("]");
        return builder.toString();
    }
    
    public Vector2i getPoint() {
        return point;
    }
    
    public void setPoint(Vector2i point) {
        this.point = point;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writePoint(point);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        point = in.readPoint();
    }
    
}