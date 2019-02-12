package at.stefl.svm.object.action;

import java.io.IOException;

import at.stefl.svm.io.SVMDataInputStream;
import at.stefl.svm.io.SVMDataOutputStream;
import at.stefl.svm.object.basic.MapMode;

public class MapModeAction extends SVMAction {
    
    private MapMode mapMode;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MapModeAction [mapMode=");
        builder.append(mapMode);
        builder.append("]");
        return builder.toString();
    }
    
    public MapMode getMapMode() {
        return mapMode;
    }
    
    public void setMapMode(MapMode mapMode) {
        this.mapMode = mapMode;
    }
    
    @Override
    protected int getVersion() {
        return 1;
    }
    
    @Override
    protected void serializeContent(SVMDataOutputStream out) throws IOException {
        mapMode.serialize(out);
    }
    
    @Override
    protected void deserializeContent(SVMDataInputStream in, int version,
            long length) throws IOException {
        mapMode = new MapMode();
        mapMode.deserialize(in);
    }
    
}