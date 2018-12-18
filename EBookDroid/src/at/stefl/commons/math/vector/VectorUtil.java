package at.stefl.commons.math.vector;

public class VectorUtil {
    
    public static Vector2d radialComponent(Vector2d direction, Vector2d vector) {
        Vector2d directionNorm = direction.normalize();
        return directionNorm.mul(directionNorm.dot(vector));
    }
    
    public static Vector2d normalComponent(Vector2d direction, Vector2d vector) {
        return vector.sub(radialComponent(direction, vector));
    }
    
    public static void project(Vector2d direction, Vector2d vector,
            Vector2d[] result) {
        result[0] = radialComponent(direction, vector);
        result[1] = vector.sub(result[0]);
    }
    
    public static Vector2d[] project(Vector2d direction, Vector2d vector) {
        Vector2d[] result = new Vector2d[2];
        project(direction, vector, result);
        return result;
    }
    
}