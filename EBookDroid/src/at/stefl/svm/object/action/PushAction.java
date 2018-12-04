package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.util.PrimitiveUtil;
import at.stefl.commons.util.string.StringUtil;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class PushAction extends SVMAction {
    
    private int flags;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PushAction [flags=");
        builder.append(StringUtil.fillFront(Integer.toBinaryString(flags), '0',
                16));
        builder.append("]");
        return builder.toString();
    }
    
    public int getFlags() {
        return flags;
    }
    
    public void setFlags(int flags) {
        PrimitiveUtil.checkUnsignedShort(flags);
        this.flags = flags;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShort(flags);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        flags = in.readUnsignedShort();
    }
    
}