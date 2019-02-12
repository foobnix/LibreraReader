package at.stefl.commons.math.vector;

public class Vector2i {
    
    public static final Vector2i NULL = new Vector2i();
    public static final Vector2i X = new Vector2i(1, 0);
    public static final Vector2i Y = new Vector2i(0, 1);
    
    private int x;
    private int y;
    
    public Vector2i() {}
    
    public Vector2i(int xy) {
        this(xy, xy);
    }
    
    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2i(Vector3i xy) {
        this(xy.getX(), xy.getY());
    }
    
    public Vector2i(Vector2d xy) {
        this((int) xy.getX(), (int) xy.getY());
    }
    
    public Vector2i(Vector3d xyz) {
        this((int) xyz.getX(), (int) xyz.getY());
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof Vector2i)) return false;
        Vector2i other = (Vector2i) obj;
        
        return (x == other.x) && (y == other.y);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(x);
        bits += java.lang.Double.doubleToLongBits(y) * 37;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public Vector2i getXY() {
        return this;
    }
    
    public Vector2i getYX() {
        return new Vector2i(y, x);
    }
    
    public Vector2i setX(int x) {
        return new Vector2i(x, y);
    }
    
    public Vector2i setY(int y) {
        return new Vector2i(x, y);
    }
    
    public Vector2i setXY(Vector2i xy) {
        return xy;
    }
    
    public Vector2i setYX(Vector2i yx) {
        return new Vector2i(yx.y, yx.x);
    }
    
    public Vector2b lessThan(Vector2i b) {
        return new Vector2b(x < b.x, y < b.y);
    }
    
    public Vector2b lessThanOrEqual(Vector2i b) {
        return new Vector2b(x <= b.x, y <= b.y);
    }
    
    public Vector2b greaterThan(Vector2i b) {
        return new Vector2b(x > b.x, y > b.y);
    }
    
    public Vector2b greaterThanOrEqual(Vector2i b) {
        return new Vector2b(x >= b.x, y >= b.y);
    }
    
    public Vector2b equal(Vector2i b) {
        return new Vector2b(x == b.x, y == b.y);
    }
    
    public Vector2b notEqual(Vector2i b) {
        return new Vector2b(x != b.x, y != b.y);
    }
    
    public double length() {
        return Math.sqrt(x * x + y * y);
    }
    
    public Vector2i negate() {
        return new Vector2i(-x, -y);
    }
    
    public Vector2i abs() {
        return new Vector2i(Math.abs(x), Math.abs(y));
    }
    
    public Vector2i min(Vector2i b) {
        return new Vector2i(Math.min(x, b.x), Math.min(y, b.y));
    }
    
    public Vector2i max(Vector2i b) {
        return new Vector2i(Math.max(x, b.x), Math.max(y, b.y));
    }
    
    public Vector2i turnLeft() {
        return new Vector2i(-y, x);
    }
    
    public Vector2i turnRight() {
        return new Vector2i(y, -x);
    }
    
    public Vector2i add(int b) {
        Vector2i result = new Vector2i();
        
        result.x = x + b;
        result.y = y + b;
        
        return result;
    }
    
    public Vector2i add(Vector2i b) {
        Vector2i result = new Vector2i();
        
        result.x = x + b.x;
        result.y = y + b.y;
        
        return result;
    }
    
    public Vector2i sub(int b) {
        Vector2i result = new Vector2i();
        
        result.x = x - b;
        result.y = y - b;
        
        return result;
    }
    
    public Vector2i sub(Vector2i b) {
        Vector2i result = new Vector2i();
        
        result.x = x - b.x;
        result.y = y - b.y;
        
        return result;
    }
    
    public Vector2i mul(int b) {
        Vector2i result = new Vector2i();
        
        result.x = x * b;
        result.y = y * b;
        
        return result;
    }
    
    public Vector2i mul(Vector2i b) {
        Vector2i result = new Vector2i();
        
        result.x = x * b.x;
        result.y = y * b.y;
        
        return result;
    }
    
    // TODO: implement
    // public Vector2i mul(Matrix2i b) {
    // Vector2i result = new Vector2i();
    //
    // result.x = x * b.getM00() + y * b.getM10();
    // result.y = x * b.getM01() + y * b.getM11();
    //
    // return result;
    // }
    
    public Vector2i div(int b) {
        Vector2i result = new Vector2i();
        
        result.x = x / b;
        result.y = y / b;
        
        return result;
    }
    
    public Vector2i div(Vector2i b) {
        Vector2i result = new Vector2i();
        
        result.x = x / b.x;
        result.y = y / b.y;
        
        return result;
    }
    
    public double dot(Vector2i b) {
        return x * b.x + y * b.y;
    }
    
    public Vector2d getAsVector2d() {
        return new Vector2d(this);
    }
    
}