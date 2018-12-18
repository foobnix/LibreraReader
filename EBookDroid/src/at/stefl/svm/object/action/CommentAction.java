package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.commons.util.array.ArrayUtil;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

public class CommentAction extends SVMAction {
    
    private String comment;
    private int value;
    private byte[] data;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CommentAction [comment=");
        builder.append(comment);
        builder.append(", value=");
        builder.append(value);
        builder.append(", data=");
        builder.append(ArrayUtil.toHexString(data));
        builder.append("]");
        return builder.toString();
    }
    
    public String getComment() {
        return comment;
    }
    
    public int getValue() {
        return value;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShortPrefixedAsciiString(comment);
        out.writeInt(value);
        out.writeUnsignedInt(data.length);
        out.write(data);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        comment = in.readUnsignedShortPrefixedAsciiString();
        value = in.readInt();
        long dataSize = in.readUnsignedInt();
        data = in.readFully((int) dataSize);
    }
    
}