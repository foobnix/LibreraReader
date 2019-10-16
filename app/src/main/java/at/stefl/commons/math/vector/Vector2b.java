package at.stefl.commons.math.vector;

public class Vector2b {
    
    private boolean x;
    private boolean y;
    
    public Vector2b() {
        x = y = false;
    }
    
    public Vector2b(boolean all) {
        x = y = all;
    }
    
    public Vector2b(boolean x, boolean y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2b(boolean... components) {
        switch (components.length) {
        case 0:
            x = y = false;
            break;
        case 1:
            x = y = components[0];
            break;
        default:
            x = components[0];
            y = components[1];
            break;
        }
    }
    
    public Vector2b(Vector2b xy) {
        x = xy.x;
        y = xy.y;
    }
    
    public Vector2b(Vector3b xyz) {
        x = xyz.getX();
        y = xyz.getY();
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Vector2b)) return false;
        Vector2b vector = (Vector2b) obj;
        
        return (x == vector.x) && (y == vector.y);
    }
    
    public boolean getX() {
        return x;
    }
    
    public boolean getY() {
        return y;
    }
    
    public Vector2b getXY() {
        return this;
    }
    
    public Vector2b getYX() {
        return new Vector2b(y, x);
    }
    
    public Vector2b setX(boolean x) {
        return new Vector2b(x, y);
    }
    
    public Vector2b setY(boolean y) {
        return new Vector2b(x, y);
    }
    
    public Vector2b setXY(Vector2b xy) {
        return xy;
    }
    
    public Vector2b setYX(Vector2b yx) {
        return new Vector2b(yx.y, yx.x);
    }
    
    public Vector2b equal(Vector2b b) {
        Vector2b result = new Vector2b();
        
        result.x = x == b.x;
        result.y = y == b.y;
        
        return result;
    }
    
    public Vector2b notEqual(Vector2b b) {
        Vector2b result = new Vector2b();
        
        result.x = x != b.x;
        result.y = y != b.y;
        
        return result;
    }
    
    public boolean any() {
        return x | y;
    }
    
    public boolean all() {
        return x & y;
    }
    
    public Vector2b not() {
        return new Vector2b(!x, !y);
    }
    
    public Vector2b or(Vector2b b) {
        Vector2b result = new Vector2b();
        
        result.x = x | b.x;
        result.y = y | b.y;
        
        return result;
    }
    
    public Vector2b and(Vector2b b) {
        Vector2b result = new Vector2b();
        
        result.x = x & b.x;
        result.y = y & b.y;
        
        return result;
    }
    
}