package at.stefl.commons.math.geometry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import at.stefl.commons.math.matrix.Matrix2d;
import at.stefl.commons.math.vector.Vector2d;

public class GeometryLineIntersection2D
        extends
        GeometryLineObjectIntersection2D<GeometryLine2D, GeometryLine2D, GeometryPoint2D, GeometryLine2D> {
    
    private final Set<GeometryPoint2D> intersectionPoints = new HashSet<GeometryPoint2D>();
    private final Set<GeometryLine2D> intersectionLines = new HashSet<GeometryLine2D>();
    
    public GeometryLineIntersection2D(GeometryLine2D line1, GeometryLine2D line2) {
        super(line1, line2);
    }
    
    @Override
    protected boolean testIntersectionImpl() {
        Vector2d a1 = geometryObject1.pointA;
        Vector2d b1 = geometryObject1.pointB;
        Vector2d a2 = geometryObject2.pointA;
        Vector2d b2 = geometryObject2.pointB;
        Vector2d ab1 = geometryObject1.vectorAB;
        Vector2d ab2 = geometryObject2.vectorAB;
        
        if ((a1.equals(b1)) && (a2.equals(b2)) && (a1.equals(a2))) {
            intersectionPoints.add(new GeometryPoint2D(a1));
            return true;
        }
        
        Vector2d coefficient1 = ab1;
        Vector2d coefficient2 = ab2.negate();
        Vector2d result = a2.sub(a1);
        
        Matrix2d A = new Matrix2d(coefficient1, coefficient2);
        Matrix2d A1 = new Matrix2d(result, coefficient2);
        Matrix2d A2 = new Matrix2d(coefficient1, result);
        
        double detA = A.determinant();
        double detA1 = A1.determinant();
        double detA2 = A2.determinant();
        
        if ((detA == 0d) & (detA1 == 0d) & (detA2 == 0d)) {
            intersectionLines.add(calcualteIntersectionLine(a1, b1, a2, b2));
            return true;
        }
        
        Vector2d lambda = new Vector2d(detA1, detA2).div(detA);
        
        boolean intersection = lambda.greaterThanOrEqual(new Vector2d(0d))
                .all() & lambda.lessThanOrEqual(new Vector2d(1d)).all();
        if (intersection) {
            intersectionPoints.add(new GeometryPoint2D(a1.add(ab1.mul(lambda
                    .getX()))));
            return true;
        }
        
        return false;
    }
    
    private static GeometryLine2D calcualteIntersectionLine(Vector2d a1,
            Vector2d b1, Vector2d a2, Vector2d b2) {
        ArrayList<Vector2d> linePoints = new ArrayList<Vector2d>();
        linePoints.add(a1);
        linePoints.add(b1);
        linePoints.add(a2);
        linePoints.add(b2);
        
        Vector2d middle = new Vector2d();
        for (Vector2d point : linePoints) {
            middle = middle.add(point);
        }
        middle = middle.div(linePoints.size());
        
        for (int i = 0; i < 2; i++) {
            double maxLength = 0;
            Vector2d removePoint = null;
            for (Vector2d point : linePoints) {
                Vector2d distance = middle.sub(point);
                double length = distance.length();
                if (length > maxLength) {
                    maxLength = length;
                    removePoint = point;
                }
            }
            linePoints.remove(removePoint);
        }
        
        return new GeometryLine2D(linePoints.get(0), linePoints.get(1));
    }
    
    @Override
    protected Set<GeometryPoint2D> calcIntersectionPointsImpl() {
        testIntersection();
        return intersectionPoints;
    }
    
    @Override
    protected Set<GeometryLine2D> calcIntersectionLinesImpl() {
        testIntersection();
        return intersectionLines;
    }
    
}