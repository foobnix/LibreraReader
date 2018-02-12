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

/**
 * All constants needed by CPIO.
 *
 * based on code from the jRPM project (jrpm.sourceforge.net)
 *
 */
public interface CpioConstants {
    /** magic number of a cpio entry in the new format */
    String MAGIC_NEW = "070701";

    /** magic number of a cpio entry in the new format with crc */
    String MAGIC_NEW_CRC = "070702";

    /** magic number of a cpio entry in the old ascii format */
    String MAGIC_OLD_ASCII = "070707";

    /** magic number of a cpio entry in the old binary format */
    int MAGIC_OLD_BINARY = 070707;

    // These FORMAT_ constants are internal to the code

    /** write/read a CpioArchiveEntry in the new format */
    short FORMAT_NEW = 1;

    /** write/read a CpioArchiveEntry in the new format with crc */
    short FORMAT_NEW_CRC = 2;

    /** write/read a CpioArchiveEntry in the old ascii format */
    short FORMAT_OLD_ASCII = 4;

    /** write/read a CpioArchiveEntry in the old binary format */
    short FORMAT_OLD_BINARY = 8;

    /** Mask for both new formats */
    short FORMAT_NEW_MASK = 3;

    /** Mask for both old formats */
    short FORMAT_OLD_MASK = 12;

    /*
     * Constants for the MODE bits
     */

    /** Mask for all file type bits. */
    int S_IFMT   = 0170000;

 // http://www.opengroup.org/onlinepubs/9699919799/basedefs/cpio.h.html
 // has a list of the C_xxx constatnts

    /** Defines a socket */
    int C_ISSOCK = 0140000;

    /** Defines a symbolic link */
    int C_ISLNK  = 0120000;

    /** HP/UX network special (C_ISCTG) */
    int C_ISNWK  = 0110000;

    /** Defines a regular file */
    int C_ISREG  = 0100000;

    /** Defines a block device */
    int C_ISBLK  = 0060000;

    /** Defines a directory */
    int C_ISDIR  = 0040000;

    /** Defines a character device */
    int C_ISCHR  = 0020000;

    /** Defines a pipe */
    int C_ISFIFO = 0010000;


    /** Set user ID */
    int C_ISUID  = 0004000;

    /** Set group ID */
    int C_ISGID  = 0002000;

    /** On directories, restricted deletion flag. */
    int C_ISVTX  = 0001000;


    /** Permits the owner of a file to read the file */
    int C_IRUSR  = 0000400;

    /** Permits the owner of a file to write to the file */
    int C_IWUSR  = 0000200;

    /** Permits the owner of a file to execute the file or to search the directory */
    int C_IXUSR  = 0000100;


    /** Permits a file's group to read the file */
    int C_IRGRP  = 0000040;

    /** Permits a file's group to write to the file */
    int C_IWGRP  = 0000020;

    /** Permits a file's group to execute the file or to search the directory */
    int C_IXGRP  = 0000010;


    /** Permits others to read the file */
    int C_IROTH  = 0000004;

    /** Permits others to write to the file */
    int C_IWOTH  = 0000002;

    /** Permits others to execute the file or to search the directory */
    int C_IXOTH  = 0000001;

    /** The special trailer marker */
    String CPIO_TRAILER = "TRAILER!!!";

    /**
     * The default block size.
     *
     * @since 1.1
     */
    int BLOCK_SIZE = 512;
}
