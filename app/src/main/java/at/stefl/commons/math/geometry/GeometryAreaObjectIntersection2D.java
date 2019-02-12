package at.stefl.commons.math.geometry;

public abstract class GeometryAreaObjectIntersection2D<G1 extends GeometryAreaObject2D, G2 extends GeometryAreaObject2D, P extends GeometryPointObject2D, L extends GeometryLineObject2D, A extends GeometryAreaObject2D>
        extends GeometryIntersection2D<G1, G2, P, L, A> {
    
    public GeometryAreaObjectIntersection2D(G1 geometryAreaObject1,
            G2 geometryAreaObject2) {
        super(geometryAreaObject1, geometryAreaObject2);
    }
    
}