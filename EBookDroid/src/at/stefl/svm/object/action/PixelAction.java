package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.Color;

public class PixelAction extends SVMAction {
    
    private Vector2i point;
    private Color color;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PixelAction [point=");
        builder.append(point);
        builder.append(", color=");
        builder.append(color);
        builder.append("]");
        return builder.toString();
    }
    
    public Vector2i getPoint() {
        return point;
    }
    
    public Color getColorDefinition() {
        return color;
    }
    
    public void setPoint(Vector2i point) {
        this.point = point;
    }
    
    public void setColorDefinition(Color color) {
        this.color = color;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writePoint(point);
        out.writeColorInt(color);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        point = in.readPoint();
        color = in.readColorInt();
    }
    
}