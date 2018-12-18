package at.stefl.commons.math.geometry;

import at.stefl.commons.math.matrix.Matrix3d;
import at.stefl.commons.math.vector.Vector2d;

public class GeometryCircle2D extends GeometryLineObject2D {
    
    private final Vector2d center;
    private final double radius;
    
    public GeometryCircle2D(Vector2d center, double radius) {
        this.center = center;
        this.radius = radius;
    }
    
    @Override
    public String toString() {
        return GeometryCircle2D.class.getCanonicalName() + "[" + center + ", "
                + radius + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof GeometryCircle2D)) return false;
        GeometryCircle2D geometryCircle2D = (GeometryCircle2D) obj;
        
        return center.equals(geometryCircle2D.center)
                && (radius == geometryCircle2D.radius);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(radius);
        return center.hashCode() * (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public Vector2d getCenter() {
        return center;
    }
    
    public double getRadius() {
        return radius;
    }
    
    @Override
    public GeometryCircle2D transform(Matrix3d transform) {
        Vector2d center = transform.mul(this.center);
        
        return new GeometryCircle2D(center, radius);
    }
    
}