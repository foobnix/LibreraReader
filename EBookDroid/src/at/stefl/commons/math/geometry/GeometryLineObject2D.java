package at.stefl.commons.math.geometry;

import at.stefl.commons.math.matrix.Matrix3d;

public abstract class GeometryLineObject2D extends GeometryObject2D {
    
    public GeometryLineObject2D() {}
    
    @Override
    public abstract GeometryLineObject2D transform(Matrix3d transform);
    
}