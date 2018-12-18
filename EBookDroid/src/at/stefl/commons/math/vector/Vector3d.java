package at.stefl.commons.math.vector;

import at.stefl.commons.math.MathUtil;
import at.stefl.commons.math.matrix.Matrix3d;

public class Vector3d {
    
    public static final Vector3d NULL = new Vector3d();
    public static final Vector3d X = new Vector3d(1, 0, 0);
    public static final Vector3d Y = new Vector3d(0, 1, 0);
    public static final Vector3d Z = new Vector3d(0, 0, 1);
    
    private double x;
    private double y;
    private double z;
    
    public Vector3d() {}
    
    public Vector3d(double all) {
        this(all, all, all);
    }
    
    public Vector3d(double x, double y) {
        this(x, y, 0);
    }
    
    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3d(double... components) {
        switch (components.length) {
        case 0:
            x = y = z = 0d;
            break;
        case 1:
            x = y = z = components[0];
            break;
        case 2:
            x = components[0];
            y = components[1];
            z = 0d;
            break;
        default:
            x = components[0];
            y = components[1];
            z = components[2];
            break;
        }
    }
    
    public Vector3d(Vector2d xy, double z) {
        this.x = xy.getX();
        this.y = xy.getY();
        this.z = z;
    }
    
    public Vector3d(double x, Vector2d yz) {
        this.x = x;
        this.y = yz.getX();
        this.z = yz.getY();
    }
    
    public Vector3d(Vector3i xyz) {
        this(xyz.getX(), xyz.getY(), xyz.getZ());
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Vector3d)) return false;
        Vector3d vector = (Vector3d) obj;
        
        return (x == vector.x) && (y == vector.y) && (z == vector.z);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(x);
        bits += java.lang.Double.doubleToLongBits(y) * 37;
        bits += java.lang.Double.doubleToLongBits(z) * 41;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public Vector2d getXY() {
        return new Vector2d(x, y);
    }
    
    public Vector2d getXZ() {
        return new Vector2d(x, z);
    }
    
    public Vector2d getYX() {
        return new Vector2d(y, x);
    }
    
    public Vector2d getYZ() {
        return new Vector2d(y, z);
    }
    
    public Vector2d getZX() {
        return new Vector2d(z, x);
    }
    
    public Vector2d getZY() {
        return new Vector2d(z, y);
    }
    
    public Vector3d getXYZ() {
        return this;
    }
    
    public Vector3d getXZY() {
        return new Vector3d(x, z, y);
    }
    
    public Vector3d getYXZ() {
        return new Vector3d(y, x, z);
    }
    
    public Vector3d getYZX() {
        return new Vector3d(y, z, x);
    }
    
    public Vector3d getZXY() {
        return new Vector3d(z, x, y);
    }
    
    public Vector3d getZYX() {
        return new Vector3d(z, y, x);
    }
    
    public Vector3d setX(double x) {
        return new Vector3d(x, y, z);
    }
    
    public Vector3d setY(double y) {
        return new Vector3d(x, y, z);
    }
    
    public Vector3d setZ(double z) {
        return new Vector3d(x, y, z);
    }
    
    public Vector3d setXY(Vector2d xy) {
        return new Vector3d(xy, z);
    }
    
    public Vector3d setXZ(Vector2d xz) {
        return new Vector3d(xz.getX(), y, xz.getY());
    }
    
    public Vector3d setYX(Vector2d yx) {
        return new Vector3d(yx.getY(), yx.getX(), z);
    }
    
    public Vector3d setYZ(Vector2d yz) {
        return new Vector3d(x, yz);
    }
    
    public Vector3d setZX(Vector2d zx) {
        return new Vector3d(zx.getY(), y, zx.getX());
    }
    
    public Vector3d setZY(Vector2d zy) {
        return new Vector3d(x, zy.getY(), zy.getX());
    }
    
    public Vector3d setXYZ(Vector3d xyz) {
        return xyz;
    }
    
    public Vector3d setXZY(Vector3d xzy) {
        return new Vector3d(xzy.x, xzy.z, xzy.y);
    }
    
    public Vector3d setYXZ(Vector3d yxz) {
        return new Vector3d(yxz.y, yxz.x, yxz.z);
    }
    
    public Vector3d setYZX(Vector3d yzx) {
        return new Vector3d(yzx.y, yzx.z, yzx.x);
    }
    
    public Vector3d setZXY(Vector3d zxy) {
        return new Vector3d(zxy.z, zxy.x, zxy.y);
    }
    
    public Vector3d setZYX(Vector3d zyx) {
        return new Vector3d(zyx.z, zyx.y, zyx.x);
    }
    
    public Vector3b lessThan(Vector3d b) {
        return new Vector3b(x < b.x, y < b.y, z < b.z);
    }
    
    public Vector3b lessThanOrEqual(Vector3d b) {
        return new Vector3b(x <= b.x, y <= b.y, z <= b.z);
    }
    
    public Vector3b greaterThan(Vector3d b) {
        return new Vector3b(x > b.x, y > b.y, z > b.z);
    }
    
    public Vector3b greaterThanOrEqual(Vector3d b) {
        return new Vector3b(x >= b.x, y >= b.y, z >= b.z);
    }
    
    public Vector3b equal(Vector3d b) {
        return new Vector3b(x == b.x, y == b.y, z == b.z);
    }
    
    public Vector3b notEqual(Vector3d b) {
        return new Vector3b(x != b.x, y != b.y, z != b.z);
    }
    
    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    public Vector3d negate() {
        return new Vector3d(-x, -y, -z);
    }
    
    public Vector3d abs() {
        return new Vector3d(Math.abs(x), Math.abs(y), Math.abs(z));
    }
    
    public Vector3d min(Vector3d b) {
        return new Vector3d(Math.min(x, b.x), Math.min(y, b.y),
                Math.min(z, b.z));
    }
    
    public Vector3d max(Vector3d b) {
        return new Vector3d(Math.max(x, b.x), Math.max(y, b.y),
                Math.max(z, b.z));
    }
    
    public Vector3d normalize() {
        double length = length();
        if (length == 0d) return new Vector3d();
        return new Vector3d(x / length, y / length, z / length);
    }
    
    public Vector3d radians() {
        Vector3d result = new Vector3d();
        
        result.x = x * MathUtil.DEG2RAD;
        result.y = y * MathUtil.DEG2RAD;
        result.z = z * MathUtil.DEG2RAD;
        
        return result;
    }
    
    public Vector3d degrees() {
        Vector3d result = new Vector3d();
        
        result.x = x * MathUtil.RAD2DEG;
        result.y = y * MathUtil.RAD2DEG;
        result.z = z * MathUtil.RAD2DEG;
        
        return result;
    }
    
    public Vector3d sin() {
        Vector3d result = new Vector3d();
        
        result.x = Math.sin(x);
        result.y = Math.sin(y);
        result.z = Math.sin(z);
        
        return result;
    }
    
    public Vector3d cos() {
        Vector3d result = new Vector3d();
        
        result.x = Math.cos(x);
        result.y = Math.cos(y);
        result.z = Math.cos(z);
        
        return result;
    }
    
    public Vector3d tan() {
        Vector3d result = new Vector3d();
        
        result.x = Math.tan(x);
        result.y = Math.tan(y);
        result.z = Math.tan(z);
        
        return result;
    }
    
    public Vector3d asin() {
        Vector3d result = new Vector3d();
        
        result.x = Math.asin(x);
        result.y = Math.asin(y);
        result.z = Math.asin(z);
        
        return result;
    }
    
    public Vector3d acos() {
        Vector3d result = new Vector3d();
        
        result.x = Math.acos(x);
        result.y = Math.acos(y);
        result.z = Math.acos(z);
        
        return result;
    }
    
    public Vector3d atan() {
        Vector3d result = new Vector3d();
        
        result.x = Math.atan(x);
        result.y = Math.atan(y);
        result.z = Math.atan(z);
        
        return result;
    }
    
    public Vector3d atan2(Vector3d b) {
        Vector3d result = new Vector3d();
        
        result.x = Math.atan2(x, b.x);
        result.y = Math.atan2(y, b.y);
        result.z = Math.atan2(z, b.z);
        
        return result;
    }
    
    public Vector3d add(double b) {
        Vector3d result = new Vector3d();
        
        result.x = x + b;
        result.y = y + b;
        result.z = z + b;
        
        return result;
    }
    
    public Vector3d add(Vector3d b) {
        Vector3d result = new Vector3d();
        
        result.x = x + b.x;
        result.y = y + b.y;
        result.z = z + b.z;
        
        return result;
    }
    
    public Vector3d sub(double b) {
        Vector3d result = new Vector3d();
        
        result.x = x - b;
        result.y = y - b;
        result.z = z - b;
        
        return result;
    }
    
    public Vector3d sub(Vector3d b) {
        Vector3d result = new Vector3d();
        
        result.x = x - b.x;
        result.y = y - b.y;
        result.z = z - b.z;
        
        return result;
    }
    
    public Vector3d mul(double b) {
        Vector3d result = new Vector3d();
        
        result.x = x * b;
        result.y = y * b;
        result.z = z * b;
        
        return result;
    }
    
    public Vector3d mul(Vector3d b) {
        Vector3d result = new Vector3d();
        
        result.x = x * b.x;
        result.y = y * b.y;
        result.z = z * b.z;
        
        return result;
    }
    
    public Vector3d mul(Matrix3d b) {
        Vector3d result = new Vector3d();
        
        result.x = x * b.getM00() + y * b.getM10() + z * b.getM20();
        result.y = x * b.getM01() + y * b.getM11() + z * b.getM21();
        result.z = x * b.getM02() + y * b.getM12() + z * b.getM22();
        
        return result;
    }
    
    public Vector3d div(double b) {
        Vector3d result = new Vector3d();
        
        result.x = x / b;
        result.y = y / b;
        result.z = z / b;
        
        return result;
    }
    
    public Vector3d div(Vector3d b) {
        Vector3d result = new Vector3d();
        
        result.x = x / b.x;
        result.y = y / b.y;
        result.z = z / b.z;
        
        return result;
    }
    
    public double dot(Vector3d b) {
        return x * b.x + y * b.y + z * b.z;
    }
    
    public Vector3d cross(Vector3d b) {
        return new Vector3d(y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y
                * b.x);
    }
    
    public Vector3i getAsVector3i() {
        return new Vector3i(this);
    }
    
}