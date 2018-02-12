/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.commons.compress.archivers.zip;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Utility class for handling DOS and Java time conversions.
 * @Immutable
 */
public abstract class ZipUtil {
    /**
     * Smallest date/time ZIP can handle.
     */
    private static final byte[] DOS_TIME_MIN = ZipLong.getBytes(0x00002100L);

    /**
     * Convert a Date object to a DOS date/time field.
     * @param time the <code>Date</code> to convert
     * @return the date as a <code>ZipLong</code>
     */
    public static ZipLong toDosTime(final Date time) {
        return new ZipLong(toDosTime(time.getTime()));
    }

    /**
     * Convert a Date object to a DOS date/time field.
     *
     * <p>Stolen from InfoZip's <code>fileio.c</code></p>
     * @param t number of milliseconds since the epoch
     * @return the date as a byte array
     */
    public static byte[] toDosTime(final long t) {
        final byte[] result = new byte[4];
        toDosTime(t, result, 0);
        return result;
    }

    /**
     * Convert a Date object to a DOS date/time field.
     *
     * <p>Stolen from InfoZip's <code>fileio.c</code></p>
     * @param t number of milliseconds since the epoch
     * @param buf the output buffer
     * @param offset
     *         The offset within the output buffer of the first byte to be written.
     *         must be non-negative and no larger than <tt>buf.length-4</tt>
     */
    public static void toDosTime(final long t, final byte[] buf, final int offset) {
        toDosTime(Calendar.getInstance(), t, buf, offset);
    }

    static void toDosTime(final Calendar c, final long t, final byte[] buf, final int offset) {
        c.setTimeInMillis(t);

        final int year = c.get(Calendar.YEAR);
        if (year < 1980) {
            System.arraycopy(DOS_TIME_MIN, 0, buf, offset, DOS_TIME_MIN.length);// stop callers from changing the array
            return;
        }
        final int month = c.get(Calendar.MONTH) + 1;
        final long value =  ((year - 1980) << 25)
                |         (month << 21)
                |         (c.get(Calendar.DAY_OF_MONTH) << 16)
                |         (c.get(Calendar.HOUR_OF_DAY) << 11)
                |         (c.get(Calendar.MINUTE) << 5)
                |         (c.get(Calendar.SECOND) >> 1);
        ZipLong.putLong(value, buf, offset);
    }


    /**
     * Assumes a negative integer really is a positive integer that
     * has wrapped around and re-creates the original value.
     *
     * @param i the value to treat as unsigned int.
     * @return the unsigned int as a long.
     */
    public static long adjustToLong(final int i) {
        if (i < 0) {
            return 2 * ((long) Integer.MAX_VALUE) + 2 + i;
        }
        return i;
    }

    /**
     * Reverses a byte[] array.  Reverses in-place (thus provided array is
     * mutated), but also returns same for convenience.
     *
     * @param array to reverse (mutated in-place, but also returned for
     *        convenience).
     *
     * @return the reversed array (mutated in-place, but also returned for
     *        convenience).
     * @since 1.5
     */
    public static byte[] reverse(final byte[] array) {
        final int z = array.length - 1; // position of last element
        for (int i = 0; i < array.length / 2; i++) {
            final byte x = array[i];
            array[i] = array[z - i];
            array[z - i] = x;
        }
        return array;
    }

    /**
     * Converts a BigInteger into a long, and blows up
     * (NumberFormatException) if the BigInteger is too big.
     *
     * @param big BigInteger to convert.
     * @return long representation of the BigInteger.
     */
    static long bigToLong(final BigInteger big) {
        if (big.bitLength() <= 63) { // bitLength() doesn't count the sign bit.
            return big.longValue();
        }
        throw new NumberFormatException("The BigInteger cannot fit inside a 64 bit java long: [" + big + "]");
    }

    /**
     * <p>
     * Converts a long into a BigInteger.  Negative numbers between -1 and
     * -2^31 are treated as unsigned 32 bit (e.g., positive) integers.
     * Negative numbers below -2^31 cause an IllegalArgumentException
     * to be thrown.
     * </p>
     *
     * @param l long to convert to BigInteger.
     * @return BigInteger representation of the provided long.
     */
    static BigInteger longToBig(long l) {
        if (l < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Negative longs < -2^31 not permitted: [" + l + "]");
        } else if (l < 0 && l >= Integer.MIN_VALUE) {
            // If someone passes in a -2, they probably mean 4294967294
            // (For example, Unix UID/GID's are 32 bit unsigned.)
            l = ZipUtil.adjustToLong((int) l);
        }
        return BigInteger.valueOf(l);
    }

    /**
     * Converts a signed byte into an unsigned integer representation
     * (e.g., -1 becomes 255).
     *
     * @param b byte to convert to int
     * @return int representation of the provided byte
     * @since 1.5
     */
    public static int signedByteToUnsignedInt(final byte b) {
        if (b >= 0) {
            return b;
        }
        return 256 + b;
    }

    /**
     * Converts an unsigned integer to a signed byte (e.g., 255 becomes -1).
     *
     * @param i integer to convert to byte
     * @return byte representation of the provided int
     * @throws IllegalArgumentException if the provided integer is not inside the range [0,255].
     * @since 1.5
     */
    public static byte unsignedIntToSignedByte(final int i) {
        if (i > 255 || i < 0) {
            throw new IllegalArgumentException("Can only convert non-negative integers between [0,255] to byte: [" + i + "]");
        }
        if (i < 128) {
            return (byte) i;
        }
        return (byte) (i - 256);
    }

    /**
     * Convert a DOS date/time field to a Date object.
     *
     * @param zipDosTime contains the stored DOS time.
     * @return a Date instance corresponding to the given time.
     */
    public static Date fromDosTime(final ZipLong zipDosTime) {
        final long dosTime = zipDosTime.getValue();
        return new Date(dosToJavaTime(dosTime));
    }

    /**
     * Converts DOS time to Java time (number of milliseconds since
     * epoch).
     * @param dosTime time to convert
     * @return converted time
     */
    public static long dosToJavaTime(final long dosTime) {
        final Calendar cal = Calendar.getInstance();
        // CheckStyle:MagicNumberCheck OFF - no point
        cal.set(Calendar.YEAR, (int) ((dosTime >> 25) & 0x7f) + 1980);
        cal.set(Calendar.MONTH, (int) ((dosTime >> 21) & 0x0f) - 1);
        cal.set(Calendar.DATE, (int) (dosTime >> 16) & 0x1f);
        cal.set(Calendar.HOUR_OF_DAY, (int) (dosTime >> 11) & 0x1f);
        cal.set(Calendar.MINUTE, (int) (dosTime >> 5) & 0x3f);
        cal.set(Calendar.SECOND, (int) (dosTime << 1) & 0x3e);
        cal.set(Calendar.MILLISECOND, 0);
        // CheckStyle:MagicNumberCheck ON
        return cal.getTime().getTime();
    }

    /**
     * If the entry has Unicode*ExtraFields and the CRCs of the
     * names/comments match those of the extra fields, transfer the
     * known Unicode values from the extra field.
     */
    static void setNameAndCommentFromExtraFields(final ZipArchiveEntry ze,
                                                 final byte[] originalNameBytes,
                                                 final byte[] commentBytes) {
        final UnicodePathExtraField name = (UnicodePathExtraField)
            ze.getExtraField(UnicodePathExtraField.UPATH_ID);
        final String newName = getUnicodeStringIfOriginalMatches(name,
                                                           originalNameBytes);
        if (newName != null) {
            ze.setName(newName);
            ze.setNameSource(ZipArchiveEntry.NameSource.UNICODE_EXTRA_FIELD);
        }

        if (commentBytes != null && commentBytes.length > 0) {
            final UnicodeCommentExtraField cmt = (UnicodeCommentExtraField)
                ze.getExtraField(UnicodeCommentExtraField.UCOM_ID);
            final String newComment =
                getUnicodeStringIfOriginalMatches(cmt, commentBytes);
            if (newComment != null) {
                ze.setComment(newComment);
                ze.setCommentSource(ZipArchiveEntry.CommentSource.UNICODE_EXTRA_FIELD);
            }
        }
    }

    /**
     * If the stored CRC matches the one of the given name, return the
     * Unicode name of the given field.
     *
     * <p>If the field is null or the CRCs don't match, return null
     * instead.</p>
     */
    private static
        String getUnicodeStringIfOriginalMatches(final AbstractUnicodeExtraField f,
                                                 final byte[] orig) {
        if (f != null) {
            final CRC32 crc32 = new CRC32();
            crc32.update(orig);
            final long origCRC32 = crc32.getValue();

            if (origCRC32 == f.getNameCRC32()) {
                try {
                    return ZipEncodingHelper
                        .UTF8_ZIP_ENCODING.decode(f.getUnicodeName());
                } catch (final IOException ex) {
                    // UTF-8 unsupported?  should be impossible the
                    // Unicode*ExtraField must contain some bad bytes

                    // TODO log this anywhere?
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Create a copy of the given array - or return null if the
     * argument is null.
     */
    static byte[] copy(final byte[] from) {
        if (from != null) {
            final byte[] to = new byte[from.length];
            System.arraycopy(from, 0, to, 0, to.length);
            return to;
        }
        return null;
    }

    static void copy(final byte[] from, final byte[] to, final int offset) {
        if (from != null) {
            System.arraycopy(from, 0, to, offset, from.length);
        }
    }


    /**
     * Whether this library is able to read or write the given entry.
     */
    static boolean canHandleEntryData(final ZipArchiveEntry entry) {
        return supportsEncryptionOf(entry) && supportsMethodOf(entry);
    }

    /**
     * Whether this library supports the encryption used by the given
     * entry.
     *
     * @return true if the entry isn't encrypted at all
     */
    private static boolean supportsEncryptionOf(final ZipArchiveEntry entry) {
        return !entry.getGeneralPurposeBit().usesEncryption();
    }

    /**
     * Whether this library supports the compression method used by
     * the given entry.
     *
     * @return true if the compression method is supported
     */
    private static boolean supportsMethodOf(final ZipArchiveEntry entry) {
        return entry.getMethod() == ZipEntry.STORED
            || entry.getMethod() == ZipMethod.UNSHRINKING.getCode()
            || entry.getMethod() == ZipMethod.IMPLODING.getCode()
            || entry.getMethod() == ZipEntry.DEFLATED
            || entry.getMethod() == ZipMethod.ENHANCED_DEFLATED.getCode()
            || entry.getMethod() == ZipMethod.BZIP2.getCode();
    }

    /**
     * Checks whether the entry requires features not (yet) supported
     * by the library and throws an exception if it does.
     */
    static void checkRequestedFeatures(final ZipArchiveEntry ze)
        throws UnsupportedZipFeatureException {
        if (!supportsEncryptionOf(ze)) {
            throw
                new UnsupportedZipFeatureException(UnsupportedZipFeatureException
                                                   .Feature.ENCRYPTION, ze);
        }
        if (!supportsMethodOf(ze)) {
            final ZipMethod m = ZipMethod.getMethodByCode(ze.getMethod());
            if (m == null) {
                throw
                    new UnsupportedZipFeatureException(UnsupportedZipFeatureException
                                                       .Feature.METHOD, ze);
            }
            throw new UnsupportedZipFeatureException(m, ze);
        }
    }
}
