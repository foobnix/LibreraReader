/**
 * Copyright (C) 2013 Nicholas J. Little <arealityfarbetween@googlemail.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.mobi.little.nj.algorithms;

/**
 * Knuth-Morris-Pratt Algorithm for Pattern Matching
 */
public class KmpSearch {

    /**
     * Generate a next table based on the given pattern
     * 
     * @param pattern
     *            to analyse
     * @return array of shift values
     * 
     * @deprecated Replaced by {@link #calculateNext2(byte[])}
     * 
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static byte[] calculateNext(byte[] pattern) {
        int m = pattern.length;
        byte[] next = new byte[m];
        byte i = 0;
        byte j = -1;
        next[0] = -1;
        while (i + 1 < m) {
            /*
             * This generates sub-standard tables
             */
            while (j >= 0 && pattern[i] != pattern[j])
                j = next[j];
            ++j;
            ++i;
            next[i] = j;
        }
        return next;
    }

    /**
     * Generate a next table based on the given pattern
     * 
     * @param pattern
     *            to analyse
     * @return array of shift values
     */
    private static byte[] calculateNext2(byte[] pattern) {
        int m = pattern.length;
        byte[] next = new byte[m];
        byte i = 1;
        byte j = 0;
        next[0] = -1;
        boolean ok = true;
        while (i < m) {
            /*
             * If these two match, we cannot possibly have a candidate as we
             * already know pattern[i] is a failed match, so shift the match
             * pointer.
             */
            while (j >= 0 && pattern[i] == pattern[j])
                --j;
            /*
             * If all items from 0 to j - 1 match, 'j' is a good candidate for
             * the next table.
             */
            byte k = (byte) (j - 1);
            while (k >= 0 && ok)
                if (pattern[k] != pattern[k + (i - j)])
                    ok = false;
                else
                    --k;
            /*
             * Possible candidate for next table?
             */
            if (!ok) {
                --j;
                ok = true;
            } else {
                /*
                 * Shift match and pattern pointers up next to each other
                 */
                next[i] = j;
                j = i++;
            }
        }
        return next;
    }

    /**
     * <p>
     * Use Knuth-Morris-Pratt algorithm for searching a byte array
     * </p>
     * <p>
     * This call is equal to <tt>indexOf(data, pattern, 0, data.length)</tt>
     * </p>
     * 
     * @param data
     *            Data to search
     * @param pattern
     *            Pattern to match
     * @return position of pattern, or -1
     */
    public static int indexOf(byte[] data, byte[] pattern) {
        return indexOf(data, pattern, 0, data.length);
    }

    /**
     * Use Knuth-Morris-Pratt algorithm for searching a byte array
     * 
     * @param data
     *            Data to search
     * @param pattern
     *            Pattern to match
     * @param start
     *            Start index
     * @param lim
     *            Limit index
     * @return position of pattern, or -1
     */
    public static int indexOf(byte[] data, byte[] pattern, int start, int lim) {
        byte[] next = calculateNext2(pattern);
        int n = lim > data.length ? data.length : lim;
        int m = pattern.length;
        int i = start < 0 ? 0 : start;
        int j = 0;
        while (i < n) {
            while (j >= 0 && data[i] != pattern[j])
                j = next[j];
            ++i;
            ++j;
            if (j == m)
                return i - m;
        }
        return -1;
    }
}