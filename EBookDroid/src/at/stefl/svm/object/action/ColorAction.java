package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.Color;

public abstract class ColorAction extends SVMAction {
    
    private Color color;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [color=");
        builder.append(color);
        builder.append("]");
        return builder.toString();
    }
    
    public Color getColor() {
        return color;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeColorInt(color);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        color = in.readColorInt();
    }
    
}