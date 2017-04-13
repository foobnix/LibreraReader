/**
 * Copyright (C) 2013
 * Nicholas J. Little <arealityfarbetween@googlemail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mobi;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;


public class StringUtil {

    /**
     * The empty string, for semantic effect
     */
    public final static String EMPTY_STRING = "";

    public static final String[] SEPARATORS = { "[", "=>", "]" };

    private StringUtil() { }

    /**
     * String join for arrays. A call to this function is equivalent
     * to calling <tt>join(sep, 0, array.length, array)</tt>
     *
     * @param sep Separator character to use
     * @param array The array to traverse
     * @return
     */
    public static String join(char sep, Object...array)
    {
        return join(sep, 0, array.length, array);
    }

    /**
     * String join for arrays
     *
     * @param sep Separator character to use
     * @param start Index into array to start from
     * @param end Index into array to end before
     * @param array The array to traverse
     * @return
     */
    public static String join(char sep, int start, int end, Object...array)
    {
        if (start >= end || start < 0 || end >= array.length)
            throw new IllegalArgumentException();

        StringBuilder sb = new StringBuilder();

        for(int i=start; i<end; ++i)
        {
            sb.append(array[i] == null ? EMPTY_STRING : array[i].toString());

            if (i < end - 1)
                sb.append(sep);
        }

        return sb.toString();
    }

    /**
     * Test if a string is null or the empty string (after trimming)
     *
     * @param x
     *            String to test
     * @return boolean
     */
    public static boolean isNullOrWhiteSpace(String x) {
        return x == null || x.trim().isEmpty();
    }

    /**
     * Test if two strings are equal, after trimming both
     *
     * @param x
     * @param y
     * @return
     */
    public static boolean equalsIgnoreWhiteSpace(String x, String y) {
        if (x == null && y == null)
            return true;

        if (x == null || y == null)
            return false;

        return x.trim().equals(y.trim());
    }

    public static final String valueOf (Collection <?> collection)
    {
        StringBuilder sb = openBuilder ();
        boolean first = true;

        for (Object i : collection)
        {
            if (!first)
            {
                sb.append (", ");
            }

            sb.append (String.valueOf (i));

            first = false;
        }

        return closeBuilder (sb);
    }

    public static final String valueOf (Map <?, ?> map)
    {
        StringBuilder sb = openBuilder ();

        boolean first = true;

        for (Map.Entry<?, ?> i : map.entrySet ())
        {
            if (!first)
            {
                sb.append (", ");
            }

            String kv = String.format (
                "%s %s %s",
                valueOf (i.getKey ()),
                SEPARATORS[1],
                valueOf (i.getValue())
            );

            sb.append (kv);

            first = false;
        }

        return closeBuilder (sb);
    }

    public static final <T> String valueOf (T[] array)
    {
        return valueOfArray (array);
    }

    public static final String valueOfArray (Object array)
    {
        StringBuilder sb = openBuilder ();

        int count = Array.getLength (array);

        for (int i = 0; i < count; ++i)
        {
            if (0 < i)
            {
                sb.append (", ");
            }

            sb.append (valueOf (Array.get (array, i)));
        }

        return closeBuilder (sb);
    }

    public static String deepToString (Iterable<?> iterable)
    {
        StringBuilder sb = openBuilder ();

        boolean first = true;

        for (Object i : iterable)
        {
            if (!first) {
                sb.append (", ");
            }

            sb.append (valueOf (i));

            first = false;
        }

        return closeBuilder (sb);
    }

    public static final String valueOf (Object object)
    {
        if (null == object)
        {
            return "null";
        }

        Class<?> clz = object.getClass ();

        if (clz.isArray ())
        {
            return valueOfArray (object);
        }
        else if (Collection.class.isAssignableFrom (clz))
        {
            return valueOf ((Collection<?>) object);
        }
        else if (Map.class.isAssignableFrom (clz))
        {
            return valueOf ((Map<?, ?>) object);
        }
        else if (Iterable.class.isAssignableFrom (clz))
        {
            return deepToString ((Iterable<?>) object);
        }
        else
        {
            return object.toString ();
        }
    }

    private static final StringBuilder openBuilder()
    {
        return new StringBuilder (SEPARATORS[0]);
    }

    private static final String closeBuilder (StringBuilder sb)
    {
        return sb.append (SEPARATORS[2]).toString ();
    }
}
