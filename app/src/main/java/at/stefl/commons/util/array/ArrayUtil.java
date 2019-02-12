package at.stefl.commons.util.array;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;

import at.stefl.commons.math.MathUtil;
import at.stefl.commons.util.collection.CollectionUtil;

// TODO: improve attribute names
// TODO: implement array methods with offset and length
// TODO: avoid redundant code?
public class ArrayUtil {
    
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = {};
    public static final byte[] EMPTY_BYTE_ARRAY = {};
    public static final char[] EMPTY_CHAR_ARRAY = {};
    public static final short[] EMPTY_SHORT_ARRAY = {};
    public static final int[] EMPTY_INT_ARRAY = {};
    public static final long[] EMPTY_LONG_ARRAY = {};
    public static final float[] EMPTY_FLOAT_ARRAY = {};
    public static final double[] EMPTY_DOUBLE_ARRAY = {};
    public static final Object[] EMPTY_OBJECT_ARRAY = {};
    
    private static final Map<Class<?>, Object> EMPTY_ARRAY_MAP = new HashMap<Class<?>, Object>();
    
    private static final char[] HEX_ARRAY = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
    static {
        EMPTY_ARRAY_MAP.put(boolean.class, EMPTY_BOOLEAN_ARRAY);
        EMPTY_ARRAY_MAP.put(byte.class, EMPTY_BYTE_ARRAY);
        EMPTY_ARRAY_MAP.put(char.class, EMPTY_CHAR_ARRAY);
        EMPTY_ARRAY_MAP.put(short.class, EMPTY_SHORT_ARRAY);
        EMPTY_ARRAY_MAP.put(int.class, EMPTY_INT_ARRAY);
        EMPTY_ARRAY_MAP.put(long.class, EMPTY_LONG_ARRAY);
        EMPTY_ARRAY_MAP.put(float.class, EMPTY_FLOAT_ARRAY);
        EMPTY_ARRAY_MAP.put(double.class, EMPTY_DOUBLE_ARRAY);
        EMPTY_ARRAY_MAP.put(Object.class, EMPTY_OBJECT_ARRAY);
    }
    
    public static boolean validateArguments(int size, int off, int len) {
    	if (off < 0) return false;
    	if (len < 0) return false;
    	if ((off + len) > size) return false;
    	return true;
    }
    
    public static void checkArguments(int size, int off, int len) {
    	if (off < 0) throw new IllegalArgumentException("off < 0");
    	if (len < 0) throw new IllegalArgumentException("len < 0");
    	if (off >= size) throw new IllegalArgumentException("off out of bounds: " + off);
    	if (off + len > size) throw new IllegalArgumentException("off+len out of bounds: " + (off + len));
    }
    
    @SuppressWarnings("unchecked")
    public static <T, E> T getEmptyArray(Class<E> clazz) {
        Object result = EMPTY_ARRAY_MAP.get(clazz);
        
        if (result == null) {
            result = Array.newInstance(clazz, 0);
            EMPTY_ARRAY_MAP.put(clazz, result);
        }
        
        return (T) result;
    }
    
    public static <E> E getFirstNotNull(E... array) {
        E element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            if (element != null) return element;
        }
        
        return null;
    }
    
    public static <E> E getFirstNotNull(E[] array, int off, int len) {
        int end = off + len;
        E element;
        
        for (int i = off; i < end; i++) {
            element = array[i];
            if (element != null) return element;
        }
        
        return null;
    }
    
    // TODO: fix types
    public static int getEqualCount(Object object, Object... array) {
        int result = 0;
        
        for (int i = 0; i < array.length; i++) {
            if (object.equals(array[i])) result++;
        }
        
        return result;
    }
    
    public static int getEqualCount(Object object, Object[] array, int off,
            int len) {
        int end = off + len;
        int result = 0;
        
        for (int i = off; i < end; i++) {
            if (object.equals(array[i])) result++;
        }
        
        return result;
    }
    
    public static int getReferenceCount(Object object, Object... array) {
        int result = 0;
        
        for (int i = 0; i < array.length; i++) {
            if (object == array[i]) result++;
        }
        
        return result;
    }
    
    public static int getReferenceCount(Object object, Object[] array, int off,
            int len) {
        int end = off + len;
        int result = 0;
        
        for (int i = off; i < end; i++) {
            if (object == array[i]) result++;
        }
        
        return result;
    }
    
    public static int getNullCount(Object... array) {
        int result = 0;
        
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) result++;
        }
        
        return result;
    }
    
    public static int getNullCount(Object[] array, int off, int len) {
        int end = off + len;
        int result = 0;
        
        for (int i = off; i < end; i++) {
            if (array[i] == null) result++;
        }
        
        return result;
    }
    
    public static int getNotNullCount(Object... array) {
        int result = 0;
        
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null) result++;
        }
        
        return result;
    }
    
    public static int getNotNullCount(Object[] array, int off, int len) {
        int end = off + len;
        int result = 0;
        
        for (int i = off; i < end; i++) {
            if (array[i] != null) result++;
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static <E> E[] getNotNullArray(E... array) {
        int resultLength = getNotNullCount(array);
        E[] result = (E[]) Array.newInstance(array.getClass()
                .getComponentType(), resultLength);
        E element;
        
        for (int i = 0, j = 0; i < array.length; i++) {
            element = array[i];
            if (element != null) result[j++] = element;
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public static <E> E[] getNotNullArray(E[] array, int off, int len) {
        int end = off + len;
        int resultLength = getNotNullCount(array, off, len);
        E[] result = (E[]) Array.newInstance(array.getClass()
                .getComponentType(), resultLength);
        E element;
        
        for (int i = off, j = 0; i < end; i++) {
            element = array[i];
            if (element != null) result[j++] = element;
        }
        
        return result;
    }
    
    public static <T extends Collection<? super E>, E> T toCollection(T c,
            E... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Boolean>, E> T toCollection(
            T c, boolean... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Byte>, E> T toCollection(T c,
            byte... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Character>, E> T toCollection(
            T c, char... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Short>, E> T toCollection(T c,
            short... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Integer>, E> T toCollection(
            T c, int... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Long>, E> T toCollection(T c,
            long... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Float>, E> T toCollection(T c,
            float... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super Double>, E> T toCollection(T c,
            double... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }
    
    public static <T extends Collection<? super E>, E> T toCollection(T c,
            E[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Boolean>, E> T toCollection(
            T c, boolean[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Byte>, E> T toCollection(T c,
            byte[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Character>, E> T toCollection(
            T c, char[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Short>, E> T toCollection(T c,
            short[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Integer>, E> T toCollection(
            T c, int[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Long>, E> T toCollection(T c,
            long[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Float>, E> T toCollection(T c,
            float[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <T extends Collection<? super Double>, E> T toCollection(T c,
            double[] array, int off, int len) {
        CollectionUtil.addAll(c, array, off, len);
        return c;
    }
    
    public static <E> HashSet<E> toHashSet(E... array) {
        return toCollection(new HashSet<E>(array.length), array);
    }
    
    public static HashSet<Boolean> toHashSet(boolean... array) {
        return toCollection(new HashSet<Boolean>(array.length), array);
    }
    
    public static HashSet<Byte> toHashSet(byte... array) {
        return toCollection(new HashSet<Byte>(array.length), array);
    }
    
    public static HashSet<Character> toHashSet(char... array) {
        return toCollection(new HashSet<Character>(array.length), array);
    }
    
    public static HashSet<Short> toHashSet(short... array) {
        return toCollection(new HashSet<Short>(array.length), array);
    }
    
    public static HashSet<Integer> toHashSet(int... array) {
        return toCollection(new HashSet<Integer>(array.length), array);
    }
    
    public static HashSet<Long> toHashSet(long... array) {
        return toCollection(new HashSet<Long>(array.length), array);
    }
    
    public static HashSet<Float> toHashSet(float... array) {
        return toCollection(new HashSet<Float>(array.length), array);
    }
    
    public static HashSet<Double> toHashSet(double... array) {
        return toCollection(new HashSet<Double>(array.length), array);
    }
    
    public static <E> HashSet<E> toHashSet(E[] array, int off, int len) {
        return toCollection(new HashSet<E>(array.length), array, off, len);
    }
    
    public static HashSet<Boolean> toHashSet(boolean[] array, int off, int len) {
        return toCollection(new HashSet<Boolean>(array.length), array, off, len);
    }
    
    public static HashSet<Byte> toHashSet(byte[] array, int off, int len) {
        return toCollection(new HashSet<Byte>(array.length), array, off, len);
    }
    
    public static HashSet<Character> toHashSet(char[] array, int off, int len) {
        return toCollection(new HashSet<Character>(array.length), array, off,
                len);
    }
    
    public static HashSet<Short> toHashSet(short[] array, int off, int len) {
        return toCollection(new HashSet<Short>(array.length), array, off, len);
    }
    
    public static HashSet<Integer> toHashSet(int[] array, int off, int len) {
        return toCollection(new HashSet<Integer>(array.length), array, off, len);
    }
    
    public static HashSet<Long> toHashSet(long[] array, int off, int len) {
        return toCollection(new HashSet<Long>(array.length), array, off, len);
    }
    
    public static HashSet<Float> toHashSet(float[] array, int off, int len) {
        return toCollection(new HashSet<Float>(array.length), array, off, len);
    }
    
    public static HashSet<Double> toHashSet(double[] array, int off, int len) {
        return toCollection(new HashSet<Double>(array.length), array, off, len);
    }
    
    public static <E> ArrayList<E> toArrayList(E... array) {
        return toCollection(new ArrayList<E>(array.length), array);
    }
    
    public static ArrayList<Boolean> toArrayList(boolean... array) {
        return toCollection(new ArrayList<Boolean>(array.length), array);
    }
    
    public static ArrayList<Byte> toArrayList(byte... array) {
        return toCollection(new ArrayList<Byte>(array.length), array);
    }
    
    public static ArrayList<Character> toArrayList(char... array) {
        return toCollection(new ArrayList<Character>(array.length), array);
    }
    
    public static ArrayList<Short> toArrayList(short... array) {
        return toCollection(new ArrayList<Short>(array.length), array);
    }
    
    public static ArrayList<Integer> toArrayList(int... array) {
        return toCollection(new ArrayList<Integer>(array.length), array);
    }
    
    public static ArrayList<Long> toArrayList(long... array) {
        return toCollection(new ArrayList<Long>(array.length), array);
    }
    
    public static ArrayList<Float> toArrayList(float... array) {
        return toCollection(new ArrayList<Float>(array.length), array);
    }
    
    public static ArrayList<Double> toArrayList(double... array) {
        return toCollection(new ArrayList<Double>(array.length), array);
    }
    
    public static <E> ArrayList<E> toArrayList(E[] array, int off, int len) {
        return toCollection(new ArrayList<E>(array.length), array, off, len);
    }
    
    public static ArrayList<Boolean> toArrayList(boolean[] array, int off,
            int len) {
        return toCollection(new ArrayList<Boolean>(array.length), array, off,
                len);
    }
    
    public static ArrayList<Byte> toArrayList(byte[] array, int off, int len) {
        return toCollection(new ArrayList<Byte>(array.length), array, off, len);
    }
    
    public static ArrayList<Character> toArrayList(char[] array, int off,
            int len) {
        return toCollection(new ArrayList<Character>(array.length), array, off,
                len);
    }
    
    public static ArrayList<Short> toArrayList(short[] array, int off, int len) {
        return toCollection(new ArrayList<Short>(array.length), array, off, len);
    }
    
    public static ArrayList<Integer> toArrayList(int[] array, int off, int len) {
        return toCollection(new ArrayList<Integer>(array.length), array, off,
                len);
    }
    
    public static ArrayList<Long> toArrayList(long[] array, int off, int len) {
        return toCollection(new ArrayList<Long>(array.length), array, off, len);
    }
    
    public static ArrayList<Float> toArrayList(float[] array, int off, int len) {
        return toCollection(new ArrayList<Float>(array.length), array, off, len);
    }
    
    public static ArrayList<Double> toArrayList(double[] array, int off, int len) {
        return toCollection(new ArrayList<Double>(array.length), array, off,
                len);
    }
    
    public static <E extends Comparable<E>> E getGreatest(E... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        E result = array[0];
        E element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element.compareTo(result) > 0) result = element;
        }
        
        return result;
    }
    
    public static <E> E getGreatest(Comparator<? super E> comparator,
            E... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        E result = array[0];
        E element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (comparator.compare(element, result) > 0) result = element;
        }
        
        return result;
    }
    
    public static <E extends Comparable<E>> E getGreatestNotNull(E... array) {
        E result = null;
        E element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            if (element == null) continue;
            if ((result == null) || (element.compareTo(result) > 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static <E> E getGreatestNotNull(Comparator<? super E> comparator,
            E... array) {
        E result = null;
        E element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            if (element == null) continue;
            if ((result == null) || (comparator.compare(element, result) > 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static boolean getGreatest(boolean... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        for (int i = 0; i < array.length; i++) {
            if (array[i]) return true;
        }
        
        return false;
    }
    
    public static byte getGreatest(byte... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        byte result = array[0];
        byte element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static char getGreatest(char... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        char result = array[0];
        char element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static short getGreatest(short... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        short result = array[0];
        short element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static int getGreatest(int... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        int result = array[0];
        int element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static long getGreatest(long... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        long result = array[0];
        long element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static float getGreatest(float... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        float result = array[0];
        float element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static double getGreatest(double... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        double result = array[0];
        double element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element > result) result = element;
        }
        
        return result;
    }
    
    public static <E extends Comparable<? super E>> E getSmallest(E... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        E result = array[0];
        E element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element.compareTo(result) < 0) result = element;
        }
        
        return result;
    }
    
    public static <E> E getSmallest(Comparator<? super E> comparator,
            E... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        E result = array[0];
        E element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (comparator.compare(element, result) < 0) result = element;
        }
        
        return result;
    }
    
    public static <E extends Comparable<? super E>> E getSmallestNotNull(
            E... array) {
        E result = null;
        E element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            if (element == null) continue;
            if ((result == null) || (element.compareTo(result) < 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static <E> E getSmallestNotNull(Comparator<? super E> comparator,
            E... array) {
        E result = null;
        E element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            if (element == null) continue;
            if ((result == null) || (comparator.compare(element, result) < 0)) result = element;
        }
        
        if (result == null) throw new NoSuchElementException();
        
        return result;
    }
    
    public static boolean getSmallest(boolean... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        for (int i = 0; i < array.length; i++) {
            if (!array[i]) return false;
        }
        
        return true;
    }
    
    public static byte getSmallest(byte... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        byte result = array[0];
        byte element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Byte.MIN_VALUE) return Byte.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    public static char getSmallest(char... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        char result = array[0];
        char element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Character.MIN_VALUE) return Character.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    public static short getSmallest(short... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        short result = array[0];
        short element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Short.MIN_VALUE) return Short.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    public static int getSmallest(int... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        int result = array[0];
        int element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Integer.MIN_VALUE) return Integer.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    public static long getSmallest(long... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        long result = array[0];
        long element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Long.MIN_VALUE) return Long.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    public static float getSmallest(float... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        float result = array[0];
        float element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Float.MIN_VALUE) return Float.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    public static double getSmallest(double... array) {
        if (array.length == 0) throw new NoSuchElementException();
        
        double result = array[0];
        double element;
        
        for (int i = 1; i < array.length; i++) {
            element = array[i];
            if (element == Double.MIN_VALUE) return Double.MIN_VALUE;
            if (element < result) result = element;
        }
        
        return result;
    }
    
    // TODO: handle exceptions?
    public static boolean equals(Object[] array1, int off1, Object[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        Object element1;
        Object element2;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            element1 = array1[i1];
            element2 = array2[i2];
            if (!((element1 == null) ? (element2 == null) : element1
                    .equals(element2))) return false;
        }
        
        return true;
    }
    
    public static boolean equals(boolean[] array1, int off1, boolean[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(byte[] array1, int off1, byte[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(char[] array1, int off1, char[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(short[] array1, int off1, short[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(int[] array1, int off1, int[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(long[] array1, int off1, long[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(float[] array1, int off1, float[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static boolean equals(double[] array1, int off1, double[] array2,
            int off2, int len) {
        if (array1 == array2) return true;
        if ((array1 == null) || (array2 == null)) return false;
        
        int end1 = off1 + len;
        
        for (int i1 = off1, i2 = off2; i2 < end1; i1++, i2++) {
            if (array1[i1] != array2[i2]) return false;
        }
        
        return true;
    }
    
    public static int hashCode(Object... array) {
        if (array == null) return 0;
        
        int result = 1;
        Object element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            result = 31 * result + ((element == null) ? 0 : element.hashCode());
        }
        
        return result;
    }
    
    public static int hashCode(boolean... array) {
        if (array == null) return 0;
        
        int result = 1;
        
        for (int i = 0; i < array.length; i++) {
            result = 31 * result + (array[i] ? 1231 : 1237);
        }
        
        return result;
    }
    
    public static int hashCode(byte... array) {
        if (array == null) return 0;
        
        int result = 1;
        
        for (int i = 0; i < array.length; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(char... array) {
        if (array == null) return 0;
        
        int result = 1;
        
        for (int i = 0; i < array.length; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(short... array) {
        if (array == null) return 0;
        
        int result = 1;
        
        for (int i = 0; i < array.length; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(int... array) {
        if (array == null) return 0;
        
        int result = 1;
        
        for (int i = 0; i < array.length; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(long... array) {
        if (array == null) return 0;
        
        int result = 1;
        long element;
        
        for (int i = 0; i < array.length; i++) {
            element = array[i];
            result = 31 * result + (int) (element ^ (element >>> 32));
        }
        
        return result;
    }
    
    public static int hashCode(float... array) {
        if (array == null) return 0;
        
        int result = 1;
        
        for (int i = 0; i < array.length; i++) {
            result = 31 * result + Float.floatToIntBits(array[i]);
        }
        
        return result;
    }
    
    public static int hashCode(double... array) {
        if (array == null) return 0;
        
        int result = 1;
        long bits;
        
        for (int i = 0; i < array.length; i++) {
            bits = Double.doubleToLongBits(array[i]);
            result = 31 * result + (int) (bits ^ (bits >>> 32));
        }
        
        return result;
    }
    
    // TODO: handle exception
    public static int hashCode(Object[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        Object element;
        
        for (int i = off; i < end; i++) {
            element = array[i];
            result = 31 * result + ((element == null) ? 0 : element.hashCode());
        }
        
        return result;
    }
    
    public static int hashCode(boolean[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            result = 31 * result + (array[i] ? 1231 : 1237);
        }
        
        return result;
    }
    
    public static int hashCode(byte[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(char[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(short[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(int[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            result = 31 * result + array[i];
        }
        
        return result;
    }
    
    public static int hashCode(long[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        long element;
        
        for (int i = off; i < end; i++) {
            element = array[i];
            result = 31 * result + (int) (element ^ (element >>> 32));
        }
        
        return result;
    }
    
    public static int hashCode(float[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        
        for (int i = off; i < end; i++) {
            result = 31 * result + Float.floatToIntBits(array[i]);
        }
        
        return result;
    }
    
    public static int hashCode(double[] array, int off, int len) {
        if (array == null) return 0;
        
        int result = 1;
        int end = off + len;
        long bits;
        
        for (int i = off; i < end; i++) {
            bits = Double.doubleToLongBits(array[i]);
            result = 31 * result + (int) (bits ^ (bits >>> 32));
        }
        
        return result;
    }
    
    public static void swap(Object[] array, int i, int j) {
        Object tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(boolean[] array, int i, int j) {
        boolean tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(byte[] array, int i, int j) {
        byte tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(char[] array, int i, int j) {
        char tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(short[] array, int i, int j) {
        short tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(int[] array, int i, int j) {
        int tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(long[] array, int i, int j) {
        long tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(float[] array, int i, int j) {
        float tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void swap(double[] array, int i, int j) {
        double tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }
    
    public static void turn(Object[] array) {
        Object tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(boolean[] array) {
        boolean tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(byte[] array) {
        byte tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(char[] array) {
        char tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(short[] array) {
        short tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(int[] array) {
        int tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(long[] array) {
        long tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(float[] array) {
        float tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(double[] array) {
        double tmp;
        
        for (int i = 0, j = array.length - 1; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(Object[] array, int off, int len) {
        int last = off + len - 1;
        Object tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(boolean[] array, int off, int len) {
        int last = off + len - 1;
        boolean tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(byte[] array, int off, int len) {
        int last = off + len - 1;
        byte tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(char[] array, int off, int len) {
        int last = off + len - 1;
        char tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(short[] array, int off, int len) {
        int last = off + len - 1;
        short tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(int[] array, int off, int len) {
        int last = off + len - 1;
        int tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(long[] array, int off, int len) {
        int last = off + len - 1;
        long tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(float[] array, int off, int len) {
        int last = off + len - 1;
        float tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    public static void turn(double[] array, int off, int len) {
        int last = off + len - 1;
        double tmp;
        
        for (int i = off, j = last; i < j; i++, j--) {
            tmp = array[i];
            array[i] = array[j];
            array[j] = tmp;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<T> arrayClass, int length) {
        if (!arrayClass.isArray()) throw new IllegalArgumentException(
                "not an array");
        return (T) Array.newInstance(arrayClass.getComponentType(), length);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T copyOf(T array, int newLength) {
        if (newLength < 0) throw new NegativeArraySizeException();
        
        int length = Array.getLength(array);
        T newArray = newInstance((Class<T>) array.getClass(), newLength);
        System.arraycopy(array, 0, newArray, 0, Math.min(length, newLength));
        
        return newArray;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T copyOfRange(T array, int from, int to) {
        if (from < 0) throw new ArrayIndexOutOfBoundsException(from);
        
        int length = Array.getLength(array);
        if (from > length) throw new ArrayIndexOutOfBoundsException(from);
        
        int newLength = to - from;
        if (newLength < 0) throw new IllegalArgumentException();
        
        T newArray = newInstance((Class<T>) array.getClass(), newLength);
        System.arraycopy(array, from, newArray, 0, Math.min(length, newLength));
        
        return newArray;
    }
    
    public static <T> T growDirect(T array, int newLength) {
        int length = Array.getLength(array);
        if (length >= newLength) return array;
        
        return copyOf(array, newLength);
    }
    
    public static <T> T growArithmetic(T array, int newLength, int stepSize) {
        int length = Array.getLength(array);
        if (length >= newLength) return array;
        
        newLength = length + stepSize
                + MathUtil.floor(newLength - length, stepSize);
        return copyOf(array, newLength);
    }
    
    public static <T> T growGeometric(T array, int newLength, int base) {
        int length = Array.getLength(array);
        if (length >= newLength) return array;
        
        int addExponent = (int) Math.ceil(((double) newLength / length)
                * (1d / base));
        newLength = (int) (length * Math.pow(base, addExponent));
        return copyOf(array, newLength);
    }
    
    // TODO: implement all toString
    
    public static String toHexString(byte[] array) {
        return toStringHex(array, 0, array.length);
    }
    
    public static String toStringHex(byte[] array, int off, int len) {
        char[] result = new char[len << 1];
        int end = off + len;
        
        for (int i = off, b, j = 0; i < end; i++) {
            b = array[i] & 0xff;
            result[j++] = HEX_ARRAY[b >> 4];
            result[j++] = HEX_ARRAY[b & 0x0f];
        }
        
        return new String(result);
    }
    
    public static byte[] toByteArray(char[] array) {
        return toByteArray(array, 0, array.length);
    }
    
    public static byte[] toByteArray(char[] array, int off, int len) {
        return toByteArray(array, off, len, new byte[len], 0);
    }
    
    public static byte[] toByteArray(char[] from, int fromOff, int len,
            byte[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (byte) from[i];
        }
        
        return to;
    }
    
    public static byte[] toByteArray(short[] array) {
        return toByteArray(array, 0, array.length);
    }
    
    public static byte[] toByteArray(short[] array, int off, int len) {
        return toByteArray(array, off, len, new byte[len], 0);
    }
    
    public static byte[] toByteArray(short[] from, int fromOff, int len,
            byte[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (byte) from[i];
        }
        
        return to;
    }
    
    public static byte[] toByteArray(int[] array) {
        return toByteArray(array, 0, array.length);
    }
    
    public static byte[] toByteArray(int[] array, int off, int len) {
        return toByteArray(array, off, len, new byte[len], 0);
    }
    
    public static byte[] toByteArray(int[] from, int fromOff, int len,
            byte[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (byte) from[i];
        }
        
        return to;
    }
    
    public static byte[] toByteArray(long[] array) {
        return toByteArray(array, 0, array.length);
    }
    
    public static byte[] toByteArray(long[] array, int off, int len) {
        return toByteArray(array, off, len, new byte[len], 0);
    }
    
    public static byte[] toByteArray(long[] from, int fromOff, int len,
            byte[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (byte) from[i];
        }
        
        return to;
    }
    
    public static byte[] toByteArray(float[] array) {
        return toByteArray(array, 0, array.length);
    }
    
    public static byte[] toByteArray(float[] array, int off, int len) {
        return toByteArray(array, off, len, new byte[len], 0);
    }
    
    public static byte[] toByteArray(float[] from, int fromOff, int len,
            byte[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (byte) from[i];
        }
        
        return to;
    }
    
    public static byte[] toByteArray(double[] array) {
        return toByteArray(array, 0, array.length);
    }
    
    public static byte[] toByteArray(double[] array, int off, int len) {
        return toByteArray(array, off, len, new byte[len], 0);
    }
    
    public static byte[] toByteArray(double[] from, int fromOff, int len,
            byte[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (byte) from[i];
        }
        
        return to;
    }
    
    public static char[] toCharArray(byte[] array) {
        return toCharArray(array, 0, array.length);
    }
    
    public static char[] toCharArray(byte[] array, int off, int len) {
        return toCharArray(array, off, len, new char[len], 0);
    }
    
    public static char[] toCharArray(byte[] from, int fromOff, int len,
            char[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (char) from[i];
        }
        
        return to;
    }
    
    public static char[] toCharArray(short[] array) {
        return toCharArray(array, 0, array.length);
    }
    
    public static char[] toCharArray(short[] array, int off, int len) {
        return toCharArray(array, off, len, new char[len], 0);
    }
    
    public static char[] toCharArray(short[] from, int fromOff, int len,
            char[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (char) from[i];
        }
        
        return to;
    }
    
    public static char[] toCharArray(int[] array) {
        return toCharArray(array, 0, array.length);
    }
    
    public static char[] toCharArray(int[] array, int off, int len) {
        return toCharArray(array, off, len, new char[len], 0);
    }
    
    public static char[] toCharArray(int[] from, int fromOff, int len,
            char[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (char) from[i];
        }
        
        return to;
    }
    
    public static char[] toCharArray(long[] array) {
        return toCharArray(array, 0, array.length);
    }
    
    public static char[] toCharArray(long[] array, int off, int len) {
        return toCharArray(array, off, len, new char[len], 0);
    }
    
    public static char[] toCharArray(long[] from, int fromOff, int len,
            char[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (char) from[i];
        }
        
        return to;
    }
    
    public static char[] toCharArray(float[] array) {
        return toCharArray(array, 0, array.length);
    }
    
    public static char[] toCharArray(float[] array, int off, int len) {
        return toCharArray(array, off, len, new char[len], 0);
    }
    
    public static char[] toCharArray(float[] from, int fromOff, int len,
            char[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (char) from[i];
        }
        
        return to;
    }
    
    public static char[] toCharArray(double[] array) {
        return toCharArray(array, 0, array.length);
    }
    
    public static char[] toCharArray(double[] array, int off, int len) {
        return toCharArray(array, off, len, new char[len], 0);
    }
    
    public static char[] toCharArray(double[] from, int fromOff, int len,
            char[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (char) from[i];
        }
        
        return to;
    }
    
    public static short[] toShortArray(byte[] array) {
        return toShortArray(array, 0, array.length);
    }
    
    public static short[] toShortArray(byte[] array, int off, int len) {
        return toShortArray(array, off, len, new short[len], 0);
    }
    
    public static short[] toShortArray(byte[] from, int fromOff, int len,
            short[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static short[] toShortArray(char[] array) {
        return toShortArray(array, 0, array.length);
    }
    
    public static short[] toShortArray(char[] array, int off, int len) {
        return toShortArray(array, off, len, new short[len], 0);
    }
    
    public static short[] toShortArray(char[] from, int fromOff, int len,
            short[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (short) from[i];
        }
        
        return to;
    }
    
    public static short[] toShortArray(int[] array) {
        return toShortArray(array, 0, array.length);
    }
    
    public static short[] toShortArray(int[] array, int off, int len) {
        return toShortArray(array, off, len, new short[len], 0);
    }
    
    public static short[] toShortArray(int[] from, int fromOff, int len,
            short[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (short) from[i];
        }
        
        return to;
    }
    
    public static short[] toShortArray(long[] array) {
        return toShortArray(array, 0, array.length);
    }
    
    public static short[] toShortArray(long[] array, int off, int len) {
        return toShortArray(array, off, len, new short[len], 0);
    }
    
    public static short[] toShortArray(long[] from, int fromOff, int len,
            short[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (short) from[i];
        }
        
        return to;
    }
    
    public static short[] toShortArray(float[] array) {
        return toShortArray(array, 0, array.length);
    }
    
    public static short[] toShortArray(float[] array, int off, int len) {
        return toShortArray(array, off, len, new short[len], 0);
    }
    
    public static short[] toShortArray(float[] from, int fromOff, int len,
            short[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (short) from[i];
        }
        
        return to;
    }
    
    public static short[] toShortArray(double[] array) {
        return toShortArray(array, 0, array.length);
    }
    
    public static short[] toShortArray(double[] array, int off, int len) {
        return toShortArray(array, off, len, new short[len], 0);
    }
    
    public static short[] toShortArray(double[] from, int fromOff, int len,
            short[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (short) from[i];
        }
        
        return to;
    }
    
    public static int[] toIntArray(byte[] array) {
        return toIntArray(array, 0, array.length);
    }
    
    public static int[] toIntArray(byte[] array, int off, int len) {
        return toIntArray(array, off, len, new int[len], 0);
    }
    
    public static int[] toIntArray(byte[] from, int fromOff, int len, int[] to,
            int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static int[] toIntArray(char[] array) {
        return toIntArray(array, 0, array.length);
    }
    
    public static int[] toIntArray(char[] array, int off, int len) {
        return toIntArray(array, off, len, new int[len], 0);
    }
    
    public static int[] toIntArray(char[] from, int fromOff, int len, int[] to,
            int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static int[] toIntArray(short[] array) {
        return toIntArray(array, 0, array.length);
    }
    
    public static int[] toIntArray(short[] array, int off, int len) {
        return toIntArray(array, off, len, new int[len], 0);
    }
    
    public static int[] toIntArray(short[] from, int fromOff, int len,
            int[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static int[] toIntArray(long[] array) {
        return toIntArray(array, 0, array.length);
    }
    
    public static int[] toIntArray(long[] array, int off, int len) {
        return toIntArray(array, off, len, new int[len], 0);
    }
    
    public static int[] toIntArray(long[] from, int fromOff, int len, int[] to,
            int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (int) from[i];
        }
        
        return to;
    }
    
    public static int[] toIntArray(float[] array) {
        return toIntArray(array, 0, array.length);
    }
    
    public static int[] toIntArray(float[] array, int off, int len) {
        return toIntArray(array, off, len, new int[len], 0);
    }
    
    public static int[] toIntArray(float[] from, int fromOff, int len,
            int[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (int) from[i];
        }
        
        return to;
    }
    
    public static int[] toIntArray(double[] array) {
        return toIntArray(array, 0, array.length);
    }
    
    public static int[] toIntArray(double[] array, int off, int len) {
        return toIntArray(array, off, len, new int[len], 0);
    }
    
    public static int[] toIntArray(double[] from, int fromOff, int len,
            int[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (int) from[i];
        }
        
        return to;
    }
    
    public static long[] toLongArray(byte[] array) {
        return toLongArray(array, 0, array.length);
    }
    
    public static long[] toLongArray(byte[] array, int off, int len) {
        return toLongArray(array, off, len, new long[len], 0);
    }
    
    public static long[] toLongArray(byte[] from, int fromOff, int len,
            long[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static long[] toLongArray(char[] array) {
        return toLongArray(array, 0, array.length);
    }
    
    public static long[] toLongArray(char[] array, int off, int len) {
        return toLongArray(array, off, len, new long[len], 0);
    }
    
    public static long[] toLongArray(char[] from, int fromOff, int len,
            long[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static long[] toLongArray(short[] array) {
        return toLongArray(array, 0, array.length);
    }
    
    public static long[] toLongArray(short[] array, int off, int len) {
        return toLongArray(array, off, len, new long[len], 0);
    }
    
    public static long[] toLongArray(short[] from, int fromOff, int len,
            long[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static long[] toLongArray(int[] array) {
        return toLongArray(array, 0, array.length);
    }
    
    public static long[] toLongArray(int[] array, int off, int len) {
        return toLongArray(array, off, len, new long[len], 0);
    }
    
    public static long[] toLongArray(int[] from, int fromOff, int len,
            long[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static long[] toLongArray(float[] array) {
        return toLongArray(array, 0, array.length);
    }
    
    public static long[] toLongArray(float[] array, int off, int len) {
        return toLongArray(array, off, len, new long[len], 0);
    }
    
    public static long[] toLongArray(float[] from, int fromOff, int len,
            long[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (long) from[i];
        }
        
        return to;
    }
    
    public static long[] toLongArray(double[] array) {
        return toLongArray(array, 0, array.length);
    }
    
    public static long[] toLongArray(double[] array, int off, int len) {
        return toLongArray(array, off, len, new long[len], 0);
    }
    
    public static long[] toLongArray(double[] from, int fromOff, int len,
            long[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (long) from[i];
        }
        
        return to;
    }
    
    public static float[] toFloatArray(byte[] array) {
        return toFloatArray(array, 0, array.length);
    }
    
    public static float[] toFloatArray(byte[] array, int off, int len) {
        return toFloatArray(array, off, len, new float[len], 0);
    }
    
    public static float[] toFloatArray(byte[] from, int fromOff, int len,
            float[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static float[] toFloatArray(char[] array) {
        return toFloatArray(array, 0, array.length);
    }
    
    public static float[] toFloatArray(char[] array, int off, int len) {
        return toFloatArray(array, off, len, new float[len], 0);
    }
    
    public static float[] toFloatArray(char[] from, int fromOff, int len,
            float[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static float[] toFloatArray(short[] array) {
        return toFloatArray(array, 0, array.length);
    }
    
    public static float[] toFloatArray(short[] array, int off, int len) {
        return toFloatArray(array, off, len, new float[len], 0);
    }
    
    public static float[] toFloatArray(short[] from, int fromOff, int len,
            float[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static float[] toFloatArray(int[] array) {
        return toFloatArray(array, 0, array.length);
    }
    
    public static float[] toFloatArray(int[] array, int off, int len) {
        return toFloatArray(array, off, len, new float[len], 0);
    }
    
    public static float[] toFloatArray(int[] from, int fromOff, int len,
            float[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static float[] toFloatArray(long[] array) {
        return toFloatArray(array, 0, array.length);
    }
    
    public static float[] toFloatArray(long[] array, int off, int len) {
        return toFloatArray(array, off, len, new float[len], 0);
    }
    
    public static float[] toFloatArray(long[] from, int fromOff, int len,
            float[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static float[] toFloatArray(double[] array) {
        return toFloatArray(array, 0, array.length);
    }
    
    public static float[] toFloatArray(double[] array, int off, int len) {
        return toFloatArray(array, off, len, new float[len], 0);
    }
    
    public static float[] toFloatArray(double[] from, int fromOff, int len,
            float[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = (float) from[i];
        }
        
        return to;
    }
    
    public static double[] toDoubleArray(byte[] array) {
        return toDoubleArray(array, 0, array.length);
    }
    
    public static double[] toDoubleArray(byte[] array, int off, int len) {
        return toDoubleArray(array, off, len, new double[len], 0);
    }
    
    public static double[] toDoubleArray(byte[] from, int fromOff, int len,
            double[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static double[] toDoubleArray(char[] array) {
        return toDoubleArray(array, 0, array.length);
    }
    
    public static double[] toDoubleArray(char[] array, int off, int len) {
        return toDoubleArray(array, off, len, new double[len], 0);
    }
    
    public static double[] toDoubleArray(char[] from, int fromOff, int len,
            double[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static double[] toDoubleArray(short[] array) {
        return toDoubleArray(array, 0, array.length);
    }
    
    public static double[] toDoubleArray(short[] array, int off, int len) {
        return toDoubleArray(array, off, len, new double[len], 0);
    }
    
    public static double[] toDoubleArray(short[] from, int fromOff, int len,
            double[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static double[] toDoubleArray(int[] array) {
        return toDoubleArray(array, 0, array.length);
    }
    
    public static double[] toDoubleArray(int[] array, int off, int len) {
        return toDoubleArray(array, off, len, new double[len], 0);
    }
    
    public static double[] toDoubleArray(int[] from, int fromOff, int len,
            double[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static double[] toDoubleArray(long[] array) {
        return toDoubleArray(array, 0, array.length);
    }
    
    public static double[] toDoubleArray(long[] array, int off, int len) {
        return toDoubleArray(array, off, len, new double[len], 0);
    }
    
    public static double[] toDoubleArray(long[] from, int fromOff, int len,
            double[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    public static double[] toDoubleArray(float[] array) {
        return toDoubleArray(array, 0, array.length);
    }
    
    public static double[] toDoubleArray(float[] array, int off, int len) {
        return toDoubleArray(array, off, len, new double[len], 0);
    }
    
    public static double[] toDoubleArray(float[] from, int fromOff, int len,
            double[] to, int toOff) {
        int end = fromOff + len;
        
        for (int i = fromOff, j = toOff; i < end; i++, j++) {
            to[j] = from[i];
        }
        
        return to;
    }
    
    private ArrayUtil() {}
    
}