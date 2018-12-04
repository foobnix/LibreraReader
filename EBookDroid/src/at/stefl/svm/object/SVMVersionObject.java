package at.stefl.svm.object;

import java.io.IOException;

import at.stefl.commons.io.ByteStreamUtil;
import at.stefl.commons.io.CountingOutputStream;
import at.stefl.commons.io.LimitedInputStream;
import at.stefl.commons.io.NullOutputStream;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;

// TODO: check input/output size
// TODO: reduce in/out stream chain
public abstract class SVMVersionObject extends SVMObject {
    
    private long countLength() throws IOException {
        CountingOutputStream counter = new CountingOutputStream(
                NullOutputStream.NULL);
        serializeContent(new SVMDataOutputStream(counter));
        return counter.count();
    }
    
    @Override
    public void serialize(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShort(getVersion());
        out.writeUnsignedInt(countLength());
        
        serializeContent(out);
    }
    
    @Override
    public SVMVersionObject deserialize(SVMDataInputStream in)
            throws IOException {
        int version = in.readUnsignedShort();
        long length = in.readUnsignedInt();
        
        in = new SVMDataInputStream(new LimitedInputStream(in, length), in);
        deserializeContent(in, version, length);
        
        ByteStreamUtil.flushBytewise(in);
        
        return this;
    }
    
    protected abstract int getVersion();
    
    protected abstract void serializeContent(SVMDataOutputStream out)
            throws IOException;
    
    protected abstract void deserializeContent(SVMDataInputStream in,
            int version, long length) throws IOException;
    
}