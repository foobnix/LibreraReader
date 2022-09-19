package at.stefl.commons.util.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import at.stefl.commons.util.object.ObjectTransformer;

// TODO: improve argument names
// TODO: call method by method, avoid redundant code?
public class CollectionUtil {
    public static <E> boolean addAll(Collection<? super E> c, E... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Boolean> c,
            boolean... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Byte> c, byte... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Character> c, char... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Short> c, short... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Integer> c, int... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Long> c, long... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Float> c, float... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Double> c, double... array) {
        boolean result = false;
        
        for (int i = 0; i < array.length; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static <E> boolean addAll(Collection<? super E> c, E[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Boolean> c,
            boolean[] array, int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Byte> c, byte[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Character> c, char[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Short> c, short[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Integer> c, int[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Long> c, long[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Float> c, float[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    public static boolean addAll(Collection<? super Double> c, double[] array,
            int off, int len) {
        int end = off + len;
        boolean result = false;
        
        for (int i = off; i < end; i++) {
            result |= c.add(array[i]);
        }
        
        return result;
    }
    
    // TODO: implement key/value generator
    // TODO: implement multiple put (put to multiple maps)
    public static <K, V> void putAll(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        V value;
        K key;
        
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            key = keyGenerator.transform(value);
            map.put(key, value);
        }
    }
    
    public static <K, V> void putAllNotNull(Map<? super K, ? super V> map,
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        V value;
        K key;
        
        for (int i = 0; i < values.length; i++) {
            value = values[i];
            key = keyGenerator.transform(value);
            if (key == null) continue;
            map.put(key, value);
        }
    }
    
    public static <K, V> HashMap<K, V> toHashMap(
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        HashMap<K, V> result = new HashMap<>();
        putAll(result, keyGenerator, values);
        return result;
    }
    
    public static <K, V> HashMap<K, V> toHashMapNotNull(
            ObjectTransformer<? super V, ? extends K> keyGenerator, V... values) {
        HashMap<K, V> result = new HashMap<>();
        putAllNotNull(result, keyGenerator, values);
        return result;
    }
    
    public static <V> void getNotNull(Map<?, ? extends V> map,
            Collection<? extends Object> keys, Collection<? super V> values) {
        for (Object key : keys) {
            V value = map.get(key);
            if (value != null) values.add(value);
        }
    }
    
    private CollectionUtil() {}
}