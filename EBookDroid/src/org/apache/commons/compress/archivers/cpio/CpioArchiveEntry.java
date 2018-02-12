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
package org.apache.commons.compress.archivers.cpio;

import java.io.File;
import java.util.Date;

import org.apache.commons.compress.archivers.ArchiveEntry;

/**
 * A cpio archive consists of a sequence of files. There are several types of
 * headers defided in two categories of new and old format. The headers are
 * recognized by magic numbers:
 *
 * <ul>
 * <li>"070701" ASCII for new portable format</li>
 * <li>"070702" ASCII for new portable format with CRC</li>
 * <li>"070707" ASCII for old ascii (also known as Portable ASCII, odc or old
 * character format</li>
 * <li>070707 binary for old binary</li>
 * </ul>
 *
 * <p>The old binary format is limited to 16 bits for user id, group
 * id, device, and inode numbers. It is limited to 4 gigabyte file
 * sizes.
 *
 * The old ASCII format is limited to 18 bits for the user id, group
 * id, device, and inode numbers. It is limited to 8 gigabyte file
 * sizes.
 *
 * The new ASCII format is limited to 4 gigabyte file sizes.
 *
 * CPIO 2.5 knows also about tar, but it is not recognized here.</p>
 *
 *
 * <h3>OLD FORMAT</h3>
 *
 * <p>Each file has a 76 (ascii) / 26 (binary) byte header, a variable
 * length, NUL terminated filename, and variable length file data. A
 * header for a filename "TRAILER!!!" indicates the end of the
 * archive.</p>
 *
 * <p>All the fields in the header are ISO 646 (approximately ASCII)
 * strings of octal numbers, left padded, not NUL terminated.</p>
 *
 * <pre>
 * FIELDNAME        NOTES
 * c_magic          The integer value octal 070707.  This value can be used to deter-
 *                  mine whether this archive is written with little-endian or big-
 *                  endian integers.
 * c_dev            Device that contains a directory entry for this file
 * c_ino            I-node number that identifies the input file to the file system
 * c_mode           The mode specifies both the regular permissions and the file type.
 * c_uid            Numeric User ID of the owner of the input file
 * c_gid            Numeric Group ID of the owner of the input file
 * c_nlink          Number of links that are connected to the input file
 * c_rdev           For block special and character special entries, this field
 *                  contains the associated device number.  For all other entry types,
 *                  it should be set to zero by writers and ignored by readers.
 * c_mtime[2]       Modification time of the file, indicated as the number of seconds
 *                  since the start of the epoch, 00:00:00 UTC January 1, 1970.  The
 *                  four-byte integer is stored with the most-significant 16 bits
 *                  first followed by the least-significant 16 bits.  Each of the two
 *                  16 bit values are stored in machine-native byte order.
 * c_namesize       Length of the path name, including the terminating null byte
 * c_filesize[2]    Length of the file in bytes. This is the length of the data
 *                  section that follows the header structure. Must be 0 for
 *                  FIFOs and directories
 *
 * All fields are unsigned short fields with 16-bit integer values
 * apart from c_mtime and c_filesize which are 32-bit integer values
 * </pre>
 *
 * <p>If necessary, the filename and file data are padded with a NUL byte to an even length</p>
 *
 * <p>Special files, directories, and the trailer are recorded with
 * the h_filesize field equal to 0.</p>
 *
 * <p>In the ASCII version of this format, the 16-bit entries are represented as 6-byte octal numbers,
 * and the 32-bit entries are represented as 11-byte octal numbers. No padding is added.</p>
 *
 * <h3>NEW FORMAT</h3>
 *
 * <p>Each file has a 110 byte header, a variable length, NUL
 * terminated filename, and variable length file data. A header for a
 * filename "TRAILER!!!" indicates the end of the archive. All the
 * fields in the header are ISO 646 (approximately ASCII) strings of
 * hexadecimal numbers, left padded, not NUL terminated.</p>
 *
 * <pre>
 * FIELDNAME        NOTES
 * c_magic[6]       The string 070701 for new ASCII, the string 070702 for new ASCII with CRC
 * c_ino[8]
 * c_mode[8]
 * c_uid[8]
 * c_gid[8]
 * c_nlink[8]
 * c_mtim[8]
 * c_filesize[8]    must be 0 for FIFOs and directories
 * c_maj[8]
 * c_min[8]
 * c_rmaj[8]        only valid for chr and blk special files
 * c_rmin[8]        only valid for chr and blk special files
 * c_namesize[8]    count includes terminating NUL in pathname
 * c_check[8]       0 for "new" portable format; for CRC format
 *                  the sum of all the bytes in the file
 * </pre>
 *
 * <p>New ASCII Format The "new" ASCII format uses 8-byte hexadecimal
 * fields for all numbers and separates device numbers into separate
 * fields for major and minor numbers.</p>
 *
 * <p>The pathname is followed by NUL bytes so that the total size of
 * the fixed header plus pathname is a multiple of four. Likewise, the
 * file data is padded to a multiple of four bytes.</p>
 *
 * <p>This class uses mutable fields and is not considered to be
 * threadsafe.</p>
 *
 * <p>Based on code from the jRPM project (http://jrpm.sourceforge.net).</p>
 *
 * <p>The MAGIC numbers and other constants are defined in {@link CpioConstants}</p>
 *
 * <p>
 * N.B. does not handle the cpio "tar" format
 * </p>
 * @NotThreadSafe
 * @see <a href="http://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt">http://people.freebsd.org/~kientzle/libarchive/man/cpio.5.txt</a>
 */
public class CpioArchiveEntry implements CpioConstants, ArchiveEntry {

    // Header description fields - should be same throughout an archive

    /**
     * See constructor documenation for possible values.
     */
    private final short fileFormat;

    /** The number of bytes in each header record; depends on the file format */
    private final int headerSize;

    /** The boundary to which the header and data elements are aligned: 0, 2 or 4 bytes */
    private final int alignmentBoundary;

    // Header fields

    private long chksum = 0;

    /** Number of bytes in the file */
    private long filesize = 0;

    private long gid = 0;

    private long inode = 0;

    private long maj = 0;

    private long min = 0;

    private long mode = 0;

    private long mtime = 0;

    private String name;

    private long nlink = 0;

    private long rmaj = 0;

    private long rmin = 0;

    private long uid = 0;

    /**
     * Creates a CpioArchiveEntry with a specified format.
     *
     * @param format
     *            The cpio format for this entry.
     * <p>
     * Possible format values are:
     * <pre>
     * CpioConstants.FORMAT_NEW
     * CpioConstants.FORMAT_NEW_CRC
     * CpioConstants.FORMAT_OLD_BINARY
     * CpioConstants.FORMAT_OLD_ASCII
     * </pre>
     */
    public CpioArchiveEntry(final short format) {
        switch (format) {
        case FORMAT_NEW:
            this.headerSize = 110;
            this.alignmentBoundary = 4;
            break;
        case FORMAT_NEW_CRC:
            this.headerSize = 110;
            this.alignmentBoundary = 4;
            break;
        case FORMAT_OLD_ASCII:
            this.headerSize = 76;
            this.alignmentBoundary = 0;
            break;
        case FORMAT_OLD_BINARY:
            this.headerSize = 26;
            this.alignmentBoundary = 2;
            break;
        default:
            throw new IllegalArgumentException("Unknown header type");
        }
        this.fileFormat = format;
    }

    /**
     * Creates a CpioArchiveEntry with a specified name. The format of
     * this entry will be the new format.
     *
     * @param name
     *            The name of this entry.
     */
    public CpioArchiveEntry(final String name) {
        this(FORMAT_NEW, name);
    }

    /**
     * Creates a CpioArchiveEntry with a specified name.
     *
     * @param format
     *            The cpio format for this entry.
     * @param name
     *            The name of this entry.
     * <p>
     * Possible format values are:
     * <pre>
     * CpioConstants.FORMAT_NEW
     * CpioConstants.FORMAT_NEW_CRC
     * CpioConstants.FORMAT_OLD_BINARY
     * CpioConstants.FORMAT_OLD_ASCII
     * </pre>
     *
     * @since 1.1
     */
    public CpioArchiveEntry(final short format, final String name) {
        this(format);
        this.name = name;
    }

    /**
     * Creates a CpioArchiveEntry with a specified name. The format of
     * this entry will be the new format.
     *
     * @param name
     *            The name of this entry.
     * @param size
     *            The size of this entry
     */
    public CpioArchiveEntry(final String name, final long size) {
        this(name);
        this.setSize(size);
    }

    /**
     * Creates a CpioArchiveEntry with a specified name.
     *
     * @param format
     *            The cpio format for this entry.
     * @param name
     *            The name of this entry.
     * @param size
     *            The size of this entry
     * <p>
     * Possible format values are:
     * <pre>
     * CpioConstants.FORMAT_NEW
     * CpioConstants.FORMAT_NEW_CRC
     * CpioConstants.FORMAT_OLD_BINARY
     * CpioConstants.FORMAT_OLD_ASCII
     * </pre>
     *
     * @since 1.1
     */
    public CpioArchiveEntry(final short format, final String name,
                            final long size) {
        this(format, name);
        this.setSize(size);
    }

    /**
     * Creates a CpioArchiveEntry with a specified name for a
     * specified file. The format of this entry will be the new
     * format.
     *
     * @param inputFile
     *            The file to gather information from.
     * @param entryName
     *            The name of this entry.
     */
    public CpioArchiveEntry(final File inputFile, final String entryName) {
        this(FORMAT_NEW, inputFile, entryName);
    }

    /**
     * Creates a CpioArchiveEntry with a specified name for a
     * specified file.
     *
     * @param format
     *            The cpio format for this entry.
     * @param inputFile
     *            The file to gather information from.
     * @param entryName
     *            The name of this entry.
     * <p>
     * Possible format values are:
     * <pre>
     * CpioConstants.FORMAT_NEW
     * CpioConstants.FORMAT_NEW_CRC
     * CpioConstants.FORMAT_OLD_BINARY
     * CpioConstants.FORMAT_OLD_ASCII
     * </pre>
     *
     * @since 1.1
     */
    public CpioArchiveEntry(final short format, final File inputFile,
                            final String entryName) {
        this(format, entryName, inputFile.isFile() ? inputFile.length() : 0);
        if (inputFile.isDirectory()){
            setMode(C_ISDIR);
        } else if (inputFile.isFile()){
            setMode(C_ISREG);
        } else {
            throw new IllegalArgumentException("Cannot determine type of file "
                                               + inputFile.getName());
        }
        // TODO set other fields as needed
        setTime(inputFile.lastModified() / 1000);
    }

    /**
     * Check if the method is allowed for the defined format.
     */
    private void checkNewFormat() {
        if ((this.fileFormat & FORMAT_NEW_MASK) == 0) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Check if the method is allowed for the defined format.
     */
    private void checkOldFormat() {
        if ((this.fileFormat & FORMAT_OLD_MASK) == 0) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Get the checksum.
     * Only supported for the new formats.
     *
     * @return Returns the checksum.
     * @throws UnsupportedOperationException if the format is not a new format
     */
    public long getChksum() {
        checkNewFormat();
        return this.chksum & 0xFFFFFFFFL;
    }

    /**
     * Get the device id.
     *
     * @return Returns the device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with a new
     *             format.
     */
    public long getDevice() {
        checkOldFormat();
        return this.min;
    }

    /**
     * Get the major device id.
     *
     * @return Returns the major device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with an old
     *             format.
     */
    public long getDeviceMaj() {
        checkNewFormat();
        return this.maj;
    }

    /**
     * Get the minor device id
     *
     * @return Returns the minor device id.
     * @throws UnsupportedOperationException if format is not a new format
     */
    public long getDeviceMin() {
        checkNewFormat();
        return this.min;
    }

    /**
     * Get the filesize.
     *
     * @return Returns the filesize.
     * @see org.apache.commons.compress.archivers.ArchiveEntry#getSize()
     */
    @Override
    public long getSize() {
        return this.filesize;
    }

    /**
     * Get the format for this entry.
     *
     * @return Returns the format.
     */
    public short getFormat() {
        return this.fileFormat;
    }

    /**
     * Get the group id.
     *
     * @return Returns the group id.
     */
    public long getGID() {
        return this.gid;
    }

    /**
     * Get the header size for this CPIO format
     *
     * @return Returns the header size in bytes.
     */
    public int getHeaderSize() {
        return this.headerSize;
    }

    /**
     * Get the alignment boundary for this CPIO format
     *
     * @return Returns the aligment boundary (0, 2, 4) in bytes
     */
    public int getAlignmentBoundary() {
        return this.alignmentBoundary;
    }

    /**
     * Get the number of bytes needed to pad the header to the alignment boundary.
     *
     * @return the number of bytes needed to pad the header (0,1,2,3)
     */
    public int getHeaderPadCount(){
        if (this.alignmentBoundary == 0) { return 0; }
        int size = this.headerSize + 1;  // Name has terminating null
        if (name != null) {
            size += name.length();
        }
        final int remain = size % this.alignmentBoundary;
        if (remain > 0){
            return this.alignmentBoundary - remain;
        }
        return 0;
    }

    /**
     * Get the number of bytes needed to pad the data to the alignment boundary.
     *
     * @return the number of bytes needed to pad the data (0,1,2,3)
     */
    public int getDataPadCount(){
        if (this.alignmentBoundary == 0) { return 0; }
        final long size = this.filesize;
        final int remain = (int) (size % this.alignmentBoundary);
        if (remain > 0){
            return this.alignmentBoundary - remain;
        }
        return 0;
    }

    /**
     * Set the inode.
     *
     * @return Returns the inode.
     */
    public long getInode() {
        return this.inode;
    }

    /**
     * Get the mode of this entry (e.g. directory, regular file).
     *
     * @return Returns the mode.
     */
    public long getMode() {
        return mode == 0 && !CPIO_TRAILER.equals(name) ? C_ISREG : mode;
    }

    /**
     * Get the name.
     *
     * @return Returns the name.
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the number of links.
     *
     * @return Returns the number of links.
     */
    public long getNumberOfLinks() {
        return nlink == 0 ?
            isDirectory() ? 2 : 1
            : nlink;
    }

    /**
     * Get the remote device id.
     *
     * @return Returns the remote device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with a new
     *             format.
     */
    public long getRemoteDevice() {
        checkOldFormat();
        return this.rmin;
    }

    /**
     * Get the remote major device id.
     *
     * @return Returns the remote major device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with an old
     *             format.
     */
    public long getRemoteDeviceMaj() {
        checkNewFormat();
        return this.rmaj;
    }

    /**
     * Get the remote minor device id.
     *
     * @return Returns the remote minor device id.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with an old
     *             format.
     */
    public long getRemoteDeviceMin() {
        checkNewFormat();
        return this.rmin;
    }

    /**
     * Get the time in seconds.
     *
     * @return Returns the time.
     */
    public long getTime() {
        return this.mtime;
    }

    @Override
    public Date getLastModifiedDate() {
        return new Date(1000 * getTime());
    }

    /**
     * Get the user id.
     *
     * @return Returns the user id.
     */
    public long getUID() {
        return this.uid;
    }

    /**
     * Check if this entry represents a block device.
     *
     * @return TRUE if this entry is a block device.
     */
    public boolean isBlockDevice() {
        return CpioUtil.fileType(mode) == C_ISBLK;
    }

    /**
     * Check if this entry represents a character device.
     *
     * @return TRUE if this entry is a character device.
     */
    public boolean isCharacterDevice() {
        return CpioUtil.fileType(mode) == C_ISCHR;
    }

    /**
     * Check if this entry represents a directory.
     *
     * @return TRUE if this entry is a directory.
     */
    @Override
    public boolean isDirectory() {
        return CpioUtil.fileType(mode) == C_ISDIR;
    }

    /**
     * Check if this entry represents a network device.
     *
     * @return TRUE if this entry is a network device.
     */
    public boolean isNetwork() {
        return CpioUtil.fileType(mode) == C_ISNWK;
    }

    /**
     * Check if this entry represents a pipe.
     *
     * @return TRUE if this entry is a pipe.
     */
    public boolean isPipe() {
        return CpioUtil.fileType(mode) == C_ISFIFO;
    }

    /**
     * Check if this entry represents a regular file.
     *
     * @return TRUE if this entry is a regular file.
     */
    public boolean isRegularFile() {
        return CpioUtil.fileType(mode) == C_ISREG;
    }

    /**
     * Check if this entry represents a socket.
     *
     * @return TRUE if this entry is a socket.
     */
    public boolean isSocket() {
        return CpioUtil.fileType(mode) == C_ISSOCK;
    }

    /**
     * Check if this entry represents a symbolic link.
     *
     * @return TRUE if this entry is a symbolic link.
     */
    public boolean isSymbolicLink() {
        return CpioUtil.fileType(mode) == C_ISLNK;
    }

    /**
     * Set the checksum. The checksum is calculated by adding all bytes of a
     * file to transfer (crc += buf[pos] &amp; 0xFF).
     *
     * @param chksum
     *            The checksum to set.
     */
    public void setChksum(final long chksum) {
        checkNewFormat();
        this.chksum = chksum & 0xFFFFFFFFL;
    }

    /**
     * Set the device id.
     *
     * @param device
     *            The device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with a new
     *             format.
     */
    public void setDevice(final long device) {
        checkOldFormat();
        this.min = device;
    }

    /**
     * Set major device id.
     *
     * @param maj
     *            The major device id to set.
     */
    public void setDeviceMaj(final long maj) {
        checkNewFormat();
        this.maj = maj;
    }

    /**
     * Set the minor device id
     *
     * @param min
     *            The minor device id to set.
     */
    public void setDeviceMin(final long min) {
        checkNewFormat();
        this.min = min;
    }

    /**
     * Set the filesize.
     *
     * @param size
     *            The filesize to set.
     */
    public void setSize(final long size) {
        if (size < 0 || size > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("invalid entry size <" + size
                                               + ">");
        }
        this.filesize = size;
    }

    /**
     * Set the group id.
     *
     * @param gid
     *            The group id to set.
     */
    public void setGID(final long gid) {
        this.gid = gid;
    }

    /**
     * Set the inode.
     *
     * @param inode
     *            The inode to set.
     */
    public void setInode(final long inode) {
        this.inode = inode;
    }

    /**
     * Set the mode of this entry (e.g. directory, regular file).
     *
     * @param mode
     *            The mode to set.
     */
    public void setMode(final long mode) {
        final long maskedMode = mode & S_IFMT;
        switch ((int) maskedMode) {
        case C_ISDIR:
        case C_ISLNK:
        case C_ISREG:
        case C_ISFIFO:
        case C_ISCHR:
        case C_ISBLK:
        case C_ISSOCK:
        case C_ISNWK:
            break;
        default:
            throw new IllegalArgumentException(
                                               "Unknown mode. "
                                               + "Full: " + Long.toHexString(mode)
                                               + " Masked: " + Long.toHexString(maskedMode));
        }

        this.mode = mode;
    }

    /**
     * Set the name.
     *
     * @param name
     *            The name to set.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set the number of links.
     *
     * @param nlink
     *            The number of links to set.
     */
    public void setNumberOfLinks(final long nlink) {
        this.nlink = nlink;
    }

    /**
     * Set the remote device id.
     *
     * @param device
     *            The remote device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with a new
     *             format.
     */
    public void setRemoteDevice(final long device) {
        checkOldFormat();
        this.rmin = device;
    }

    /**
     * Set the remote major device id.
     *
     * @param rmaj
     *            The remote major device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with an old
     *             format.
     */
    public void setRemoteDeviceMaj(final long rmaj) {
        checkNewFormat();
        this.rmaj = rmaj;
    }

    /**
     * Set the remote minor device id.
     *
     * @param rmin
     *            The remote minor device id to set.
     * @throws UnsupportedOperationException
     *             if this method is called for a CpioArchiveEntry with an old
     *             format.
     */
    public void setRemoteDeviceMin(final long rmin) {
        checkNewFormat();
        this.rmin = rmin;
    }

    /**
     * Set the time in seconds.
     *
     * @param time
     *            The time to set.
     */
    public void setTime(final long time) {
        this.mtime = time;
    }

    /**
     * Set the user id.
     *
     * @param uid
     *            The user id to set.
     */
    public void setUID(final long uid) {
        this.uid = uid;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (name == null ? 0 : name.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CpioArchiveEntry other = (CpioArchiveEntry) obj;
        if (name == null) {
            return other.name == null;
        } else {
            return name.equals(other.name);
        }
    }
}
