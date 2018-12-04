package at.stefl.commons.math.geometry;

import java.util.HashSet;
import java.util.Set;

public abstract class GeometryPointAreaObjectIntersection2D<G1 extends GeometryPointObject2D, G2 extends GeometryAreaObject2D, P extends GeometryPointObject2D>
        extends
        GeometryIntersection2D<G1, G2, P, GeometryLineObject2D, GeometryAreaObject2D> {
    
    public GeometryPointAreaObjectIntersection2D(G1 geometryPointObject,
            G2 geometryAreaObject) {
        super(geometryPointObject, geometryAreaObject);
    }
    
    @Override
    protected Set<GeometryLineObject2D> calcIntersectionLinesImpl() {
        return new HashSet<GeometryLineObject2D>();
    }
    
    @Override
    protected Set<GeometryAreaObject2D> calcIntersectionAreasImpl() {
        return new HashSet<GeometryAreaObject2D>();
    }
    
}