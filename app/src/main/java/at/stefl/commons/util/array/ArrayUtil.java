package at.stefl.commons.util.array;

import com.google.common.primitives.Chars;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import at.stefl.commons.util.collection.CollectionUtil;

// TODO: improve attribute names
// TODO: implement array methods with offset and length
// TODO: avoid redundant code?
public class ArrayUtil {
    public static final byte[] EMPTY_BYTE_ARRAY = {};
    public static final char[] EMPTY_CHAR_ARRAY = {};
    private static final char[] HEX_ARRAY = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static boolean validateArguments(int size, int off, int len) {
    	if (off < 0) return false;
    	if (len < 0) return false;
        return (off + len) <= size;
    }

    public static <T extends Collection<? super E>, E> T toCollection(T c, E... array) {
        CollectionUtil.addAll(c, array);
        return c;
    }

    @SafeVarargs
    public static <E> Set<E> toSet(E... array) {
        return new HashSet<>(Arrays.asList(array));
    }

    public static Set<Character> toSet(char... array) {
        return new HashSet<>(Chars.asList(array));
    }

    public static void swap(byte[] array, int i, int j) {
        byte tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
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

    private ArrayUtil() {}
}
