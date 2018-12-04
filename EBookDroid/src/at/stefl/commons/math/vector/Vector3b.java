package at.stefl.commons.math.vector;

public class Vector3b {
    
    private boolean x;
    private boolean y;
    private boolean z;
    
    public Vector3b() {
        x = y = z = false;
    }
    
    public Vector3b(boolean all) {
        x = y = z = all;
    }
    
    public Vector3b(boolean x, boolean y) {
        this.x = x;
        this.y = y;
        this.z = false;
    }
    
    public Vector3b(boolean x, boolean y, boolean z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3b(boolean... components) {
        switch (components.length) {
        case 0:
            x = y = z = false;
            break;
        case 1:
            x = y = z = components[0];
            break;
        case 2:
            x = components[0];
            y = components[1];
            z = false;
            break;
        default:
            x = components[0];
            y = components[1];
            z = components[2];
            break;
        }
    }
    
    public Vector3b(Vector3b xy, boolean z) {
        this.x = xy.x;
        this.y = xy.y;
        this.z = z;
    }
    
    public Vector3b(boolean x, Vector3b yz) {
        this.x = x;
        this.y = yz.x;
        this.z = yz.y;
    }
    
    public Vector3b(Vector3b xyz) {
        x = xyz.x;
        y = xyz.y;
        z = xyz.z;
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Vector3b)) return false;
        Vector3b vector = (Vector3b) obj;
        
        return (x == vector.x) && (y == vector.y) && (z == vector.z);
    }
    
    public boolean getX() {
        return x;
    }
    
    public boolean getY() {
        return y;
    }
    
    public boolean getZ() {
        return z;
    }
    
    public Vector2b getXY() {
        return new Vector2b(x, y);
    }
    
    public Vector2b getXZ() {
        return new Vector2b(x, z);
    }
    
    public Vector2b getYX() {
        return new Vector2b(y, x);
    }
    
    public Vector2b getYZ() {
        return new Vector2b(y, z);
    }
    
    public Vector2b getZX() {
        return new Vector2b(z, x);
    }
    
    public Vector2b getZY() {
        return new Vector2b(z, y);
    }
    
    public Vector3b getXYZ() {
        return this;
    }
    
    public Vector3b getXZY() {
        return new Vector3b(x, z, y);
    }
    
    public Vector3b getYXZ() {
        return new Vector3b(y, x, z);
    }
    
    public Vector3b getYZX() {
        return new Vector3b(y, z, x);
    }
    
    public Vector3b getZXY() {
        return new Vector3b(z, x, y);
    }
    
    public Vector3b getZYX() {
        return new Vector3b(z, y, x);
    }
    
    public Vector3b setX(boolean x) {
        return new Vector3b(x, y, z);
    }
    
    public Vector3b setY(boolean y) {
        return new Vector3b(x, y, z);
    }
    
    public Vector3b setZ(boolean z) {
        return new Vector3b(x, y, z);
    }
    
    public Vector3b setXY(Vector3b xy) {
        return new Vector3b(xy, z);
    }
    
    public Vector3b setXZ(Vector3b xz) {
        return new Vector3b(xz.x, y, xz.y);
    }
    
    public Vector3b setYX(Vector3b yx) {
        return new Vector3b(yx.y, yx.x, z);
    }
    
    public Vector3b setYZ(Vector3b yz) {
        return new Vector3b(x, yz);
    }
    
    public Vector3b setZX(Vector3b zx) {
        return new Vector3b(zx.y, y, zx.x);
    }
    
    public Vector3b setZY(Vector3b zy) {
        return new Vector3b(x, zy.y, zy.x);
    }
    
    public Vector3b setXYZ(Vector3b xyz) {
        return xyz;
    }
    
    public Vector3b setXZY(Vector3b xzy) {
        return new Vector3b(xzy.x, xzy.z, xzy.y);
    }
    
    public Vector3b setYXZ(Vector3b yxz) {
        return new Vector3b(yxz.y, yxz.x, yxz.z);
    }
    
    public Vector3b setYZX(Vector3b yzx) {
        return new Vector3b(yzx.y, yzx.z, yzx.x);
    }
    
    public Vector3b setZXY(Vector3b zxy) {
        return new Vector3b(zxy.z, zxy.x, zxy.y);
    }
    
    public Vector3b setZYX(Vector3b zyx) {
        return new Vector3b(zyx.z, zyx.y, zyx.x);
    }
    
    public Vector3b equal(Vector3b b) {
        Vector3b result = new Vector3b();
        
        result.x = x == b.x;
        result.y = y == b.y;
        result.z = z == b.z;
        
        return result;
    }
    
    public Vector3b notEqual(Vector3b b) {
        Vector3b result = new Vector3b();
        
        result.x = x != b.x;
        result.y = y != b.y;
        result.z = z != b.z;
        
        return result;
    }
    
    public boolean any() {
        return x | y | z;
    }
    
    public boolean all() {
        return x & y & z;
    }
    
    public Vector3b not() {
        return new Vector3b(!x, !y, !z);
    }
    
    public Vector3b or(Vector3b b) {
        Vector3b result = new Vector3b();
        
        result.x = x | b.x;
        result.y = y | b.y;
        result.z = z | b.z;
        
        return result;
    }
    
    public Vector3b and(Vector3b b) {
        Vector3b result = new Vector3b();
        
        result.x = x & b.x;
        result.y = y & b.y;
        result.z = z & b.z;
        
        return result;
    }
    
}