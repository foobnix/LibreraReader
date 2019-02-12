package at.stefl.commons.math.geometry;

import at.stefl.commons.math.matrix.Matrix3d;
import at.stefl.commons.math.vector.Vector2d;

public class GeometryPoint2D extends GeometryPointObject2D {
    
    private final Vector2d point;
    
    public GeometryPoint2D(Vector2d point) {
        this.point = point;
    }
    
    @Override
    public String toString() {
        return GeometryPoint2D.class.getCanonicalName() + point;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof GeometryPoint2D)) return false;
        GeometryPoint2D geometryPoint2D = (GeometryPoint2D) obj;
        
        return point.equals(geometryPoint2D.point);
    }
    
    @Override
    public int hashCode() {
        return point.hashCode();
    }
    
    public Vector2d getPoint() {
        return point;
    }
    
    @Override
    public GeometryPoint2D transform(Matrix3d transform) {
        Vector2d point = transform.mul(this.point);
        
        return new GeometryPoint2D(point);
    }
    
}