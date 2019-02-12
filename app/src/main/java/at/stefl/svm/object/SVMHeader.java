package at.stefl.svm.object;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.commons.util.PrimitiveUtil;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.basic.MapMode;

public class SVMHeader extends SVMVersionObject {
    
    private long compressionMode;
    private MapMode mapMode;
    private Vector2i size;
    private long actionCount;
    
    private short renderGraphicReplacements;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MetaHeader [compressionMode=");
        builder.append(compressionMode);
        builder.append(", mapMode=");
        builder.append(mapMode);
        builder.append(", size=");
        builder.append(size);
        builder.append(", actionCount=");
        builder.append(actionCount);
        builder.append(", renderGraphicReplacements=");
        builder.append(renderGraphicReplacements);
        builder.append("]");
        return builder.toString();
    }
    
    public long getCompressionMode() {
        return compressionMode;
    }
    
    public MapMode getMapMode() {
        return mapMode;
    }
    
    public Vector2i getSize() {
        return size;
    }
    
    public long getActionCount() {
        return actionCount;
    }
    
    public short getRenderGraphicReplacements() {
        return renderGraphicReplacements;
    }
    
    public void setCompressionMode(long compressionMode) {
        PrimitiveUtil.checkUnsignedInt(compressionMode);
        this.compressionMode = compressionMode;
    }
    
    public void setMapMode(MapMode mapMode) {
        this.mapMode = mapMode;
    }
    
    public void setSize(Vector2i size) {
        this.size = size;
    }
    
    public void setActionCount(long actionCount) {
        PrimitiveUtil.checkUnsignedInt(compressionMode);
        this.actionCount = actionCount;
    }
    
    public void setRenderGraphicReplacements(short renderGraphicReplacements) {
        PrimitiveUtil.checkUnsignedShort(renderGraphicReplacements);
        this.renderGraphicReplacements = renderGraphicReplacements;
    }
    
    @Override
    public SVMHeader deserialize(SVMDataInputStream in) throws IOException {
        return (SVMHeader) super.deserialize(in);
    }
    
    @Override
    public int getVersion() {
        return 2;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedInt(compressionMode);
        mapMode.serialize(out);
        out.writeSize(size);
        out.writeUnsignedInt(actionCount);
        
        // version 2
        out.writeUnsignedByte(renderGraphicReplacements);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        compressionMode = in.readUnsignedInt();
        mapMode = new MapMode();
        mapMode.deserialize(in);
        size = in.readSize();
        actionCount = in.readUnsignedInt();
        
        if (version >= 2) {
            renderGraphicReplacements = in.readUnsignedByte();
        }
    }
    
}