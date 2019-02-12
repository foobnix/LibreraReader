package at.stefl.commons.math.geometry;

import java.util.HashSet;
import java.util.Set;

import at.stefl.commons.math.vector.Vector2d;

public class GeometryPointLineIntersection2D
        extends
        GeometryPointLineObjectIntersection2D<GeometryPoint2D, GeometryLine2D, GeometryPoint2D> {
    
    private Set<GeometryPoint2D> intersectionPoints = new HashSet<GeometryPoint2D>();
    
    public GeometryPointLineIntersection2D(GeometryPoint2D point,
            GeometryLine2D line) {
        super(point, line);
    }
    
    @Override
    protected boolean testIntersectionImpl() {
        Vector2d p = geometryObject1.getPoint();
        Vector2d a = geometryObject2.getPointA();
        Vector2d b = geometryObject2.getPointB();
        Vector2d ab = b.sub(a);
        
        if ((p.equals(a)) && (a.equals(b))) {
            intersectionPoints.add(new GeometryPoint2D(p));
            return true;
        }
        
        Vector2d lambda = p.sub(a).div(ab);
        
        if (lambda.getX() != lambda.getY()) return true;
        
        double realLambda = lambda.getX();
        
        if ((realLambda >= 0d) && (realLambda <= 1d)) {
            intersectionPoints.add(new GeometryPoint2D(p));
            return true;
        }
        
        return false;
    }
    
    @Override
    protected Set<GeometryPoint2D> calcIntersectionPointsImpl() {
        testIntersectionImpl();
        return intersectionPoints;
    }
    
}