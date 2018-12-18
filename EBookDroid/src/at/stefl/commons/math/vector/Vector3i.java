package at.stefl.commons.math.vector;

public class Vector3i {
    
    public static final Vector3i NULL = new Vector3i();
    public static final Vector3i X = new Vector3i(1, 0, 0);
    public static final Vector3i Y = new Vector3i(0, 1, 0);
    public static final Vector3i Z = new Vector3i(0, 0, 1);
    
    private int x;
    private int y;
    private int z;
    
    public Vector3i() {}
    
    public Vector3i(int xyz) {
        this(xyz, xyz, xyz);
    }
    
    public Vector3i(int x, int y) {
        this(x, y, 0);
    }
    
    public Vector3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3i(Vector2i xy, int z) {
        this(xy.getX(), xy.getY(), z);
    }
    
    public Vector3i(int x, Vector2i yz) {
        this(x, yz.getX(), yz.getY());
    }
    
    public Vector3i(Vector3d xyz) {
        this((int) xyz.getX(), (int) xyz.getY(), (int) xyz.getZ());
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Vector3i)) return false;
        Vector3i vector = (Vector3i) obj;
        
        return (x == vector.x) && (y == vector.y) && (z == vector.z);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(x);
        bits += java.lang.Double.doubleToLongBits(y) * 37;
        bits += java.lang.Double.doubleToLongBits(z) * 41;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getZ() {
        return z;
    }
    
    public Vector2i getXY() {
        return new Vector2i(x, y);
    }
    
    public Vector2i getXZ() {
        return new Vector2i(x, z);
    }
    
    public Vector2i getYX() {
        return new Vector2i(y, x);
    }
    
    public Vector2i getYZ() {
        return new Vector2i(y, z);
    }
    
    public Vector2i getZX() {
        return new Vector2i(z, x);
    }
    
    public Vector2i getZY() {
        return new Vector2i(z, y);
    }
    
    public Vector3i getXYZ() {
        return this;
    }
    
    public Vector3i getXZY() {
        return new Vector3i(x, z, y);
    }
    
    public Vector3i getYXZ() {
        return new Vector3i(y, x, z);
    }
    
    public Vector3i getYZX() {
        return new Vector3i(y, z, x);
    }
    
    public Vector3i getZXY() {
        return new Vector3i(z, x, y);
    }
    
    public Vector3i getZYX() {
        return new Vector3i(z, y, x);
    }
    
    public Vector3i setX(int x) {
        return new Vector3i(x, y, z);
    }
    
    public Vector3i setY(int y) {
        return new Vector3i(x, y, z);
    }
    
    public Vector3i setZ(int z) {
        return new Vector3i(x, y, z);
    }
    
    public Vector3i setXY(Vector2i xy) {
        return new Vector3i(xy, z);
    }
    
    public Vector3i setXZ(Vector2i xz) {
        return new Vector3i(xz.getX(), y, xz.getY());
    }
    
    public Vector3i setYX(Vector2i yx) {
        return new Vector3i(yx.getY(), yx.getX(), z);
    }
    
    public Vector3i setYZ(Vector2i yz) {
        return new Vector3i(x, yz);
    }
    
    public Vector3i setZX(Vector2i zx) {
        return new Vector3i(zx.getY(), y, zx.getX());
    }
    
    public Vector3i setZY(Vector2i zy) {
        return new Vector3i(x, zy.getY(), zy.getX());
    }
    
    public Vector3i setXYZ(Vector3i xyz) {
        return xyz;
    }
    
    public Vector3i setXZY(Vector3i xzy) {
        return new Vector3i(xzy.x, xzy.z, xzy.y);
    }
    
    public Vector3i setYXZ(Vector3i yxz) {
        return new Vector3i(yxz.y, yxz.x, yxz.z);
    }
    
    public Vector3i setYZX(Vector3i yzx) {
        return new Vector3i(yzx.y, yzx.z, yzx.x);
    }
    
    public Vector3i setZXY(Vector3i zxy) {
        return new Vector3i(zxy.z, zxy.x, zxy.y);
    }
    
    public Vector3i setZYX(Vector3i zyx) {
        return new Vector3i(zyx.z, zyx.y, zyx.x);
    }
    
    public Vector3b lessThan(Vector3i b) {
        return new Vector3b(x < b.x, y < b.y, z < b.z);
    }
    
    public Vector3b lessThanOrEqual(Vector3i b) {
        return new Vector3b(x <= b.x, y <= b.y, z <= b.z);
    }
    
    public Vector3b greaterThan(Vector3i b) {
        return new Vector3b(x > b.x, y > b.y, z > b.z);
    }
    
    public Vector3b greaterThanOrEqual(Vector3i b) {
        return new Vector3b(x >= b.x, y >= b.y, z >= b.z);
    }
    
    public Vector3b equal(Vector3i b) {
        return new Vector3b(x == b.x, y == b.y, z == b.z);
    }
    
    public Vector3b notEqual(Vector3i b) {
        return new Vector3b(x != b.x, y != b.y, z != b.z);
    }
    
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    public Vector3i negate() {
        return new Vector3i(-x, -y, -z);
    }
    
    public Vector3i abs() {
        return new Vector3i(Math.abs(x), Math.abs(y), Math.abs(z));
    }
    
    public Vector3i min(Vector3i b) {
        return new Vector3i(Math.min(x, b.x), Math.min(y, b.y),
                Math.min(z, b.z));
    }
    
    public Vector3i max(Vector3i b) {
        return new Vector3i(Math.max(x, b.x), Math.max(y, b.y),
                Math.max(z, b.z));
    }
    
    public Vector3i add(int b) {
        Vector3i result = new Vector3i();
        
        result.x = x + b;
        result.y = y + b;
        result.z = z + b;
        
        return result;
    }
    
    public Vector3i add(Vector3i b) {
        Vector3i result = new Vector3i();
        
        result.x = x + b.x;
        result.y = y + b.y;
        result.z = z + b.z;
        
        return result;
    }
    
    public Vector3i sub(int b) {
        Vector3i result = new Vector3i();
        
        result.x = x - b;
        result.y = y - b;
        result.z = z - b;
        
        return result;
    }
    
    public Vector3i sub(Vector3i b) {
        Vector3i result = new Vector3i();
        
        result.x = x - b.x;
        result.y = y - b.y;
        result.z = z - b.z;
        
        return result;
    }
    
    public Vector3i mul(int b) {
        Vector3i result = new Vector3i();
        
        result.x = x * b;
        result.y = y * b;
        result.z = z * b;
        
        return result;
    }
    
    public Vector3i mul(Vector3i b) {
        Vector3i result = new Vector3i();
        
        result.x = x * b.x;
        result.y = y * b.y;
        result.z = z * b.z;
        
        return result;
    }
    
    // TODO: implement
    // public Vector3i mul(Matrix3i b) {
    // Vector3i result = new Vector3i();
    //
    // result.x = x * b.getM00() + y * b.getM10() + z * b.getM20();
    // result.y = x * b.getM01() + y * b.getM11() + z * b.getM21();
    // result.z = x * b.getM02() + y * b.getM12() + z * b.getM22();
    //
    // return result;
    // }
    
    public Vector3i div(int b) {
        Vector3i result = new Vector3i();
        
        result.x = x / b;
        result.y = y / b;
        result.z = z / b;
        
        return result;
    }
    
    public Vector3i div(Vector3i b) {
        Vector3i result = new Vector3i();
        
        result.x = x / b.x;
        result.y = y / b.y;
        result.z = z / b.z;
        
        return result;
    }
    
    public double dot(Vector3i b) {
        return x * b.x + y * b.y + z * b.z;
    }
    
    public Vector3i cross(Vector3i b) {
        return new Vector3i(y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y
                * b.x);
    }
    
    public Vector3d getAsVector3d() {
        return new Vector3d(this);
    }
    
}