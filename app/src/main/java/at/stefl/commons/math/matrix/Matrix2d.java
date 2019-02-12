package at.stefl.commons.math.matrix;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.commons.util.string.StringUtil;

public class Matrix2d {
    
    public static final Matrix2d IDENTITY = new Matrix2d(1);
    
    private double m00, m01;
    private double m10, m11;
    
    public Matrix2d() {
        m00 = 0d;
        m01 = 0d;
        m10 = 0d;
        m11 = 0d;
    }
    
    public Matrix2d(double diagonal) {
        m00 = diagonal;
        m01 = 0d;
        m10 = 0d;
        m11 = diagonal;
    }
    
    public Matrix2d(double m00, double m10) {
        this.m00 = m00;
        this.m01 = 0d;
        this.m10 = m10;
        this.m11 = 0d;
    }
    
    public Matrix2d(double m00, double m10, double m01) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = 0d;
    }
    
    public Matrix2d(double m00, double m10, double m01, double m11) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
    }
    
    public Matrix2d(double... components) {
        switch (components.length) {
        case 0:
            m00 = 0d;
            m01 = 0d;
            m10 = 0d;
            m11 = 0d;
            break;
        case 1:
            m00 = components[0];
            m01 = 0d;
            m10 = 0d;
            m11 = components[0];
            break;
        case 2:
            m00 = components[0];
            m01 = 0d;
            m10 = components[1];
            m11 = 0d;
            break;
        case 3:
            m00 = components[0];
            m01 = components[2];
            m10 = components[1];
            m11 = 0d;
            break;
        default:
            m00 = components[0];
            m01 = components[2];
            m10 = components[1];
            m11 = components[3];
            break;
        }
    }
    
    public Matrix2d(Vector2d column0, double m01, double m11) {
        this.m00 = column0.getX();
        this.m01 = m01;
        this.m10 = column0.getY();
        this.m11 = m11;
    }
    
    public Matrix2d(double m00, double m10, Vector2d column1) {
        this.m00 = m00;
        this.m01 = column1.getX();
        this.m10 = m10;
        this.m11 = column1.getY();
    }
    
    public Matrix2d(Vector2d column0, Vector2d column1) {
        this.m00 = column0.getX();
        this.m01 = column1.getX();
        this.m10 = column0.getY();
        this.m11 = column1.getY();
    }
    
    public Matrix2d(Matrix3d matrix3) {
        this.m00 = matrix3.getM00();
        this.m01 = matrix3.getM01();
        this.m10 = matrix3.getM10();
        this.m11 = matrix3.getM11();
    }
    
    @Override
    public String toString() {
        return "[" + m00 + ", " + m01 + "]" + StringUtil.NEW_LINE + "[" + m10
                + ", " + m11 + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Matrix2d)) return false;
        Matrix2d matrix = (Matrix2d) obj;
        
        return (m00 == matrix.m00) && (m10 == matrix.m10)
                && (m01 == matrix.m01) && (m11 == matrix.m11);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(m00);
        bits += java.lang.Double.doubleToLongBits(m10) * 37;
        bits += java.lang.Double.doubleToLongBits(m01) * 41;
        bits += java.lang.Double.doubleToLongBits(m11) * 43;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public double getM00() {
        return m00;
    }
    
    public double getM10() {
        return m10;
    }
    
    public double getM01() {
        return m01;
    }
    
    public double getM11() {
        return m11;
    }
    
    public Vector2d getColumn0() {
        return new Vector2d(m00, m10);
    }
    
    public Vector2d getColumn1() {
        return new Vector2d(m01, m11);
    }
    
    public Matrix2d setM00(double m00) {
        return new Matrix2d(m00, m10, m01, m11);
    }
    
    public Matrix2d setM10(double m10) {
        return new Matrix2d(m00, m10, m01, m11);
    }
    
    public Matrix2d setM01(double m01) {
        return new Matrix2d(m00, m10, m01, m11);
    }
    
    public Matrix2d setM11(double m11) {
        return new Matrix2d(m00, m10, m01, m11);
    }
    
    public Matrix2d setColumn0(Vector2d column0) {
        return new Matrix2d(column0, m01, m11);
    }
    
    public Matrix2d setColumn1(Vector2d column1) {
        return new Matrix2d(m00, m10, column1);
    }
    
    public double determinant() {
        return m00 * m11 - m01 * m10;
    }
    
    public Matrix2d negate() {
        return new Matrix2d(-m00, -m10, -m01, -m11);
    }
    
    public Matrix2d transpose() {
        return new Matrix2d(m00, m01, m10, m11);
    }
    
    public Matrix2d add(double b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 + b;
        result.m01 = m01 + b;
        result.m10 = m10 + b;
        result.m11 = m11 + b;
        
        return result;
    }
    
    public Matrix2d add(Matrix2d b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 + b.m00;
        result.m01 = m01 + b.m01;
        result.m10 = m10 + b.m10;
        result.m11 = m11 + b.m11;
        
        return result;
    }
    
    public Matrix2d sub(double b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 - b;
        result.m01 = m01 - b;
        result.m10 = m10 - b;
        result.m11 = m11 - b;
        
        return result;
    }
    
    public Matrix2d sub(Matrix2d b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 - b.m00;
        result.m01 = m01 - b.m01;
        result.m10 = m10 - b.m10;
        result.m11 = m11 - b.m11;
        
        return result;
    }
    
    public Matrix2d mul(double b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 * b;
        result.m01 = m01 * b;
        result.m10 = m10 * b;
        result.m11 = m11 * b;
        
        return result;
    }
    
    public Vector2d mul(Vector2d b) {
        double x = m00 * b.getX() + m01 * b.getY();
        double y = m10 * b.getX() + m11 * b.getY();
        
        return new Vector2d(x, y);
    }
    
    public Matrix2d mul(Matrix2d b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 * b.m00 + m01 * b.m10;
        result.m10 = m10 * b.m00 + m11 * b.m10;
        result.m01 = m00 * b.m01 + m01 * b.m11;
        result.m11 = m10 * b.m01 + m11 * b.m11;
        
        return result;
    }
    
    public Matrix2d div(double b) {
        Matrix2d result = new Matrix2d();
        
        result.m00 = m00 / b;
        result.m01 = m01 / b;
        result.m10 = m10 / b;
        result.m11 = m11 / b;
        
        return result;
    }
    
    public static Matrix2d rotation(double angle) {
        return new Matrix2d(Math.cos(angle), Math.sin(angle), -Math.sin(angle),
                Math.cos(angle));
    }
    
}