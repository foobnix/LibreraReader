package at.stefl.commons.math.geometry;

import java.util.HashSet;
import java.util.Set;

public abstract class GeometryPointObjectIntersection2D<G1 extends GeometryPointObject2D, G2 extends GeometryPointObject2D, P extends GeometryPointObject2D>
        extends
        GeometryIntersection2D<G1, G2, P, GeometryLineObject2D, GeometryAreaObject2D> {
    
    public GeometryPointObjectIntersection2D(G1 geometryPointObject1,
            G2 geometryPointObject2) {
        super(geometryPointObject1, geometryPointObject2);
    }
    
    @Override
    protected final Set<GeometryLineObject2D> calcIntersectionLinesImpl() {
        return new HashSet<GeometryLineObject2D>();
    }
    
    @Override
    protected final Set<GeometryAreaObject2D> calcIntersectionAreasImpl() {
        return new HashSet<GeometryAreaObject2D>();
    }
    
}