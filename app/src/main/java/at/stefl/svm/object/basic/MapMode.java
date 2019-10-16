package at.stefl.svm.object.basic;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.commons.util.PrimitiveUtil;
import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.Fraction;
import at.stefl.svm.object.SVMVersionObject;

public class MapMode extends SVMVersionObject {
    
    private int unit;
    private Vector2i origin;
    private Fraction scaleX;
    private Fraction scaleY;
    private boolean simple;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MapMode [unit=");
        builder.append(unit);
        builder.append(", origin=");
        builder.append(origin);
        builder.append(", scaleX=");
        builder.append(scaleX);
        builder.append(", scaleY=");
        builder.append(scaleY);
        builder.append(", isSimple=");
        builder.append(simple);
        builder.append("]");
        return builder.toString();
    }
    
    public int getUnit() {
        return unit;
    }
    
    public Vector2i getOrigin() {
        return origin;
    }
    
    public Fraction getScaleX() {
        return scaleX;
    }
    
    public Fraction getScaleY() {
        return scaleY;
    }
    
    public boolean isSimple() {
        return simple;
    }
    
    public void setUnit(int unit) {
        PrimitiveUtil.checkUnsignedShort(unit);
        this.unit = unit;
    }
    
    public void setOrigin(Vector2i origin) {
        this.origin = origin;
    }
    
    public void setScaleX(Fraction scaleX) {
        this.scaleX = scaleX;
    }
    
    public void setScaleY(Fraction scaleY) {
        this.scaleY = scaleY;
    }
    
    public void setSimple(boolean isSimple) {
        this.simple = isSimple;
    }
    
    @Override
    public int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        out.writeUnsignedShort(unit);
        out.writePoint(origin);
        out.writeFraction(scaleX);
        out.writeFraction(scaleY);
        out.writeBoolean(simple);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        unit = in.readUnsignedShort();
        origin = in.readPoint();
        scaleX = in.readFraction();
        scaleY = in.readFraction();
        simple = in.readBoolean();
    }
    
}