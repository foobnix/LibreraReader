package at.stefl.svm.object;

public class Color {
    
    private final int argb;
    
    public Color(int rgb) {
        this.argb = 0xff000000 | rgb;
    }
    
    public Color(int red, int green, int blue) {
        this.argb = 0xff000000 | ((red & 0xff) << 16) | ((green & 0xff) << 8)
                | ((blue & 0xff) << 0);
    }
    
    @Override
    public String toString() {
        return "[r=" + getRed() + ", g=" + getGreen() + ", b=" + getBlue()
                + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof Color)) return false;
        
        return argb == ((Color) obj).argb;
    }
    
    @Override
    public int hashCode() {
        return argb;
    }
    
    public int getARGB() {
        return argb;
    }
    
    public int getAlpha() {
        return (argb >> 24) & 0xff;
    }
    
    public int getRed() {
        return (argb >> 16) & 0xff;
    }
    
    public int getGreen() {
        return (argb >> 8) & 0xff;
    }
    
    public int getBlue() {
        return (argb >> 0) & 0xff;
    }
    
}