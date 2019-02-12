package org.emdev.utils;

import java.util.Collection;
import java.util.Map;

import org.json.JSONArray;

public final class LengthUtils {

  /** Empty string singleton. */
  private static final String SAFE_STRING = "";

  /**
   * Fake constructor.
   */
  private LengthUtils() {
  }

  /**
   * Checks if the given string is empty.
   *
   * @param s
   *          string
   * @return <code>true</code> if the given reference is <code>null</code> or string is empty
   */
  public static boolean isEmpty(final String s) {
    return length(s) == 0;
  }

  /**
   * Checks if any of given strings are empty.
   *
   * @param strings
   *          strings to test
   * @return <code>true</code> if any of given strings are <code>null</code> or empty
   */
  public static boolean isAnyEmpty(final String... strings) {
    for (final String s : strings) {
      if (length(s) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if all of given strings are empty.
   *
   * @param strings
   *          strings to test
   * @return <code>true</code> if all of given strings are <code>null</code> or empty
   */
  public static boolean isAllEmpty(final String... strings) {
    for (final String s : strings) {
      if (length(s) != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if all of given strings are not empty.
   *
   * @param strings
   *          strings to test
   * @return <code>true</code> if all of given strings are not <code>null</code> and not empty
   */
  public static boolean isAllNotEmpty(final String... strings) {
    for (final String s : strings) {
      if (length(s) == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks if the given string is not empty.
   *
   * @param s
   *          string
   * @return <code>true</code> if the given reference is not <code>null</code> and string is not empty
   */
  public static boolean isNotEmpty(final String s) {
    return length(s) > 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final Object[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final JSONArray array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and string is not empty
   */
  public static boolean isNotEmpty(final boolean[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final boolean[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final byte[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final short[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final int[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final long[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final float[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given array is empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is <code>null</code> or array is empty
   */
  public static boolean isEmpty(final double[] array) {
    return length(array) == 0;
  }

  /**
   * Checks if the given collection is empty.
   *
   * @param c
   *          collection to check
   * @return <code>true</code> if the given reference is <code>null</code> or collection is empty
   */
  public static boolean isEmpty(final Collection<?> c) {
    return c == null || c.isEmpty();
  }

  /**
   * Checks if the given map is empty.
   *
   * @param map
   *          map to check
   * @return <code>true</code> if the given reference is <code>null</code> or map is empty
   */
  public static boolean isEmpty(final Map<?, ?> map) {
    return map == null || map.isEmpty();
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final byte[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final short[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final int[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final long[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final float[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final double[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final Object[] array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given array is not empty.
   *
   * @param array
   *          array to check
   * @return <code>true</code> if the given reference is not <code>null</code> and array is not empty
   */
  public static boolean isNotEmpty(final JSONArray array) {
    return length(array) > 0;
  }

  /**
   * Checks if the given collection is not empty.
   *
   * @param c
   *          collection to check
   * @return <code>true</code> if the given reference is not <code>null</code> and collection is not empty
   */
  public static boolean isNotEmpty(final Collection<?> c) {
    return c != null && !c.isEmpty();
  }

  /**
   * Checks if the given map is not empty.
   *
   * @param map
   *          map to check
   * @return <code>true</code> if the given reference is not <code>null</code> and map is not empty
   */
  public static boolean isNotEmpty(final Map<?, ?> map) {
    return map != null && !map.isEmpty();
  }

  /**
   * Safely calculates a string length.
   *
   * @param s
   *          string
   * @return real string length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final String s) {
    return s != null ? s.length() : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final Object[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final JSONArray arr) {
    return arr != null ? arr.length() : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final boolean[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates a collection length.
   *
   * @param c
   *          collection to check
   * @return real collection length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final Collection<?> c) {
    return c != null ? c.size() : 0;
  }

  /**
   * Safely calculates a map length.
   *
   * @param map
   *          map to check
   * @return real map length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final Map<?, ?> map) {
    return map != null ? map.size() : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final byte[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final short[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final int[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final long[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final float[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Safely calculates an array length.
   *
   * @param arr
   *          array
   * @return real array length or <code>0</code> if reference is <code>null</code>
   */
  public static int length(final double[] arr) {
    return arr != null ? arr.length : 0;
  }

  /**
   * Returns empty string if the original one is null.
   *
   * @param original
   *          original string
   * @return string
   */
  public static String unsafeString(final String original) {
    return length(original) == 0 ? null : original;
  }

  /**
   * Returns empty string if the original one is null.
   *
   * @param original
   *          original string
   * @return string
   */
  public static String safeString(final String original) {
    return safeString(original, safeString());
  }

  /**
   * Returns default string if the original one is empty.
   *
   * @param original
   *          original string
   * @param defaultValue
   *          default string value
   * @return string
   */
  public static String safeString(final String original, final String defaultValue) {
    return isNotEmpty(original) ? original : defaultValue;
  }

  /**
   * Returns empty safe string.
   *
   * @return a safe empty string
   */
  public static String safeString() {
    return SAFE_STRING;
  }

  /**
   * Converts objects to array.
   *
   * @param <T>
   *          type of objects in the array
   * @param objects
   *          objects to converts
   * @return array
   */
  public static <T> T[] toArray(final T... objects) {
    return objects;
  }

  /**
   * Converts object to string.
   *
   * @param obj
   *          object
   * @return string
   */
  public static String toString(final Object obj) {
    return obj != null ? obj.toString() : safeString();
  }

  /**
   * Returns a hash code value for the object.
   *
   * @param obj
   *          the obj
   * @return the int
   */
  public static int hashCode(final Object obj) {
    return obj != null ? obj.hashCode() : 0;
  }
}
