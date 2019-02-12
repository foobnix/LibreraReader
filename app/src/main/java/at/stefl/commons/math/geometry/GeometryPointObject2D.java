package at.stefl.commons.math.geometry;

import at.stefl.commons.math.matrix.Matrix3d;

public abstract class GeometryPointObject2D extends GeometryObject2D {
    
    public GeometryPointObject2D() {}
    
    @Override
    public abstract GeometryPointObject2D transform(Matrix3d transform);
    
}