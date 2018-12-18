package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.math.RectangleI;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class RectangleAction extends SVMAction {
    
    private RectangleI rectangle;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RectangleAction [rectangle=");
        builder.append(rectangle);
        builder.append("]");
        return builder.toString();
    }
    
    public RectangleI getRectangle() {
        return rectangle;
    }
    
    public void setRectangle(RectangleI rectangle) {
        this.rectangle = rectangle;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeRectangleI(rectangle);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        rectangle = in.readRectangleI();
    }
    
}