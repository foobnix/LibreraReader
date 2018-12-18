package at.stefl.commons.math.geometry;

import at.stefl.commons.math.matrix.Matrix3d;
import at.stefl.commons.math.vector.Vector2d;

public class GeometryLine2D extends GeometryLineObject2D {
    
    public final Vector2d pointA;
    public final Vector2d pointB;
    public final Vector2d vectorAB;
    public final Vector2d normal;
    
    public GeometryLine2D(Vector2d pointA, Vector2d pointB) {
        this.pointA = pointA;
        this.pointB = pointB;
        
        vectorAB = pointB.sub(pointA);
        normal = vectorAB.turnLeft().normalize();
    }
    
    @Override
    public String toString() {
        return GeometryLine2D.class.getCanonicalName() + "[" + pointA + ", "
                + pointB + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof GeometryLine2D)) return false;
        GeometryLine2D geometryLine2D = (GeometryLine2D) obj;
        
        return (pointA.equals(geometryLine2D.pointA) && pointB
                .equals(geometryLine2D.pointB))
                || (pointA.equals(geometryLine2D.pointB) && pointB
                        .equals(geometryLine2D.pointA));
    }
    
    @Override
    public int hashCode() {
        return pointA.hashCode() * pointB.hashCode();
    }
    
    public Vector2d getPointA() {
        return pointA;
    }
    
    public Vector2d getPointB() {
        return pointB;
    }
    
    public Vector2d getVectorAB() {
        return vectorAB;
    }
    
    public Vector2d getVectorBA() {
        return vectorAB.negate();
    }
    
    @Override
    public GeometryLine2D transform(Matrix3d transform) {
        Vector2d pointA = transform.mul(this.pointA);
        Vector2d pointB = transform.mul(this.pointB);
        
        return new GeometryLine2D(pointA, pointB);
    }
    
}