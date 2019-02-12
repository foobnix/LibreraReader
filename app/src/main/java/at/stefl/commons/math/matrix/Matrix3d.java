package at.stefl.commons.math.matrix;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.commons.math.vector.Vector3d;
import at.stefl.commons.util.string.StringUtil;

public class Matrix3d {
    
    public static final Matrix3d IDENTITY = new Matrix3d(1);
    
    private double m00, m01, m02;
    private double m10, m11, m12;
    private double m20, m21, m22;
    
    public Matrix3d() {}
    
    public Matrix3d(double diagonal) {
        m00 = diagonal;
        m11 = diagonal;
        m22 = diagonal;
    }
    
    public Matrix3d(double m00, double m10) {
        this.m00 = m00;
        this.m10 = m10;
    }
    
    public Matrix3d(double m00, double m10, double m20) {
        this.m00 = m00;
        this.m10 = m10;
        this.m20 = m20;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m20 = m20;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01, double m11) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        this.m20 = m20;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01, double m11,
            double m21) {
        this.m00 = m00;
        this.m01 = m01;
        this.m10 = m10;
        this.m11 = m11;
        this.m20 = m20;
        this.m21 = m21;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01, double m11,
            double m21, double m02) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m20 = m20;
        this.m21 = m21;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01, double m11,
            double m21, double m02, double m12) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01, double m11,
            double m21, double m02, double m12, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }
    
    public Matrix3d(double... components) {
        switch (components.length) {
        case 0:
            break;
        case 1:
            m00 = components[0];
            m11 = components[0];
            m22 = components[0];
            break;
        case 2:
            m00 = components[0];
            m10 = components[1];
            break;
        case 3:
            m00 = components[0];
            m10 = components[1];
            m20 = components[2];
            break;
        case 4:
            m00 = components[0];
            m01 = components[3];
            m10 = components[1];
            m20 = components[2];
            break;
        case 5:
            m00 = components[0];
            m01 = components[3];
            m10 = components[1];
            m11 = components[4];
            m20 = components[2];
            break;
        case 6:
            m00 = components[0];
            m01 = components[3];
            m10 = components[1];
            m11 = components[4];
            m20 = components[2];
            m21 = components[5];
            break;
        case 7:
            m00 = components[0];
            m01 = components[3];
            m02 = components[6];
            m10 = components[1];
            m11 = components[4];
            m20 = components[2];
            m21 = components[5];
            break;
        case 8:
            m00 = components[0];
            m01 = components[3];
            m02 = components[6];
            m10 = components[1];
            m11 = components[4];
            m12 = components[7];
            m20 = components[2];
            m21 = components[5];
            break;
        default:
            m00 = components[0];
            m01 = components[3];
            m02 = components[6];
            m10 = components[1];
            m11 = components[4];
            m12 = components[7];
            m20 = components[2];
            m21 = components[5];
            m22 = components[8];
            break;
        }
    }
    
    public Matrix3d(Vector3d column0, double m01, double m11, double m21,
            double m02, double m12, double m22) {
        this.m00 = column0.getX();
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = column0.getY();
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = column0.getZ();
        this.m21 = m21;
        this.m22 = m22;
    }
    
    public Matrix3d(double m00, double m10, double m20, Vector3d column1,
            double m02, double m12, double m22) {
        this.m00 = m00;
        this.m01 = column1.getX();
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = column1.getY();
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = column1.getZ();
        this.m22 = m22;
    }
    
    public Matrix3d(double m00, double m10, double m20, double m01, double m11,
            double m21, Vector3d column2) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = column2.getX();
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = column2.getY();
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = column2.getZ();
    }
    
    public Matrix3d(Vector3d column0, Vector3d column1, double m02, double m12,
            double m22) {
        this.m00 = column0.getX();
        this.m01 = column1.getX();
        this.m02 = m02;
        this.m10 = column0.getY();
        this.m11 = column1.getY();
        this.m12 = m12;
        this.m20 = column0.getZ();
        this.m21 = column1.getZ();
        this.m22 = m22;
    }
    
    public Matrix3d(Vector3d column0, double m01, double m11, double m21,
            Vector3d column2) {
        this.m00 = column0.getX();
        this.m01 = m01;
        this.m02 = column2.getX();
        this.m10 = column0.getY();
        this.m11 = m11;
        this.m12 = column2.getY();
        this.m20 = column0.getZ();
        this.m21 = m21;
        this.m22 = column2.getZ();
    }
    
    public Matrix3d(double m00, double m10, double m20, Vector3d column1,
            Vector3d column2) {
        this.m00 = m00;
        this.m01 = column1.getX();
        this.m02 = column2.getX();
        this.m10 = m10;
        this.m11 = column1.getY();
        this.m12 = column2.getY();
        this.m20 = m20;
        this.m21 = column1.getZ();
        this.m22 = column2.getZ();
    }
    
    public Matrix3d(Vector3d column0, Vector3d column1, Vector3d column2) {
        this.m00 = column0.getX();
        this.m01 = column1.getX();
        this.m02 = column2.getX();
        this.m10 = column0.getY();
        this.m11 = column1.getY();
        this.m12 = column2.getY();
        this.m20 = column0.getZ();
        this.m21 = column1.getZ();
        this.m22 = column2.getZ();
    }
    
    public Matrix3d(Matrix2d matrix) {
        this.m00 = matrix.getM00();
        this.m01 = matrix.getM01();
        this.m10 = matrix.getM10();
        this.m11 = matrix.getM11();
        this.m22 = 1d;
    }
    
    @Override
    public String toString() {
        return "[" + m00 + ", " + m01 + ", " + m02 + "]" + StringUtil.NEW_LINE
                + "[" + m10 + ", " + m11 + ", " + m12 + "]"
                + StringUtil.NEW_LINE + "[" + m20 + ", " + m21 + ", " + m22
                + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Matrix3d)) return false;
        Matrix3d matrix = (Matrix3d) obj;
        
        return (m00 == matrix.m00) && (m10 == matrix.m10)
                && (m20 == matrix.m20) && (m01 == matrix.m01)
                && (m11 == matrix.m11) && (m21 == matrix.m21)
                && (m02 == matrix.m02) && (m12 == matrix.m12)
                && (m22 == matrix.m22);
    }
    
    @Override
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(m00);
        bits += java.lang.Double.doubleToLongBits(m10) * 37;
        bits += java.lang.Double.doubleToLongBits(m20) * 41;
        bits += java.lang.Double.doubleToLongBits(m01) * 43;
        bits += java.lang.Double.doubleToLongBits(m11) * 47;
        bits += java.lang.Double.doubleToLongBits(m21) * 53;
        bits += java.lang.Double.doubleToLongBits(m02) * 59;
        bits += java.lang.Double.doubleToLongBits(m12) * 61;
        bits += java.lang.Double.doubleToLongBits(m22) * 67;
        return (((int) bits) ^ ((int) (bits >> 32)));
    }
    
    public double getM00() {
        return m00;
    }
    
    public double getM10() {
        return m10;
    }
    
    public double getM20() {
        return m20;
    }
    
    public double getM01() {
        return m01;
    }
    
    public double getM11() {
        return m11;
    }
    
    public double getM21() {
        return m21;
    }
    
    public double getM02() {
        return m02;
    }
    
    public double getM12() {
        return m12;
    }
    
    public double getM22() {
        return m22;
    }
    
    public Vector3d getColumn0() {
        return new Vector3d(m00, m10, m20);
    }
    
    public Vector3d getColumn1() {
        return new Vector3d(m01, m11, m21);
    }
    
    public Vector3d getColumn2() {
        return new Vector3d(m02, m12, m22);
    }
    
    public double[] getGLTransformMatrix() {
        return new double[] { m00, m10, m20, 0, m01, m11, m21, 0, 0, 0, 1, 0,
                m02, m12, 0, 1 };
    }
    
    public Matrix3d setM00(double m00) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM10(double m10) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM20(double m20) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM01(double m01) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM11(double m11) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM21(double m21) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM02(double m02) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM12(double m12) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setM22(double m22) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setColumn0(Vector3d column0) {
        return new Matrix3d(column0, m01, m11, m21, m02, m12, m22);
    }
    
    public Matrix3d setColumn1(Vector3d column1) {
        return new Matrix3d(m00, m10, m20, column1, m02, m12, m22);
    }
    
    public Matrix3d setColumn2(Vector3d column2) {
        return new Matrix3d(m00, m10, m20, m01, m11, m21, column2);
    }
    
    public double determinant() {
        return m00 * m11 * m22 + m01 * m12 * m20 + m02 * m10 * m21
                - (m02 * m11 * m20 + m01 * m10 * m22 + m00 * m12 * m21);
    }
    
    public Matrix3d negate() {
        return new Matrix3d(-m00, -m10, -m20, -m01, -m11, -m21, -m02, -m12,
                -m22);
    }
    
    public Matrix3d transpose() {
        return new Matrix3d(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }
    
    public Matrix3d add(double b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 + b;
        result.m01 = m01 + b;
        result.m02 = m02 + b;
        result.m10 = m10 + b;
        result.m11 = m11 + b;
        result.m12 = m12 + b;
        result.m20 = m20 + b;
        result.m21 = m21 + b;
        result.m22 = m22 + b;
        
        return result;
    }
    
    public Matrix3d add(Matrix3d b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 + b.m00;
        result.m01 = m01 + b.m01;
        result.m02 = m02 + b.m02;
        result.m10 = m10 + b.m10;
        result.m11 = m11 + b.m11;
        result.m12 = m12 + b.m12;
        result.m20 = m20 + b.m20;
        result.m21 = m21 + b.m21;
        result.m22 = m22 + b.m22;
        
        return result;
    }
    
    public Matrix3d sub(double b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 - b;
        result.m01 = m01 - b;
        result.m02 = m02 - b;
        result.m10 = m10 - b;
        result.m11 = m11 - b;
        result.m12 = m12 - b;
        result.m20 = m20 - b;
        result.m21 = m21 - b;
        result.m22 = m22 - b;
        
        return result;
    }
    
    public Matrix3d sub(Matrix3d b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 - b.m00;
        result.m01 = m01 - b.m01;
        result.m02 = m02 - b.m02;
        result.m10 = m10 - b.m10;
        result.m11 = m11 - b.m11;
        result.m12 = m12 - b.m12;
        result.m20 = m20 - b.m20;
        result.m21 = m21 - b.m21;
        result.m22 = m22 - b.m22;
        
        return result;
    }
    
    public Matrix3d mul(double b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 * b;
        result.m01 = m01 * b;
        result.m02 = m02 * b;
        result.m10 = m10 * b;
        result.m11 = m11 * b;
        result.m12 = m12 * b;
        result.m20 = m20 * b;
        result.m21 = m21 * b;
        result.m22 = m22 * b;
        
        return result;
    }
    
    public Vector2d mul(Vector2d b) {
        double x = m00 * b.getX() + m01 * b.getY() + m02;
        double y = m10 * b.getX() + m11 * b.getY() + m12;
        
        return new Vector2d(x, y);
    }
    
    public Vector3d mul(Vector3d b) {
        double x = m00 * b.getX() + m01 * b.getY() + m02 * b.getZ();
        double y = m10 * b.getX() + m11 * b.getY() + m12 * b.getZ();
        double z = m20 * b.getX() + m21 * b.getY() + m22 * b.getZ();
        
        return new Vector3d(x, y, z);
    }
    
    public Matrix3d mul(Matrix3d b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 * b.m00 + m01 * b.m10 + m02 * b.m20;
        result.m10 = m10 * b.m00 + m11 * b.m10 + m12 * b.m20;
        result.m20 = m20 * b.m00 + m21 * b.m10 + m22 * b.m20;
        result.m01 = m00 * b.m01 + m01 * b.m11 + m02 * b.m21;
        result.m11 = m10 * b.m01 + m11 * b.m11 + m12 * b.m21;
        result.m21 = m20 * b.m01 + m21 * b.m11 + m22 * b.m21;
        result.m02 = m00 * b.m02 + m01 * b.m12 + m02 * b.m22;
        result.m12 = m10 * b.m02 + m11 * b.m12 + m12 * b.m22;
        result.m22 = m20 * b.m02 + m21 * b.m12 + m22 * b.m22;
        
        return result;
    }
    
    public Matrix3d div(double b) {
        Matrix3d result = new Matrix3d();
        
        result.m00 = m00 / b;
        result.m01 = m01 / b;
        result.m02 = m02 / b;
        result.m10 = m10 / b;
        result.m11 = m11 / b;
        result.m12 = m12 / b;
        result.m20 = m20 / b;
        result.m21 = m21 / b;
        result.m22 = m22 / b;
        
        return result;
    }
    
}