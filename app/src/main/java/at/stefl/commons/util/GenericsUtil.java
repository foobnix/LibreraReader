package at.stefl.commons.util;

public class GenericsUtil {
    
    @SuppressWarnings("unchecked")
    public static <T> T castObject(Object object) {
        return (T) object;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Class<T> castClass(Class<?> clazz) {
        return (Class<T>) clazz;
    }
    
    private GenericsUtil() {}
    
}