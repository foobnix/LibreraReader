package at.stefl.svm.object.basic;

import java.util.List;

import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.enumeration.PolygonFlag;

// TODO: extend MetaObject
// TODO: figure out format
public class Polygon {
    
    private List<Vector2i> points;
    private List<PolygonFlag> flags;
    
    public List<Vector2i> getPoints() {
        return points;
    }
    
    public boolean hasFlags() {
        return flags != null;
    }
    
    public List<PolygonFlag> getFlags() {
        return flags;
    }
    
    public void setPoints(List<Vector2i> points) {
        this.points = points;
    }
    
    public void setFlags(List<PolygonFlag> flags) {
        this.flags = flags;
    }
    
}