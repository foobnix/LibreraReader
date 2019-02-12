package at.stefl.commons.math;


import at.stefl.commons.math.vector.Vector2d;

/**
 * Represents a rectangle with the center and the diagonal size.
 * 
 * @author Andreas Stefl
 */
public class Rectangle {
    
    private Vector2d center;
    private Vector2d size;
    
    /**
     * Creates a new <code>Rectangle</code> object with the given size.
     * 
     * @param size
     *            the diagonal size of the rectangle.
     */
    public Rectangle(Vector2d size) {
        this(new Vector2d(), size);
    }
    
    /**
     * Creates a new <code>Rectangle</code> object with the given center and
     * size.
     * 
     * @param center
     *            the center of the rectangle.
     * @param size
     *            the diagonal size of the rectangle.
     */
    public Rectangle(Vector2d center, Vector2d size) {
        if ((size.getX() < 0) || (size.getY() < 0)) throw new IllegalArgumentException(
                "The size cannot be negative.");
        
        this.center = center;
        this.size = size;
    }
    
    /**
     * Copies the given <code>Rectangle</code> object.
     * 
     * @param rectangle
     *            the <code>Rectangle</code> object to copy.
     */
    public Rectangle(Rectangle rectangle) {
        center = rectangle.center;
        size = rectangle.size;
    }
    
    
    /**
     * Returns the center and the size of the rectangle as a string.
     * 
     * @return the center and the size of the rectangle as a string.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("{");
        builder.append("center: ");
        builder.append(center);
        builder.append(", ");
        builder.append("size: ");
        builder.append(size);
        builder.append("}");
        
        return builder.toString();
    }
    
    /**
     * Tests the equality of the given object.
     * 
     * @param obj
     *            the object to test.
     * @return the equality as a boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        
        if (!(obj instanceof Rectangle)) return false;
        Rectangle rectangle = (Rectangle) obj;
        
        return center.equals(rectangle.center) && size.equals(rectangle.size);
    }
    
    /**
     * Returns the hash code of the object as an integer.
     * 
     * @return the hash code of the object as an integer.
     */
    @Override
    public int hashCode() {
        return center.hashCode() * (center.hashCode() + size.hashCode());
    }
    
    /**
     * Returns the center.
     * 
     * @return the center of the rectangle.
     */
    public Vector2d getCenter() {
        return center;
    }
    
    /**
     * Returns the diagonal size.
     * 
     * @return the diagonal size of the rectangle.
     */
    public Vector2d getSize() {
        return size;
    }
    
    /**
     * Returns the width.
     * 
     * @return the width of the rectangle.
     */
    public double getWidth() {
        return size.getX();
    }
    
    /**
     * Returns the height.
     * 
     * @return the height of the rectangle.
     */
    public double getHeight() {
        return size.getY();
    }
    
    /**
     * Returns the leftmost x value.
     * 
     * @return the leftmost x value of the rectangle.
     */
    public double left() {
        return center.getX() - size.getX() / 2;
    }
    
    /**
     * Returns the rightmost x value.
     * 
     * @return the rightmost x value of the rectangle.
     */
    public double right() {
        return center.getX() + size.getX() / 2;
    }
    
    /**
     * Returns the lowest y value.
     * 
     * @return the lowest y value of the rectangle.
     */
    public double top() {
        return center.getY() - size.getY() / 2;
    }
    
    /**
     * Returns the highest y value.
     * 
     * @return the highest y value of the rectangle.
     */
    public double bottom() {
        return center.getY() + size.getY() / 2;
    }
    
    /**
     * Returns a position vector to the left top.
     * 
     * @return a position vector to the left top of the rectangle.
     */
    public Vector2d leftTop() {
        return center.sub(size.div(2));
    }
    
    /**
     * Intersects the given point with the rectangle. If the point intersects
     * the rectangle <code>true</code> is returned, otherwise <code>false</code>
     * .
     * 
     * @param point
     *            the point as an position vector to intersect.
     * @return <code>true</code> if the point intersects the rectangle.
     *         Otherwise <code>false</code> is returned.
     */
    public boolean intersects(Vector2d point) {
        return point.sub(center).abs().lessThanOrEqual(size.div(2)).all();
    }
    
    /**
     * Intersects the given rectangle with this rectangle. If there is an
     * intersection <code>true</code> is returned, otherwise <code>false</code>.
     * 
     * @param rectangle
     *            the rectangle to intersect.
     * @return <code>true</code> if there is an intersection with this
     *         rectangle. Otherwise <code>false</code> is returned.
     */
    public boolean intersects(Rectangle rectangle) {
        Vector2d absDistance = center.sub(rectangle.center).abs();
        Vector2d maxDistance = size.add(rectangle.size).div(2);
        
        return absDistance.lessThanOrEqual(maxDistance).all();
    }
    
}