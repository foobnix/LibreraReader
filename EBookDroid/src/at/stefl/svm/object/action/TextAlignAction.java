package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class TextAlignAction extends SVMAction {
    
    private int align;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TextAlignAction [align=");
        builder.append(align);
        builder.append("]");
        return builder.toString();
    }
    
    public int getAlign() {
        return align;
    }
    
    public void setAlign(int align) {
        this.align = align;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShort(align);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        align = in.readUnsignedShort();
    }
    
}