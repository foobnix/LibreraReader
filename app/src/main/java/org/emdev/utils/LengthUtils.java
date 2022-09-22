package org.emdev.utils;

import android.text.TextUtils;

import java.util.Collection;

public final class LengthUtils {
  /**
   * Fake constructor.
   */
  private LengthUtils() {
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
   * Returns default string if the original one is empty.
   *
   * @param original
   *          original string
   * @param defaultValue
   *          default string value
   * @return string
   */
  public static String safeString(final String original, final String defaultValue) {
    return !TextUtils.isEmpty(original) ? original : defaultValue;
  }
}
