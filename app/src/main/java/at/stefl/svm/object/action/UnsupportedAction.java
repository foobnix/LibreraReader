package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.commons.util.array.ArrayUtil;
import at.stefl.svm.enumeration.ActionType;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

// TODO: remove me (debugging only)
public class UnsupportedAction extends SVMAction {
    
    private ActionType actionType;
    private int version;
    private byte[] data;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UnsupportedAction [actionType=");
        builder.append(actionType);
        builder.append(", version=");
        builder.append(version);
        builder.append(", length=");
        builder.append(data.length);
        builder.append(", data=");
        builder.append(ArrayUtil.toHexString(data));
        builder.append("]");
        return builder.toString();
    }
    
    @Override
    public ActionType getActionType() {
        return actionType;
    }
    
    @Override
    public int getVersion() {
        return version;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.write(data);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        this.version = version;
        
        if (length <= Integer.MAX_VALUE) {
            data = ByteStreamUtil.readFully(in, (int) length);
        } else {
            data = null;
        }
    }
    
}