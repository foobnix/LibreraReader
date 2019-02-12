package at.stefl.commons.math;

import at.stefl.commons.math.vector.Vector2d;

public class MathUtil {
    
    public static final double DEG2RAD = Math.PI / 180;
    
    public static final double RAD2DEG = 1 / DEG2RAD;
    
    public static double atan2(Vector2d vector) {
        return Math.atan2(vector.getY(), vector.getX());
    }
    
    public static int min(int... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        int min = Integer.MAX_VALUE;
        
        for (int i : array) {
            if (i < min) min = i;
        }
        
        return min;
    }
    
    public static long min(long... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        long min = Long.MAX_VALUE;
        
        for (long l : array) {
            if (l < min) min = l;
        }
        
        return min;
    }
    
    public static float min(float... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        float min = Float.MAX_VALUE;
        
        for (float f : array) {
            if (f < min) min = f;
        }
        
        return min;
    }
    
    public static double min(double... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        double min = Double.MAX_VALUE;
        
        for (double d : array) {
            if (d < min) min = d;
        }
        
        return min;
    }
    
    public static int max(int... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        int max = Integer.MAX_VALUE;
        
        for (int i : array) {
            if (i > max) max = i;
        }
        
        return max;
    }
    
    public static long max(long... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        long max = Long.MAX_VALUE;
        
        for (long l : array) {
            if (l > max) max = l;
        }
        
        return max;
    }
    
    public static float max(float... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        float max = Float.MAX_VALUE;
        
        for (float f : array) {
            if (f > max) max = f;
        }
        
        return max;
    }
    
    public static double max(double... array) {
        if (array.length == 0) throw new ArrayIndexOutOfBoundsException();
        
        double max = Double.MAX_VALUE;
        
        for (double d : array) {
            if (d > max) max = d;
        }
        
        return max;
    }
    
    public static int floor(int number, int divisor) {
        return (number / divisor) * divisor;
    }
    
    private MathUtil() {}
    
}