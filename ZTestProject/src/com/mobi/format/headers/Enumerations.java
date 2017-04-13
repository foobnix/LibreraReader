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
package com.mobi.format.headers;

import java.nio.charset.Charset;

public final class Enumerations {

    protected final static String FMT_NOT_RECOGNISED = "Type '%d' not recognised";

    /**
     * @see {@link PalmDocHeader}
     */
    public static enum Compression {
        HUFF_CDIC(17480), NONE(1), PALMDOC(2), UNKNOWN(15);

        public static final Compression valueOf(int x) {
            short y = (short) x;
            for (Compression i : values())
                if (i.value == x)
                    return i;
            UNKNOWN.value = y;
            return UNKNOWN;
        }

        private short value;

        private Compression(int x) {
            value = (short) x;
        }

        public byte[] getBytes() {
            return new byte[] { (byte) ((value & 0xFF00) >>> 8),
                    (byte) (value & 0xFF) };
        }

        public short getValue() {
            return value;
        }
    }

    /**
     * @see {@link MobiDocHeader}
     */
    public static enum Encoding {
        CP1252(1252) {

            @Override
            public Charset getCharset() {
                return Charset.forName("windows-1252");
            }
        },
        UTF8(65001) {

            @Override
            public Charset getCharset() {
                return Charset.forName("UTF8");
            }
        };

        public static final Encoding valueOf(int x) {
            for (Encoding i : values())
                if (i.value == x)
                    return i;
            throw new IllegalArgumentException(String.format(
                    Enumerations.FMT_NOT_RECOGNISED,
                    new Object[] { Integer.valueOf(x) }));
        }

        private final int value;

        private Encoding(int x) {
            value = x;
        }

        public abstract Charset getCharset();

        public int getValue() {
            return value;
        }
    }

    /**
     * @see {@link MobiDocHeader}
     * 
     */
    public static enum Locale {
        UK_ENGLISH(2057), UNSPECIFIED(0), US_ENGLISH(1033);

        public static final Locale valueOf(int x) {
            for (Locale i : values())
                if (i.value == x)
                    return i;
            return UNSPECIFIED;
        }

        private int value;

        private Locale(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name() + " [ " + Integer.toHexString(value) + " ]";
        }
    }

    /**
     * @see {@link MobiDocHeader}
     */
    public static enum MobiType {
        AUDIO(4), KF8_KINDLEGEN2(248), MOBI_BOOK(2), MOBI_KINDLEGEN1_2(232), NEWS(
                257), NEWS_FEED(258), NEWS_MAGAZINE(259), PALM_BOOK(3), PICS(
                513), PPT(516), TEXT(517), WORD(514), XLS(515), HTML(518);

        public static final MobiType valueOf(int x) {
            for (MobiType i : values())
                if (i.value == x)
                    return i;
            throw new IllegalArgumentException(String.format(
                    Enumerations.FMT_NOT_RECOGNISED,
                    new Object[] { Integer.valueOf(x) }));
        }

        private final int value;

        private MobiType(int x) {
            value = x;
        }

        public int getValue() {
            return value;
        }
    }

}
