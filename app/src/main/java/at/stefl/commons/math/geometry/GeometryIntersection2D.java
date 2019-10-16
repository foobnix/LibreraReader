package at.stefl.commons.math.geometry;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class GeometryIntersection2D<G1 extends GeometryObject2D, G2 extends GeometryObject2D, P extends GeometryObject2D, L extends GeometryLineObject2D, A extends GeometryAreaObject2D> {
    
    protected final G1 geometryObject1;
    protected final G2 geometryObject2;
    
    private boolean intersectionTested;
    private boolean intersection;
    
    private Set<P> intersectionPoints;
    private Set<L> intersectionLines;
    private Set<A> intersectionAreas;
    
    public GeometryIntersection2D(G1 geometryObject1, G2 geometryObject2) {
        this.geometryObject1 = geometryObject1;
        this.geometryObject2 = geometryObject2;
    }
    
    public G1 getGeometryObject1() {
        return geometryObject1;
    }
    
    public G2 getGeometryObject2() {
        return geometryObject2;
    }
    
    protected abstract boolean testIntersectionImpl();
    
    public final boolean testIntersection() {
        if (!intersectionTested) intersection = testIntersectionImpl();
        
        if (!intersection) {
            intersectionPoints = Collections.unmodifiableSet(new HashSet<P>());
            intersectionLines = Collections.unmodifiableSet(new HashSet<L>());
            intersectionAreas = Collections.unmodifiableSet(new HashSet<A>());
        }
        
        return intersection;
    }
    
    protected abstract Set<P> calcIntersectionPointsImpl();
    
    public final Set<P> calcIntersectionPoints() {
        if (intersectionPoints == null) {
            intersectionPoints = calcIntersectionPointsImpl();
            intersectionPoints = Collections
                    .unmodifiableSet(intersectionPoints);
        }
        
        return intersectionPoints;
    }
    
    protected abstract Set<L> calcIntersectionLinesImpl();
    
    public final Set<L> calcIntersectionLines() {
        if (intersectionLines == null) {
            intersectionLines = calcIntersectionLinesImpl();
            intersectionLines = Collections.unmodifiableSet(intersectionLines);
        }
        
        return intersectionLines;
    }
    
    protected abstract Set<A> calcIntersectionAreasImpl();
    
    public final Set<A> calcIntersectionAreas() {
        if (intersectionAreas == null) {
            intersectionAreas = calcIntersectionAreasImpl();
            intersectionAreas = Collections.unmodifiableSet(intersectionAreas);
        }
        
        return intersectionAreas;
    }
    
}