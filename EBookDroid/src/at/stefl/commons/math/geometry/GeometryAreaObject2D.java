package at.stefl.commons.math.geometry;

import at.stefl.commons.math.matrix.Matrix3d;

public abstract class GeometryAreaObject2D extends GeometryObject2D {
    
    public GeometryAreaObject2D() {}
    
    @Override
    public abstract GeometryAreaObject2D transform(Matrix3d transform);
    
}