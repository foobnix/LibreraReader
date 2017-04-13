/**
 * Copyright (C) 2013 
 * Nicholas J. Little <arealityfarbetween@googlemail.com>
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
package com.mobi.little.nj.adts;

import java.util.LinkedList;


/**
 * <p>
 * A class to store and represent <strong>Binary Coded Decimal
 * Numerals</strong>. The class will delay (re)creating the Byte[] until
 * required at instantiation, after serialisation, and configuration changes.
 * </p>
 * 
 * <p>
 * To force a refresh, run {@link #refresh(boolean...)}.
 * </p>
 * 
 * <h4>History</h4>
 * <dl>
 * <dt>0.1</dt>
 * <dd>Initial Revision
 * <dd>
 * <dt>0.2</dt>
 * <dd>Updated to support left justification. Fixed compression bugs.</dd>
 * <dt>0.3</dt>
 * <dd>Some performance tweaks.</dd>
 * <dt>0.4</dt>
 * <dd>Rewrite Decompression and Array => Long conversion</dd>
 * </dl>
 * 
 * <h4>Todo</h4>
 * <ul>
 * <li>Test incoming Byte[] for Compression</li>
 * </ul>
 * 
 * @author Nicholas Little
 * @version 0.4
 * 
 */
@SuppressWarnings("serial")
public class BCDNumeral extends Number implements Comparable<BCDNumeral> {

    /**
     * Configuration object. Initialised to No Maximum Byte Length Limit, RIGHT
     * justification, Compressed
     * 
     */
    public static class Config {

        /**
         * Default: Compression = false
         */
        private boolean       compressed    = false;

        /*
         * Internal State variable
         */
        private boolean       has_changed   = true;

        /**
         * Default: Justification = Justification.RIGHT
         */
        private Justification justification = Justification.RIGHT;

        /**
         * Default: Length = 0
         */
        private int           length = 0;

        /**
         * @return the justification
         */
        public Justification getJustification() {
            return justification;
        }

        /**
         * @return the max_length
         */
        public int getLength() {
            return length;
        }

        /**
         * @return the has_changed
         */
        public boolean isChanged() {
            return has_changed;
        }

        /**
         * @return the compress
         */
        public boolean isCompress() {
            return compressed;
        }

        /**
         * Set Compression
         * 
         * @param c
         *            Compression state
         * @return This Configuration Object
         */
        public Config setCompress(boolean c) {
            if (compressed != c) {
                has_changed = true;
                compressed = c;
            }
            return this;
        }

        /**
         * Set Justification
         * 
         * @param j
         *            Justification.{RIGHT|LEFT}
         * @return This Configuration Object
         */
        public Config setJustification(Justification j) {
            if (justification != j) {
                has_changed = true;
                justification = j;
            }
            return this;
        }

        /**
         * Set Byte[] Length
         * 
         * @param i
         *            Byte Length
         * @return This Configuration Object
         */
        public Config setLength(int i) {
            if (length != i) {
                has_changed = true;
                length = i < 0 ? 0 : i;
            }
            return this;
        }

        /**
         * Return a human readable string representation of this configuration
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Justification:  " + justification + "\n");
            sb.append("Compression:    " + compressed + "\n");
            sb.append("Length: " + length + "\n");
            return sb.toString();
        }
    }

    /**
     * Justification of the resulting Byte[]. RIGHT pads with 0x00 to the Left,
     * LEFT pads with 0xFF to the Right
     * 
     */
    public static enum Justification {
        LEFT, RIGHT
    }
    
    /**
     * Half an octet
     */
    public static final int SIZE_NIBBLE = 4;

    /**
     * Mask Left 4 Bits
     */
    public static final byte MASK_LEFT  = 0xF;

    /**
     * Mask right 4 Bits
     */
    public static final byte MASK_RIGHT = (byte) 0xF0;

    /**
     * Right Padding Byte
     */
    public static final byte PAD_RIGHT  = (byte) 0xFF;

    /**
     * Zero
     */
    public static final byte ZERO       = 0x0;

    /**
     * Compress Incoming Byte Array
     * 
     * @param in
     *            Uncompressed Byte Array to Compress
     * @return Compressed BCD Byte[]
     */
    public static Byte[] BCDNArrayCompress(Byte[] in) {
        int length = in.length / 2 + in.length % 2;
        Byte[] rtn = new Byte[length];
        
        int i = in.length - 1,
            j = rtn.length - 1,
            lo, hi;
        
        for (; i >= 0; i -= 2, --j) {
            
            hi = (i > 0 ? in[i - 1] << SIZE_NIBBLE : ZERO);
            lo = in[i] & MASK_LEFT;
            
            rtn[j] = (byte) (hi | lo);
        }
        
        return rtn;
    }

    /**
     * Decompress a BCD Byte[]
     * 
     * @param in
     *            Compressed BCD Byte[]
     * @return Uncompressed BCD Byte[]
     */
    public static Byte[] BCDNArrayDecompress(Byte[] in) {
        Byte[] rtn = new Byte[in.length * 2];
        
        // Start from the end of each array
        int i = in.length - 1,
            j = rtn.length - 1;
        
        int lo, hi;
        boolean inPad = true;
        
        for (; i >= 0; --i) {

            // Extract the low and high nibbles
            lo = in[i] & MASK_LEFT;
            hi = in[i] >>> 4;
            
            // Evaluate if the low nibble is padding
            if (inPad) {
                inPad = MASK_LEFT == lo;
            }
            rtn[j--] = inPad ? PAD_RIGHT : (byte) lo;
            
            // Evaluate if the high nibble is padding
            if (inPad) {
                inPad = MASK_LEFT == hi;
            }
            rtn[j--] = inPad ? PAD_RIGHT : (byte) hi;
            
        }
        
        return rtn;
    }

    /**
     * Convert BCD Byte[] to Long Integer
     * 
     * @param in
     *            BCD Byte[] Array
     * @return Long Integer Value
     */
    public static Long BCDNArrayToInteger(Byte[] in, boolean compressed) {
        // Convert a complex problem to a simple one
        if (compressed) {
            in = BCDNArrayDecompress(in);
        }
        
        // Start from the back and skip any padding
        int i = in.length - 1;
        while(PAD_RIGHT == in[i] && i >= 0) --i;
        
        // Collect powers for each digit
        int pow = 0;
        long tmp = 0L;
        byte current;
        for (; i >= 0; --i) {
            current = in[i];
            tmp += current * Math.pow(10, pow);
            ++pow;
        }
        
        return tmp;
    }

    /**
     * Convert a primitive byte[] to a Byte[]
     * 
     * @param in
     *            byte[] to convert
     * @return Byte[] copy
     */
    public static Byte[] ConvertPrimitiveArray(byte[] in) {
        
        Byte[] rtn = new Byte[in.length];
        for (int i = 0; i < rtn.length; i++)
            rtn[i] = in[i];
        
        return rtn;
    }

    /**
     * Process a string into a BCDNumeral
     * 
     * @param s
     *            String to parse
     * @param compressed
     *            String is a compressed representation
     * @param j
     *            Justification to use when formatting the array
     * @return BCDNumeral
     */

    /**
     * Process a string into a BCDNumeral
     * 
     * @param s
     *            String to parse
     * @param conf
     *            Configuration to use
     * @return BCDNumeral
     */

    /**
     * Convert a Long Integer to a BCD Byte[]
     * 
     * @param i
     *            Long value to convert
     * @param compress
     *            Compress 2 digits into one byte
     * @param j
     *            {@link Justification}
     * @param length
     *            Length of resulting Byte[], 0 == No Restriction
     * @return Byte[] Representation
     */
    public static Byte[] IntegerToBCDNArray(long i, boolean compress,
            Justification j, int length) {
        
        Config conf = new Config()
            .setCompress(compress)
            .setJustification(j)
            .setLength(length);
        
        return IntegerToBCDNArray(i, conf);
    }

    /**
     * Construct a BCDN Byte[]
     * 
     * @param i
     *            Long value to convert
     * @param config
     *            {@link Config}
     * @return Byte[] Representation
     */
    public static Byte[] IntegerToBCDNArray(long i, Config config) {
        if (i < 0)
            throw new IllegalArgumentException("negative");
        
        /*
         * Record length parameter for use and expand if
         * we'll be compressing
         */
        int length = config.compressed ? config.length * 2 
                                       : config.length;
        boolean hasLength = length > 0;
        
        /*
         * Take a List<Byte>
         */
        long value = i;
        LinkedList<Byte> digits = new LinkedList<Byte>();
        do {
            digits.addFirst((byte)(value % 10));
            value /= 10;
        } while(value > 0);
        
        if (hasLength) {
            /*
             * Truncate to length
             */
            while (digits.size() > length) {
                digits.removeFirst();
            }
            
            /*
             * Justify up to minimum length
             */
            switch (config.justification) {
            case RIGHT:
                while (digits.size() < length)
                    digits.addFirst(ZERO);
                break;
            case LEFT:
                while (digits.size() < length)
                    digits.addLast(PAD_RIGHT);
                break;
            }
        }
        
        /*
         * Transfer to Byte[]
         */
        Byte[] result = digits.toArray(new Byte[digits.size()]);
        
        /*
         * Compress if required
         */
        if (config.compressed) {
            result = BCDNArrayCompress(result);
        }
        
        return result;
    }

    /**
     * Byte[] Representation
     */
    private transient Byte[] binary_coded;

    /**
     * Configuration & State Tracking
     */
    private final Config config;

    /**
     * Long Integer Value
     */
    private final Long value;

    /**
     * Construct a BCD from the input array and configuration
     * 
     * @param in
     *            Initial Byte[] Input
     * @param config
     *            {@link Config}
     */
    public BCDNumeral(Byte[] in, Config config) {
        this.config = config;
        binary_coded = in.clone();
        value = BCDNArrayToInteger(in, config.compressed);
        config.setLength(in.length);
        config.has_changed = false;
    }

    /**
     * Instantiate a BCD Numeral with default configuration
     * 
     * @param value
     *            Long value to encode
     */
    public BCDNumeral(long value) {
        this(value, new Config());
    }

    /**
     * Instantiate a BCD Numeral
     * 
     * @param value
     *            Long value to encode
     * @param config
     *            {@link Config}
     */
    public BCDNumeral(long value, Config config) {
        if (value < 0)
            throw new IllegalArgumentException("negative");
        
        this.value = value;
        this.config = config;
    }

    /**
     * Get {@link Config} for this BCD
     * 
     * @return {@link Config} Object
     */
    public Config getConfig() { return config; }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(BCDNumeral that) {
        return value.compareTo(that.value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Number#doubleValue()
     */
    @Override
    public double doubleValue() {
        return value.doubleValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Number#floatValue()
     */
    @Override
    public float floatValue() {
        return value.floatValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Number#intValue()
     */
    @Override
    public int intValue() {
        return value.intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Number#longValue()
     */
    @Override
    public long longValue() {
        return value.longValue();
    }

    /**
     * Check and Refresh Array
     * 
     * @param force
     *            Optional parameter to force refresh
     */
    public void refresh(boolean force) {
        boolean refresh = config.has_changed || null == binary_coded;
        
        if (refresh || force) {
            binary_coded = IntegerToBCDNArray(value, config);
            config.has_changed = false;
        }
    }
    
    /**
     * @see {@link BCDNumeral#refresh(boolean)}
     */
    public void refresh() { refresh(false); }

    /**
     * Copy to Byte[]
     * 
     * @return Copy of internal Byte[]
     */
    public Byte[] toArray() {
        refresh();
        
        return binary_coded.clone();
    }

    /**
     * Return a hex string representation of the internal Byte[]
     * 
     * @return Hex String
     */

    /**
     * Copy to primitive byte[]
     * 
     * @return Copy of internal Byte[], as byte[]
     */
    public byte[] toPrimitiveArray() {
        refresh();
        
        byte[] rtn = new byte[binary_coded.length];
        for (int i = 0; i < rtn.length; i++) {
            rtn[i] = binary_coded[i];
        }
        
        return rtn;
    }
    
    @Override
    public String toString() { return value.toString(); }
}
