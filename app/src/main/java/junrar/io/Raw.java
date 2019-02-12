/*
 * Copyright (c) 2007 innoSysTec (R) GmbH, Germany. All rights reserved.
 * Original author: Edmund Wagner
 * Creation date: 18.06.2007
 *
 * Source: $HeadURL$
 * Last changed: $LastChangedDate$
 * 
 * the unrar licence applies to all junrar source and binary distributions 
 * you are not allowed to use this source to re-create the RAR compression algorithm
 * 
 * Here some html entities which can be used for escaping javadoc tags:
 * "&":  "&#038;" or "&amp;"
 * "<":  "&#060;" or "&lt;"
 * ">":  "&#062;" or "&gt;"
 * "@":  "&#064;" 
 */
package junrar.io;

/**
 * Read / write numbers to a byte[] regarding the endianness of the array
 * 
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class Raw {
    /**
     * Read a short value from the byte array at the given position (Big Endian)
     * 
     * @param array
     *            the array to read from
     * @param pos
     *            the position
     * @return the value
     */
    public static final short readShortBigEndian(byte[] array, int pos) {
	short temp = 0;
	temp |= array[pos] & 0xff;
	temp <<= 8;
	temp |= array[pos + 1] & 0xff;
	return temp;
    }

    /**
     * Read a int value from the byte array at the given position (Big Endian)
     * 
     * @param array
     *            the array to read from
     * @param pos
     *            the offset
     * @return the value
     */
    public static final int readIntBigEndian(byte[] array, int pos) {
	int temp = 0;
	temp |= array[pos] & 0xff;
	temp <<= 8;
	temp |= array[pos + 1] & 0xff;
	temp <<= 8;
	temp |= array[pos + 2] & 0xff;
	temp <<= 8;
	temp |= array[pos + 3] & 0xff;
	return temp;
    }

    /**
     * Read a long value from the byte array at the given position (Big Endian)
     * 
     * @param array
     *            the array to read from
     * @param pos
     *            the offset
     * @return the value
     */
    public static final long readLongBigEndian(byte[] array, int pos) {
	int temp = 0;
	temp |= array[pos] & 0xff;
	temp <<= 8;
	temp |= array[pos + 1] & 0xff;
	temp <<= 8;
	temp |= array[pos + 2] & 0xff;
	temp <<= 8;
	temp |= array[pos + 3] & 0xff;
	temp <<= 8;
	temp |= array[pos + 4] & 0xff;
	temp <<= 8;
	temp |= array[pos + 5] & 0xff;
	temp <<= 8;
	temp |= array[pos + 6] & 0xff;
	temp <<= 8;
	temp |= array[pos + 7] & 0xff;
	return temp;
    }

    /**
     * Read a short value from the byte array at the given position (little
     * Endian)
     * 
     * @param array
     *            the array to read from
     * @param pos
     *            the offset
     * @return the value
     */
    public static final short readShortLittleEndian(byte[] array, int pos) {
	short result = 0;
	result += array[pos + 1] & 0xff;
	result <<= 8;
	result += array[pos] & 0xff;
	return result;
    }

    /**
     * Read an int value from the byte array at the given position (little
     * Endian)
     * 
     * @param array
     *            the array to read from
     * @param pos
     *            the offset
     * @return the value
     */
    public static final int readIntLittleEndian(byte[] array, int pos) {
	return ((array[pos + 3] & 0xff) << 24)
		| ((array[pos + 2] & 0xff) << 16)
		| ((array[pos + 1] & 0xff) << 8) | ((array[pos] & 0xff));
    }

    /**
     * Read an long value(unsigned int) from the byte array at the given
     * position (little Endian)
     * 
     * @param array
     * @param pos
     * @return
     */
    public static final long readIntLittleEndianAsLong(byte[] array, int pos) {
	return (((long) array[pos + 3] & 0xff) << 24)
		| (((long) array[pos + 2] & 0xff) << 16)
		| (((long) array[pos + 1] & 0xff) << 8)
		| (((long) array[pos] & 0xff));
    }

    /**
     * Read a long value from the byte array at the given position (little
     * Endian)
     * 
     * @param array
     *            the array to read from
     * @param pos
     *            the offset
     * @return the value
     */
    public static final long readLongLittleEndian(byte[] array, int pos) {
	int temp = 0;
	temp |= array[pos + 7] & 0xff;
	temp <<= 8;
	temp |= array[pos + 6] & 0xff;
	temp <<= 8;
	temp |= array[pos + 5] & 0xff;
	temp <<= 8;
	temp |= array[pos + 4] & 0xff;
	temp <<= 8;
	temp |= array[pos + 3] & 0xff;
	temp <<= 8;
	temp |= array[pos + 2] & 0xff;
	temp <<= 8;
	temp |= array[pos + 1] & 0xff;
	temp <<= 8;
	temp |= array[pos];
	return temp;
    }

    /**
     * Write a short value into the byte array at the given position (Big
     * endian)
     * 
     * @param array
     *            the array
     * @param pos
     *            the offset
     * @param value
     *            the value to write
     */
    public static final void writeShortBigEndian(byte[] array, int pos,
	    short value) {
	array[pos] = (byte) (value >>> 8);
	array[pos + 1] = (byte) (value & 0xFF);

    }

    /**
     * Write an int value into the byte array at the given position (Big endian)
     * 
     * @param array
     *            the array
     * @param pos
     *            the offset
     * @param value
     *            the value to write
     */
    public static final void writeIntBigEndian(byte[] array, int pos, int value) {
	array[pos] = (byte) ((value >>> 24) & 0xff);
	array[pos + 1] = (byte) ((value >>> 16) & 0xff);
	array[pos + 2] = (byte) ((value >>> 8) & 0xff);
	array[pos + 3] = (byte) ((value) & 0xff);

    }

    /**
     * Write a long value into the byte array at the given position (Big endian)
     * 
     * @param array
     *            the array
     * @param pos
     *            the offset
     * @param value
     *            the value to write
     */
    public static final void writeLongBigEndian(byte[] array, int pos,
	    long value) {
	array[pos] = (byte) (value >>> 56);
	array[pos + 1] = (byte) (value >>> 48);
	array[pos + 2] = (byte) (value >>> 40);
	array[pos + 3] = (byte) (value >>> 32);
	array[pos + 4] = (byte) (value >>> 24);
	array[pos + 5] = (byte) (value >>> 16);
	array[pos + 6] = (byte) (value >>> 8);
	array[pos + 7] = (byte) (value & 0xFF);

    }

    /**
     * Write a short value into the byte array at the given position (little
     * endian)
     * 
     * @param array
     *            the array
     * @param pos
     *            the offset
     * @param value
     *            the value to write
     */
    public static final void writeShortLittleEndian(byte[] array, int pos,
	    short value) {
	array[pos + 1] = (byte) (value >>> 8);
	array[pos] = (byte) (value & 0xFF);

    }

    /**
     * Increment a short value at the specified position by the specified amount
     * (little endian).
     */
    public static final void incShortLittleEndian(byte[] array, int pos, int dv) {
	int c = ((array[pos] & 0xff) + (dv & 0xff)) >>> 8;
	array[pos] += dv & 0xff;
	if ((c > 0) || ((dv & 0xff00) != 0)) {
	    array[pos + 1] += ((dv >>> 8) & 0xff) + c;
	}
    }

    /**
     * Write an int value into the byte array at the given position (little
     * endian)
     * 
     * @param array
     *            the array
     * @param pos
     *            the offset
     * @param value
     *            the value to write
     */
    public static final void writeIntLittleEndian(byte[] array, int pos,
	    int value) {
	array[pos + 3] = (byte) (value >>> 24);
	array[pos + 2] = (byte) (value >>> 16);
	array[pos + 1] = (byte) (value >>> 8);
	array[pos] = (byte) (value & 0xFF);

    }

    /**
     * Write a long value into the byte array at the given position (little
     * endian)
     * 
     * @param array
     *            the array
     * @param pos
     *            the offset
     * @param value
     *            the value to write
     */
    public static final void writeLongLittleEndian(byte[] array, int pos,
	    long value) {
	array[pos + 7] = (byte) (value >>> 56);
	array[pos + 6] = (byte) (value >>> 48);
	array[pos + 5] = (byte) (value >>> 40);
	array[pos + 4] = (byte) (value >>> 32);
	array[pos + 3] = (byte) (value >>> 24);
	array[pos + 2] = (byte) (value >>> 16);
	array[pos + 1] = (byte) (value >>> 8);
	array[pos] = (byte) (value & 0xFF);

    }
}
