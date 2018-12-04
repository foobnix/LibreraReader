package at.stefl.commons.math.vector;


import at.stefl.commons.math.MathUtil;
import at.stefl.commons.math.matrix.Matrix2d;

public class Vector2d {
    
    public static final Vector2d NULL = new Vector2d();
    public static final Vector2d X = new Vector2d(1, 0);
    public static final Vector2d Y = new Vector2d(0, 1);
    
    private double x;
    private double y;
    
    public Vector2d() {
        x = y = 0d;
    }
    
    public Vector2d(double all) {
        x = y = all;
    }
    
    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2d(double... components) {
        switch (components.length) {
        case 0:
            x = y = 0d;
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
    
    public Vector2d(Vector3d xyz) {
        this(xyz.getX(), xyz.getY());
    }
    
    public Vector2d(Vector2i xy) {
        this(xy.getX(), xy.getY());
    }
    
    public Vector2d(Vector3i xyz) {
        this(xyz.getX(), xyz.getY());
    }
    
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Vector2d)) return false;
        Vector2d vector = (Vector2d) obj;
        
        return (x == vector.x) && (y == vector.y);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(x);
        bits += java.lang.Double.doubleToLongBits(y) * 37;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public Vector2d getXY() {
        return this;
    }
    
    public Vector2d getYX() {
        return new Vector2d(y, x);
    }
    
    public Vector2d setX(double x) {
        return new Vector2d(x, y);
    }
    
    public Vector2d setY(double y) {
        return new Vector2d(x, y);
    }
    
    public Vector2d setXY(Vector2d xy) {
        return xy;
    }
    
    public Vector2d setYX(Vector2d yx) {
        return new Vector2d(yx.y, yx.x);
    }
    
    public Vector2b lessThan(Vector2d b) {
        return new Vector2b(x < b.x, y < b.y);
    }
    
    public Vector2b lessThanOrEqual(Vector2d b) {
        return new Vector2b(x <= b.x, y <= b.y);
    }
    
    public Vector2b greaterThan(Vector2d b) {
        return new Vector2b(x > b.x, y > b.y);
    }
    
    public Vector2b greaterThanOrEqual(Vector2d b) {
        return new Vector2b(x >= b.x, y >= b.y);
    }
    
    public Vector2b equal(Vector2d b) {
        return new Vector2b(x == b.x, y == b.y);
    }
    
    public Vector2b notEqual(Vector2d b) {
        return new Vector2b(x != b.x, y != b.y);
    }
    
    public double length() {
        return Math.sqrt(x * x + y * y);
    }
    
    public Vector2d negate() {
        return new Vector2d(-x, -y);
    }
    
    public Vector2d abs() {
        return new Vector2d(Math.abs(x), Math.abs(y));
    }
    
    public Vector2d min(Vector2d b) {
        return new Vector2d(Math.min(x, b.x), Math.min(y, b.y));
    }
    
    public Vector2d max(Vector2d b) {
        return new Vector2d(Math.max(x, b.x), Math.max(y, b.y));
    }
    
    public Vector2d turnLeft() {
        return new Vector2d(-y, x);
    }
    
    public Vector2d turnRight() {
        return new Vector2d(y, -x);
    }
    
    public Vector2d normalize() {
        double length = length();
        if (length == 0d) return new Vector2d();
        return new Vector2d(x / length, y / length);
    }
    
    public Vector2d radians() {
        Vector2d result = new Vector2d();
        
        result.x = x * MathUtil.DEG2RAD;
        result.y = y * MathUtil.DEG2RAD;
        
        return result;
    }
    
    public Vector2d degrees() {
        Vector2d result = new Vector2d();
        
        result.x = x * MathUtil.RAD2DEG;
        result.y = y * MathUtil.RAD2DEG;
        
        return result;
    }
    
    public Vector2d sin() {
        Vector2d result = new Vector2d();
        
        result.x = Math.sin(x);
        result.y = Math.sin(y);
        
        return result;
    }
    
    public Vector2d cos() {
        Vector2d result = new Vector2d();
        
        result.x = Math.cos(x);
        result.y = Math.cos(y);
        
        return result;
    }
    
    public Vector2d tan() {
        Vector2d result = new Vector2d();
        
        result.x = Math.tan(x);
        result.y = Math.tan(y);
        
        return result;
    }
    
    public Vector2d asin() {
        Vector2d result = new Vector2d();
        
        result.x = Math.asin(x);
        result.y = Math.asin(y);
        
        return result;
    }
    
    public Vector2d acos() {
        Vector2d result = new Vector2d();
        
        result.x = Math.acos(x);
        result.y = Math.acos(y);
        
        return result;
    }
    
    public Vector2d atan() {
        Vector2d result = new Vector2d();
        
        result.x = Math.atan(x);
        result.y = Math.atan(y);
        
        return result;
    }
    
    public Vector2d atan2(Vector2d b) {
        Vector2d result = new Vector2d();
        
        result.x = Math.atan2(x, b.x);
        result.y = Math.atan2(y, b.y);
        
        return result;
    }
    
    public Vector2d add(double b) {
        Vector2d result = new Vector2d();
        
        result.x = x + b;
        result.y = y + b;
        
        return result;
    }
    
    public Vector2d add(Vector2d b) {
        Vector2d result = new Vector2d();
        
        result.x = x + b.x;
        result.y = y + b.y;
        
        return result;
    }
    
    public Vector2d sub(double b) {
        Vector2d result = new Vector2d();
        
        result.x = x - b;
        result.y = y - b;
        
        return result;
    }
    
    public Vector2d sub(Vector2d b) {
        Vector2d result = new Vector2d();
        
        result.x = x - b.x;
        result.y = y - b.y;
        
        return result;
    }
    
    public Vector2d mul(double b) {
        Vector2d result = new Vector2d();
        
        result.x = x * b;
        result.y = y * b;
        
        return result;
    }
    
    public Vector2d mul(Vector2d b) {
        Vector2d result = new Vector2d();
        
        result.x = x * b.x;
        result.y = y * b.y;
        
        return result;
    }
    
    public Vector2d mul(Matrix2d b) {
        Vector2d result = new Vector2d();
        
        result.x = x * b.getM00() + y * b.getM10();
        result.y = x * b.getM01() + y * b.getM11();
        
        return result;
    }
    
    public Vector2d div(double b) {
        Vector2d result = new Vector2d();
        
        result.x = x / b;
        result.y = y / b;
        
        return result;
    }
    
    public Vector2d div(Vector2d b) {
        Vector2d result = new Vector2d();
        
        result.x = x / b.x;
        result.y = y / b.y;
        
        return result;
    }
    
    public double dot(Vector2d b) {
        return x * b.x + y * b.y;
    }
    
}