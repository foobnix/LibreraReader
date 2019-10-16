package at.stefl.commons.math;

import at.stefl.commons.math.vector.Vector2d;

public class RectangleD {
    
    private final double left;
    private final double top;
    private final double right;
    private final double bottom;
    
    public RectangleD(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
    
    public RectangleD(RectangleI r) {
        this(r.getLeft(), r.getTop(), r.getRight(), r.getBottom());
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long tmp;
        
        tmp = Double.doubleToLongBits(bottom);
        result = prime * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(left);
        result = prime * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(right);
        result = prime * result + (int) (tmp ^ (tmp >>> 32));
        tmp = Double.doubleToLongBits(top);
        result = prime * result + (int) (tmp ^ (tmp >>> 32));
        
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (getClass() != obj.getClass()) return false;
        RectangleD other = (RectangleD) obj;
        
        if (bottom != other.bottom) return false;
        if (left != other.left) return false;
        if (right != other.right) return false;
        if (top != other.top) return false;
        
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("RectangeD [left=");
        builder.append(left);
        builder.append(", top=");
        builder.append(top);
        builder.append(", right=");
        builder.append(right);
        builder.append(", bottom=");
        builder.append(bottom);
        builder.append("]");
        
        return builder.toString();
    }
    
    public double getLeft() {
        return left;
    }
    
    public double getTop() {
        return top;
    }
    
    public double getRight() {
        return right;
    }
    
    public double getBottom() {
        return bottom;
    }
    
    public Vector2d getCenter() {
        return new Vector2d((left + right) / 2d, (top + bottom) / 2d);
    }
    
    public Vector2d getLeftTop() {
        return new Vector2d(left, top);
    }
    
    public Vector2d getRightTop() {
        return new Vector2d(right, top);
    }
    
    public Vector2d getRightBottom() {
        return new Vector2d(right, bottom);
    }
    
    public Vector2d getLeftBottom() {
        return new Vector2d(left, bottom);
    }
    
    public double getWidth() {
        return right - left;
    }
    
    public double getHeight() {
        return bottom - top;
    }
    
    public Vector2d getSize() {
        return new Vector2d(getWidth(), getHeight());
    }
    
    public RectangleI getAsRectangleI() {
        return new RectangleI(this);
    }
    
}