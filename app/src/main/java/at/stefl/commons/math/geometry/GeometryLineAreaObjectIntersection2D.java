package at.stefl.commons.math.geometry;

import java.util.HashSet;
import java.util.Set;

public abstract class GeometryLineAreaObjectIntersection2D<G1 extends GeometryLineObject2D, G2 extends GeometryLineObject2D, P extends GeometryPointObject2D, L extends GeometryLineObject2D>
        extends GeometryIntersection2D<G1, G2, P, L, GeometryAreaObject2D> {
    
    public GeometryLineAreaObjectIntersection2D(G1 geometryLineObject1,
            G2 geometryLineObject2) {
        super(geometryLineObject1, geometryLineObject2);
    }
    
    @Override
    protected final Set<GeometryAreaObject2D> calcIntersectionAreasImpl() {
        return new HashSet<GeometryAreaObject2D>();
    }
    
}