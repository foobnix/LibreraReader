/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.zip;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.zip.ZipException;

import static org.apache.commons.compress.archivers.zip.ZipUtil.reverse;
import static org.apache.commons.compress.archivers.zip.ZipUtil.signedByteToUnsignedInt;
import static org.apache.commons.compress.archivers.zip.ZipUtil.unsignedIntToSignedByte;

/**
 * An extra field that stores UNIX UID/GID data (owner &amp; group ownership) for a given
 * zip entry.  We're using the field definition given in Info-Zip's source archive:
 * zip-3.0.tar.gz/proginfo/extrafld.txt
 *
 * <pre>
 * Local-header version:
 *
 * Value         Size        Description
 * -----         ----        -----------
 * 0x7875        Short       tag for this extra block type ("ux")
 * TSize         Short       total data size for this block
 * Version       1 byte      version of this extra field, currently 1
 * UIDSize       1 byte      Size of UID field
 * UID           Variable    UID for this entry (little endian)
 * GIDSize       1 byte      Size of GID field
 * GID           Variable    GID for this entry (little endian)
 *
 * Central-header version:
 *
 * Value         Size        Description
 * -----         ----        -----------
 * 0x7855        Short       tag for this extra block type ("Ux")
 * TSize         Short       total data size for this block (0)
 * </pre>
 * @since 1.5
 */
public class X7875_NewUnix implements ZipExtraField, Cloneable, Serializable {
    private static final ZipShort HEADER_ID = new ZipShort(0x7875);
    private static final ZipShort ZERO = new ZipShort(0);
    private static final BigInteger ONE_THOUSAND = BigInteger.valueOf(1000);
    private static final long serialVersionUID = 1L;

    private int version = 1; // always '1' according to current info-zip spec.

    // BigInteger helps us with little-endian / big-endian conversions.
    // (thanks to BigInteger.toByteArray() and a reverse() method we created).
    // Also, the spec theoretically allows UID/GID up to 255 bytes long!
    //
    // NOTE:  equals() and hashCode() currently assume these can never be null.
    private BigInteger uid;
    private BigInteger gid;

    /**
     * Constructor for X7875_NewUnix.
     */
    public X7875_NewUnix() {
        reset();
    }

    /**
     * The Header-ID.
     *
     * @return the value for the header id for this extrafield
     */
    @Override
    public ZipShort getHeaderId() {
        return HEADER_ID;
    }

    /**
     * Gets the UID as a long.  UID is typically a 32 bit unsigned
     * value on most UNIX systems, so we return a long to avoid
     * integer overflow into the negatives in case values above
     * and including 2^31 are being used.
     *
     * @return the UID value.
     */
    public long getUID() { return ZipUtil.bigToLong(uid); }

    /**
     * Gets the GID as a long.  GID is typically a 32 bit unsigned
     * value on most UNIX systems, so we return a long to avoid
     * integer overflow into the negatives in case values above
     * and including 2^31 are being used.
     *
     * @return the GID value.
     */
    public long getGID() { return ZipUtil.bigToLong(gid); }

    /**
     * Sets the UID.
     *
     * @param l UID value to set on this extra field.
     */
    public void setUID(final long l) {
        this.uid = ZipUtil.longToBig(l);
    }

    /**
     * Sets the GID.
     *
     * @param l GID value to set on this extra field.
     */
    public void setGID(final long l) {
        this.gid = ZipUtil.longToBig(l);
    }

    /**
     * Length of the extra field in the local file data - without
     * Header-ID or length specifier.
     *
     * @return a <code>ZipShort</code> for the length of the data of this extra field
     */
    @Override
    public ZipShort getLocalFileDataLength() {
        byte[] b = trimLeadingZeroesForceMinLength(uid.toByteArray());
        final int uidSize = b == null ? 0 : b.length;
        b = trimLeadingZeroesForceMinLength(gid.toByteArray());
        final int gidSize = b == null ? 0 : b.length;

        // The 3 comes from:  version=1 + uidsize=1 + gidsize=1
        return new ZipShort(3 + uidSize + gidSize);
    }

    /**
     * Length of the extra field in the central directory data - without
     * Header-ID or length specifier.
     *
     * @return a <code>ZipShort</code> for the length of the data of this extra field
     */
    @Override
    public ZipShort getCentralDirectoryLength() {
        return ZERO;
    }

    /**
     * The actual data to put into local file data - without Header-ID
     * or length specifier.
     *
     * @return get the data
     */
    @Override
    public byte[] getLocalFileDataData() {
        byte[] uidBytes = uid.toByteArray();
        byte[] gidBytes = gid.toByteArray();

        // BigInteger might prepend a leading-zero to force a positive representation
        // (e.g., so that the sign-bit is set to zero).  We need to remove that
        // before sending the number over the wire.
        uidBytes = trimLeadingZeroesForceMinLength(uidBytes);
        int uidBytesLen = uidBytes != null ? uidBytes.length : 0;
        gidBytes = trimLeadingZeroesForceMinLength(gidBytes);
        int gidBytesLen = gidBytes != null ? gidBytes.length : 0;

        // Couldn't bring myself to just call getLocalFileDataLength() when we've
        // already got the arrays right here.  Yeah, yeah, I know, premature
        // optimization is the root of all...
        //
        // The 3 comes from:  version=1 + uidsize=1 + gidsize=1
        final byte[] data = new byte[3 + uidBytesLen + gidBytesLen];

        // reverse() switches byte array from big-endian to little-endian.
        if (uidBytes != null) {
            reverse(uidBytes);
        }
        if (gidBytes != null) {
            reverse(gidBytes);
        }

        int pos = 0;
        data[pos++] = unsignedIntToSignedByte(version);
        data[pos++] = unsignedIntToSignedByte(uidBytesLen);
        if (uidBytes != null) {
            System.arraycopy(uidBytes, 0, data, pos, uidBytesLen);
        }
        pos += uidBytesLen;
        data[pos++] = unsignedIntToSignedByte(gidBytesLen);
        if (gidBytes != null) {
            System.arraycopy(gidBytes, 0, data, pos, gidBytesLen);
        }
        return data;
    }

    /**
     * The actual data to put into central directory data - without Header-ID
     * or length specifier.
     *
     * @return get the data
     */
    @Override
    public byte[] getCentralDirectoryData() {
        return new byte[0];
    }

    /**
     * Populate data from this array as if it was in local file data.
     *
     * @param data   an array of bytes
     * @param offset the start offset
     * @param length the number of bytes in the array from offset
     * @throws java.util.zip.ZipException on error
     */
    @Override
    public void parseFromLocalFileData(
            final byte[] data, int offset, final int length
    ) throws ZipException {
        reset();
        this.version = signedByteToUnsignedInt(data[offset++]);
        final int uidSize = signedByteToUnsignedInt(data[offset++]);
        final byte[] uidBytes = new byte[uidSize];
        System.arraycopy(data, offset, uidBytes, 0, uidSize);
        offset += uidSize;
        this.uid = new BigInteger(1, reverse(uidBytes)); // sign-bit forced positive

        final int gidSize = signedByteToUnsignedInt(data[offset++]);
        final byte[] gidBytes = new byte[gidSize];
        System.arraycopy(data, offset, gidBytes, 0, gidSize);
        this.gid = new BigInteger(1, reverse(gidBytes)); // sign-bit forced positive
    }

    /**
     * Doesn't do anything since this class doesn't store anything
     * inside the central directory.
     */
    @Override
    public void parseFromCentralDirectoryData(
            final byte[] buffer, final int offset, final int length
    ) throws ZipException {
    }

    /**
     * Reset state back to newly constructed state.  Helps us make sure
     * parse() calls always generate clean results.
     */
    private void reset() {
        // Typical UID/GID of the first non-root user created on a unix system.
        uid = ONE_THOUSAND;
        gid = ONE_THOUSAND;
    }

    /**
     * Returns a String representation of this class useful for
     * debugging purposes.
     *
     * @return A String representation of this class useful for
     *         debugging purposes.
     */
    @Override
    public String toString() {
        return "0x7875 Zip Extra Field: UID=" + uid + " GID=" + gid;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof X7875_NewUnix) {
            final X7875_NewUnix xf = (X7875_NewUnix) o;
            // We assume uid and gid can never be null.
            return version == xf.version && uid.equals(xf.uid) && gid.equals(xf.gid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hc = -1234567 * version;
        // Since most UID's and GID's are below 65,536, this is (hopefully!)
        // a nice way to make sure typical UID and GID values impact the hash
        // as much as possible.
        hc ^= Integer.rotateLeft(uid.hashCode(), 16);
        hc ^= gid.hashCode();
        return hc;
    }

    /**
     * Not really for external usage, but marked "package" visibility
     * to help us JUnit it.   Trims a byte array of leading zeroes while
     * also enforcing a minimum length, and thus it really trims AND pads
     * at the same time.
     *
     * @param array byte[] array to trim & pad.
     * @return trimmed & padded byte[] array.
     */
    static byte[] trimLeadingZeroesForceMinLength(final byte[] array) {
        if (array == null) {
            return array;
        }

        int pos = 0;
        for (final byte b : array) {
            if (b == 0) {
                pos++;
            } else {
                break;
            }
        }

        /*

        I agonized over my choice of MIN_LENGTH=1.  Here's the situation:
        InfoZip (the tool I am using to test interop) always sets these
        to length=4.  And so a UID of 0 (typically root) for example is
        encoded as {4,0,0,0,0} (len=4, 32 bits of zero), when it could just
        as easily be encoded as {1,0} (len=1, 8 bits of zero) according to
        the spec.

        In the end I decided on MIN_LENGTH=1 for four reasons:

        1.)  We are adhering to the spec as far as I can tell, and so
             a consumer that cannot parse this is broken.

        2.)  Fundamentally, zip files are about shrinking things, so
             let's save a few bytes per entry while we can.

        3.)  Of all the people creating zip files using commons-
             compress, how many care about UNIX UID/GID attributes
             of the files they store?   (e.g., I am probably thinking
             way too hard about this and no one cares!)

        4.)  InfoZip's tool, even though it carefully stores every UID/GID
             for every file zipped on a unix machine (by default) currently
             appears unable to ever restore UID/GID.
             unzip -X has no effect on my machine, even when run as root!!!!

        And thus it is decided:  MIN_LENGTH=1.

        If anyone runs into interop problems from this, feel free to set
        it to MIN_LENGTH=4 at some future time, and then we will behave
        exactly like InfoZip (requires changes to unit tests, though).

        And I am sorry that the time you spent reading this comment is now
        gone and you can never have it back.

        */
        final int MIN_LENGTH = 1;

        final byte[] trimmedArray = new byte[Math.max(MIN_LENGTH, array.length - pos)];
        final int startPos = trimmedArray.length - (array.length - pos);
        System.arraycopy(array, pos, trimmedArray, startPos, trimmedArray.length - startPos);
        return trimmedArray;
    }
}
