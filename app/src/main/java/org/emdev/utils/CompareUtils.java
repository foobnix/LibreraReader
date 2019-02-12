package org.emdev.utils;

public final class CompareUtils {

  /**
   * Fake constructor.
   */
  private CompareUtils() {
  }

  /**
   * Compare two boolean values.
   *
   * @param val1
   *          first value
   * @param val2
   *          second value
   * @return on of the following values:
   *         <ul>
   *         <li><code>-1</code> if the first value is <code>false</code> and the second one is <code>true</code></li>
   *         <li><code>0</code> if both values are equal</li>
   *         <li><code>1</code> if the first value is <code>true</code> and the second one is <code>false</code></li>
   *         </ul>
   */
  public static int compare(final boolean val1, final boolean val2) {
    return compare(val1 ? 1 : 0, val2 ? 1 : 0);
  }

  /**
   * Compare two integer values.
   *
   * @param val1
   *          first value
   * @param val2
   *          second value
   * @return on of the following values:
   *         <ul>
   *         <li><code>-1</code> if the first value is less than the second one</li>
   *         <li><code>0</code> if both values are equal</li>
   *         <li><code>1</code> if the first value is greater than the second one</li>
   *         </ul>
   */
  public static int compare(final int val1, final int val2) {
    return val1 < val2 ? -1 : val1 > val2 ? 1 : 0;
  }

  /**
   * Compare two long values.
   *
   * @param val1
   *          first value
   * @param val2
   *          second value
   * @return on of the following values:
   *         <ul>
   *         <li><code>-1</code> if the first value is less than the second one</li>
   *         <li><code>0</code> if both values are equal</li>
   *         <li><code>1</code> if the first value is greater than the second one</li>
   *         </ul>
   */
  public static int compare(final long val1, final long val2) {
    return val1 < val2 ? -1 : val1 > val2 ? 1 : 0;
  }

  /**
   * Compare two float values.
   *
   * @param val1
   *          first value
   * @param val2
   *          second value
   * @return on of the following values:
   *         <ul>
   *         <li><code>-1</code> if the first value is less than the second one</li>
   *         <li><code>0</code> if both values are equal</li>
   *         <li><code>1</code> if the first value is greater than the second one</li>
   *         </ul>
   */
  public static int compare(final float val1, final float val2) {
    return val1 < val2 ? -1 : val1 > val2 ? 1 : 0;
  }

  /**
   * Compares two object.
   *
   * @param t1
   *          first object
   * @param t2
   *          second object
   * @return on of the following values:
   *         <ul>
   *         <li><code>-1</code> if the first value is less than the second one</li>
   *         <li><code>0</code> if both values are equal</li>
   *         <li><code>1</code> if the first value is greater than the second one</li>
   *         </ul>
   */
  public static <T extends Comparable<T>> int compare(final T t1, final T t2) {
    if (t1 == null) {
      return t2 == null ? 0 : -1;
    }
    if (t2 == null) {
        return 1;
    }
    return t1.compareTo(t2);
  }

/**
   * Compares two objects.
   *
   * @param o1
   *          first object
   * @param o2
   *          second object
   * @return if objects are equal or both are null
   */
  public static boolean equals(final Object o1, final Object o2) {
    if (o1 == null) {
      return o2 == null ? true : false;
    }

    return o1.equals(o2);
  }

  /**
   * Compares two strings.
   *
   * @param s1
   *          first string
   * @param s2
   *          second string
   * @return if strings are equal or both are null
   */
  public static boolean equals(final String s1, final String s2) {
    if (LengthUtils.isEmpty(s1)) {
      return LengthUtils.isEmpty(s2) ? true : false;
    }

    return s1.equals(s2);
  }

  /**
   * Compares two arrays of strings.
   *
   * @param s1
   *          first string array
   * @param s2
   *          second string array
   * @return if all strings are equal or both are null
   */
  public static boolean equals(final String[] s1, final String[] s2) {
    final int length1 = LengthUtils.length(s1);
    final int length2 = LengthUtils.length(s2);

    if (length1 != length2) {
      return false;
    }

    for (int i = 0; i < length1; i++) {
      if (!equals(s1[i], s2[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compares two strings ignoring char cases.
   *
   * @param s1
   *          first string
   * @param s2
   *          second string
   * @return if strings are equal or both are null
   */
  public static boolean equalsIgnoreCase(final String s1, final String s2) {
    if (LengthUtils.isEmpty(s1)) {
      return LengthUtils.isEmpty(s2) ? true : false;
    }

    return s1.equalsIgnoreCase(s2);
  }

  /**
   * Compares two arrays of strings ignoring char cases.
   *
   * @param s1
   *          first string array
   * @param s2
   *          second string array
   * @return if all strings are equal or both are null
   */
  public static boolean equalsIgnoreCase(final String[] s1, final String[] s2) {
    final int length1 = LengthUtils.length(s1);
    final int length2 = LengthUtils.length(s2);

    if (length1 != length2) {
      return false;
    }

    for (int i = 0; i < length1; i++) {
      if (!equalsIgnoreCase(s1[i], s2[i])) {
        return false;
      }
    }
    return true;
  }

}
