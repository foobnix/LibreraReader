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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.utils.InputStreamStatistics;

import static org.apache.commons.compress.archivers.zip.ZipConstants.DWORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.SHORT;
import static org.apache.commons.compress.archivers.zip.ZipConstants.WORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC_SHORT;

/**
 * Replacement for <code>java.util.ZipFile</code>.
 *
 * <p>This class adds support for file name encodings other than UTF-8
 * (which is required to work on ZIP files created by native zip tools
 * and is able to skip a preamble like the one found in self
 * extracting archives.  Furthermore it returns instances of
 * <code>org.apache.commons.compress.archivers.zip.ZipArchiveEntry</code>
 * instead of <code>java.util.zip.ZipEntry</code>.</p>
 *
 * <p>It doesn't extend <code>java.util.zip.ZipFile</code> as it would
 * have to reimplement all methods anyway.  Like
 * <code>java.util.ZipFile</code>, it uses SeekableByteChannel under the
 * covers and supports compressed and uncompressed entries.  As of
 * Apache Commons Compress 1.3 it also transparently supports Zip64
 * extensions and thus individual entries and archives larger than 4
 * GB or with more than 65536 entries.</p>
 *
 * <p>The method signatures mimic the ones of
 * <code>java.util.zip.ZipFile</code>, with a couple of exceptions:
 *
 * <ul>
 *   <li>There is no getName method.</li>
 *   <li>entries has been renamed to getEntries.</li>
 *   <li>getEntries and getEntry return
 *   <code>org.apache.commons.compress.archivers.zip.ZipArchiveEntry</code>
 *   instances.</li>
 *   <li>close is allowed to throw IOException.</li>
 * </ul>
 *
 */
public class ZipFile implements Closeable {
    private static final int HASH_SIZE = 509;
    static final int NIBLET_MASK = 0x0f;
    static final int BYTE_SHIFT = 8;
    private static final int POS_0 = 0;
    private static final int POS_1 = 1;
    private static final int POS_2 = 2;
    private static final int POS_3 = 3;
    private static final byte[] ONE_ZERO_BYTE = new byte[1];

    /**
     * List of entries in the order they appear inside the central
     * directory.
     */
    private final List<ZipArchiveEntry> entries =
        new LinkedList<>();

    /**
     * Maps String to list of ZipArchiveEntrys, name -> actual entries.
     */
    private final Map<String, LinkedList<ZipArchiveEntry>> nameMap =
        new HashMap<>(HASH_SIZE);

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to UTF-8.</p>
     */
    private final String encoding;

    /**
     * The zip encoding to use for filenames and the file comment.
     */
    private final ZipEncoding zipEncoding;

    /**
     * File name of actual source.
     */
    private final String archiveName;

    /**
     * The actual data source.
     */
    private final SeekableByteChannel archive;

    /**
     * Whether to look for and use Unicode extra fields.
     */
    private final boolean useUnicodeExtraFields;

    /**
     * Whether the file is closed.
     */
    private volatile boolean closed = true;

    // cached buffers - must only be used locally in the class (COMPRESS-172 - reduce garbage collection)
    private final byte[] dwordBuf = new byte[DWORD];
    private final byte[] wordBuf = new byte[WORD];
    private final byte[] cfhBuf = new byte[CFH_LEN];
    private final byte[] shortBuf = new byte[SHORT];
    private final ByteBuffer dwordBbuf = ByteBuffer.wrap(dwordBuf);
    private final ByteBuffer wordBbuf = ByteBuffer.wrap(wordBuf);
    private final ByteBuffer cfhBbuf = ByteBuffer.wrap(cfhBuf);

    /**
     * Opens the given file for reading, assuming "UTF8" for file names.
     *
     * @param f the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final File f) throws IOException {
        this(f, ZipEncodingHelper.UTF8);
    }

    /**
     * Opens the given file for reading, assuming "UTF8".
     *
     * @param name name of the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final String name) throws IOException {
        this(new File(name), ZipEncodingHelper.UTF8);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names, scanning unicode extra fields.
     *
     * @param name name of the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final String name, final String encoding) throws IOException {
        this(new File(name), encoding, true);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names and scanning for unicode extra fields.
     *
     * @param f the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final File f, final String encoding) throws IOException {
        this(f, encoding, true);
    }

    /**
     * Opens the given file for reading, assuming the specified
     * encoding for file names.
     *
     * @param f the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     *
     * @throws IOException if an error occurs while reading the file.
     */
    public ZipFile(final File f, final String encoding, final boolean useUnicodeExtraFields)
        throws IOException {
        this(Files.newByteChannel(f.toPath(), EnumSet.of(StandardOpenOption.READ)),
             f.getAbsolutePath(), encoding, useUnicodeExtraFields, true);
    }

    /**
     * Opens the given channel for reading, assuming "UTF8" for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the archive.
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.13
     */
    public ZipFile(final SeekableByteChannel channel)
            throws IOException {
        this(channel, "unknown archive", ZipEncodingHelper.UTF8, true);
    }

    /**
     * Opens the given channel for reading, assuming the specified
     * encoding for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the archive.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.13
     */
    public ZipFile(final SeekableByteChannel channel, final String encoding)
        throws IOException {
        this(channel, "unknown archive", encoding, true);
    }

    /**
     * Opens the given channel for reading, assuming the specified
     * encoding for file names.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to read from an in-memory archive.</p>
     *
     * @param channel the archive.
     * @param archiveName name of the archive, used for error messages only.
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     * @param useUnicodeExtraFields whether to use InfoZIP Unicode
     * Extra Fields (if present) to set the file names.
     *
     * @throws IOException if an error occurs while reading the file.
     * @since 1.13
     */
    public ZipFile(final SeekableByteChannel channel, final String archiveName,
                   final String encoding, final boolean useUnicodeExtraFields)
        throws IOException {
        this(channel, archiveName, encoding, useUnicodeExtraFields, false);
    }

    private ZipFile(final SeekableByteChannel channel, final String archiveName,
                    final String encoding, final boolean useUnicodeExtraFields,
                    final boolean closeOnError)
        throws IOException {
        this.archiveName = archiveName;
        this.encoding = encoding;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        this.useUnicodeExtraFields = useUnicodeExtraFields;
        archive = channel;
        boolean success = false;
        try {
            final Map<ZipArchiveEntry, NameAndComment> entriesWithoutUTF8Flag =
                populateFromCentralDirectory();
            resolveLocalFileHeaderData(entriesWithoutUTF8Flag);
            success = true;
        } finally {
            closed = !success;
            if (!success && closeOnError) {
                IOUtils.closeQuietly(archive);
            }
        }
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * @return null if using the platform's default character encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Closes the archive.
     * @throws IOException if an error occurs closing the archive.
     */
    @Override
    public void close() throws IOException {
        // this flag is only written here and read in finalize() which
        // can never be run in parallel.
        // no synchronization needed.
        closed = true;

        archive.close();
    }

    /**
     * close a zipfile quietly; throw no io fault, do nothing
     * on a null parameter
     * @param zipfile file to close, can be null
     */
    public static void closeQuietly(final ZipFile zipfile) {
        IOUtils.closeQuietly(zipfile);
    }

    /**
     * Returns all entries.
     *
     * <p>Entries will be returned in the same order they appear
     * within the archive's central directory.</p>
     *
     * @return all entries as {@link ZipArchiveEntry} instances
     */
    public Enumeration<ZipArchiveEntry> getEntries() {
        return Collections.enumeration(entries);
    }

    /**
     * Returns all entries in physical order.
     *
     * <p>Entries will be returned in the same order their contents
     * appear within the archive.</p>
     *
     * @return all entries as {@link ZipArchiveEntry} instances
     *
     * @since 1.1
     */
    public Enumeration<ZipArchiveEntry> getEntriesInPhysicalOrder() {
        final ZipArchiveEntry[] allEntries = entries.toArray(new ZipArchiveEntry[entries.size()]);
        Arrays.sort(allEntries, offsetComparator);
        return Collections.enumeration(Arrays.asList(allEntries));
    }

    /**
     * Returns a named entry - or {@code null} if no entry by
     * that name exists.
     *
     * <p>If multiple entries with the same name exist the first entry
     * in the archive's central directory by that name is
     * returned.</p>
     *
     * @param name name of the entry.
     * @return the ZipArchiveEntry corresponding to the given name - or
     * {@code null} if not present.
     */
    public ZipArchiveEntry getEntry(final String name) {
        final LinkedList<ZipArchiveEntry> entriesOfThatName = nameMap.get(name);
        return entriesOfThatName != null ? entriesOfThatName.getFirst() : null;
    }

    /**
     * Returns all named entries in the same order they appear within
     * the archive's central directory.
     *
     * @param name name of the entry.
     * @return the Iterable&lt;ZipArchiveEntry&gt; corresponding to the
     * given name
     * @since 1.6
     */
    public Iterable<ZipArchiveEntry> getEntries(final String name) {
        final List<ZipArchiveEntry> entriesOfThatName = nameMap.get(name);
        return entriesOfThatName != null ? entriesOfThatName
            : Collections.<ZipArchiveEntry>emptyList();
    }

    /**
     * Returns all named entries in the same order their contents
     * appear within the archive.
     *
     * @param name name of the entry.
     * @return the Iterable&lt;ZipArchiveEntry&gt; corresponding to the
     * given name
     * @since 1.6
     */
    public Iterable<ZipArchiveEntry> getEntriesInPhysicalOrder(final String name) {
        ZipArchiveEntry[] entriesOfThatName = new ZipArchiveEntry[0];
        if (nameMap.containsKey(name)) {
            entriesOfThatName = nameMap.get(name).toArray(entriesOfThatName);
            Arrays.sort(entriesOfThatName, offsetComparator);
        }
        return Arrays.asList(entriesOfThatName);
    }

    /**
     * Whether this class is able to read the given entry.
     *
     * <p>May return false if it is set up to use encryption or a
     * compression method that hasn't been implemented yet.</p>
     * @since 1.1
     * @param ze the entry
     * @return whether this class is able to read the given entry.
     */
    public boolean canReadEntryData(final ZipArchiveEntry ze) {
        return ZipUtil.canHandleEntryData(ze);
    }

    /**
     * Expose the raw stream of the archive entry (compressed form).
     *
     * <p>This method does not relate to how/if we understand the payload in the
     * stream, since we really only intend to move it on to somewhere else.</p>
     *
     * @param ze The entry to get the stream for
     * @return The raw input stream containing (possibly) compressed data.
     * @since 1.11
     */
    public InputStream getRawInputStream(final ZipArchiveEntry ze) {
        if (!(ze instanceof Entry)) {
            return null;
        }
        final long start = ze.getDataOffset();
        return createBoundedInputStream(start, ze.getCompressedSize());
    }


    /**
     * Transfer selected entries from this zipfile to a given #ZipArchiveOutputStream.
     * Compression and all other attributes will be as in this file.
     * <p>This method transfers entries based on the central directory of the zip file.</p>
     *
     * @param target The zipArchiveOutputStream to write the entries to
     * @param predicate A predicate that selects which entries to write
     * @throws IOException on error
     */
    public void copyRawEntries(final ZipArchiveOutputStream target, final ZipArchiveEntryPredicate predicate)
            throws IOException {
        final Enumeration<ZipArchiveEntry> src = getEntriesInPhysicalOrder();
        while (src.hasMoreElements()) {
            final ZipArchiveEntry entry = src.nextElement();
            if (predicate.test( entry)) {
                target.addRawArchiveEntry(entry, getRawInputStream(entry));
            }
        }
    }

    /**
     * Returns an InputStream for reading the contents of the given entry.
     *
     * @param ze the entry to get the stream for.
     * @return a stream to read the entry from. The returned stream
     * implements {@link InputStreamStatistics}.
     * @throws IOException if unable to create an input stream from the zipentry
     */
    public InputStream getInputStream(final ZipArchiveEntry ze)
        throws IOException {
        if (!(ze instanceof Entry)) {
            return null;
        }
        // cast validity is checked just above
        ZipUtil.checkRequestedFeatures(ze);
        final long start = ze.getDataOffset();

        // doesn't get closed if the method is not supported - which
        // should never happen because of the checkRequestedFeatures
        // call above
        final InputStream is =
            new BufferedInputStream(createBoundedInputStream(start, ze.getCompressedSize())); //NOSONAR
        switch (ZipMethod.getMethodByCode(ze.getMethod())) {
            case STORED:
                return new StoredStatisticsStream(is);
            case UNSHRINKING:
                return new UnshrinkingInputStream(is);
            case IMPLODING:
                return new ExplodingInputStream(ze.getGeneralPurposeBit().getSlidingDictionarySize(),
                        ze.getGeneralPurposeBit().getNumberOfShannonFanoTrees(), is);
            case DEFLATED:
                final Inflater inflater = new Inflater(true);
                // Inflater with nowrap=true has this odd contract for a zero padding
                // byte following the data stream; this used to be zlib's requirement
                // and has been fixed a long time ago, but the contract persists so
                // we comply.
                // https://docs.oracle.com/javase/7/docs/api/java/util/zip/Inflater.html#Inflater(boolean)
                return new InflaterInputStreamWithStatistics(new SequenceInputStream(is, new ByteArrayInputStream(ONE_ZERO_BYTE)),
                    inflater) {
                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            inflater.end();
                        }
                    }
                };
            case BZIP2:
                return new BZip2CompressorInputStream(is);
            case ENHANCED_DEFLATED:
                return new Deflate64CompressorInputStream(is);
            case AES_ENCRYPTED:
            case EXPANDING_LEVEL_1:
            case EXPANDING_LEVEL_2:
            case EXPANDING_LEVEL_3:
            case EXPANDING_LEVEL_4:
            case JPEG:
            case LZMA:
            case PKWARE_IMPLODING:
            case PPMD:
            case TOKENIZATION:
            case UNKNOWN:
            case WAVPACK:
            case XZ:
            default:
                throw new ZipException("Found unsupported compression method "
                                       + ze.getMethod());
        }
    }

    /**
     * <p>
     * Convenience method to return the entry's content as a String if isUnixSymlink()
     * returns true for it, otherwise returns null.
     * </p>
     *
     * <p>This method assumes the symbolic link's file name uses the
     * same encoding that as been specified for this ZipFile.</p>
     *
     * @param entry ZipArchiveEntry object that represents the symbolic link
     * @return entry's content as a String
     * @throws IOException problem with content's input stream
     * @since 1.5
     */
    public String getUnixSymlink(final ZipArchiveEntry entry) throws IOException {
        if (entry != null && entry.isUnixSymlink()) {
            try (InputStream in = getInputStream(entry)) {
                return zipEncoding.decode(IOUtils.toByteArray(in));
            }
        }
        return null;
    }

    /**
     * Ensures that the close method of this zipfile is called when
     * there are no more references to it.
     * @see #close()
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            if (!closed) {
                System.err.println("Cleaning up unclosed ZipFile for archive "
                                   + archiveName);
                close();
            }
        } finally {
            super.finalize();
        }
    }

    /**
     * Length of a "central directory" entry structure without file
     * name, extra fields or comment.
     */
    private static final int CFH_LEN =
        /* version made by                 */ SHORT
        /* version needed to extract       */ + SHORT
        /* general purpose bit flag        */ + SHORT
        /* compression method              */ + SHORT
        /* last mod file time              */ + SHORT
        /* last mod file date              */ + SHORT
        /* crc-32                          */ + WORD
        /* compressed size                 */ + WORD
        /* uncompressed size               */ + WORD
        /* filename length                 */ + SHORT
        /* extra field length              */ + SHORT
        /* file comment length             */ + SHORT
        /* disk number start               */ + SHORT
        /* internal file attributes        */ + SHORT
        /* external file attributes        */ + WORD
        /* relative offset of local header */ + WORD;

    private static final long CFH_SIG =
        ZipLong.getValue(ZipArchiveOutputStream.CFH_SIG);

    /**
     * Reads the central directory of the given archive and populates
     * the internal tables with ZipArchiveEntry instances.
     *
     * <p>The ZipArchiveEntrys will know all data that can be obtained from
     * the central directory alone, but not the data that requires the
     * local file header or additional data to be read.</p>
     *
     * @return a map of zipentries that didn't have the language
     * encoding flag set when read.
     */
    private Map<ZipArchiveEntry, NameAndComment> populateFromCentralDirectory()
        throws IOException {
        final HashMap<ZipArchiveEntry, NameAndComment> noUTF8Flag =
            new HashMap<>();

        positionAtCentralDirectory();

        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        long sig = ZipLong.getValue(wordBuf);

        if (sig != CFH_SIG && startsWithLocalFileHeader()) {
            throw new IOException("central directory is empty, can't expand"
                                  + " corrupt archive.");
        }

        while (sig == CFH_SIG) {
            readCentralDirectoryEntry(noUTF8Flag);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            sig = ZipLong.getValue(wordBuf);
        }
        return noUTF8Flag;
    }

    /**
     * Reads an individual entry of the central directory, creats an
     * ZipArchiveEntry from it and adds it to the global maps.
     *
     * @param noUTF8Flag map used to collect entries that don't have
     * their UTF-8 flag set and whose name will be set by data read
     * from the local file header later.  The current entry may be
     * added to this map.
     */
    private void
        readCentralDirectoryEntry(final Map<ZipArchiveEntry, NameAndComment> noUTF8Flag)
        throws IOException {
        cfhBbuf.rewind();
        IOUtils.readFully(archive, cfhBbuf);
        int off = 0;
        final Entry ze = new Entry();

        final int versionMadeBy = ZipShort.getValue(cfhBuf, off);
        off += SHORT;
        ze.setVersionMadeBy(versionMadeBy);
        ze.setPlatform((versionMadeBy >> BYTE_SHIFT) & NIBLET_MASK);

        ze.setVersionRequired(ZipShort.getValue(cfhBuf, off));
        off += SHORT; // version required

        final GeneralPurposeBit gpFlag = GeneralPurposeBit.parse(cfhBuf, off);
        final boolean hasUTF8Flag = gpFlag.usesUTF8ForNames();
        final ZipEncoding entryEncoding =
            hasUTF8Flag ? ZipEncodingHelper.UTF8_ZIP_ENCODING : zipEncoding;
        if (hasUTF8Flag) {
            ze.setNameSource(ZipArchiveEntry.NameSource.NAME_WITH_EFS_FLAG);
        }
        ze.setGeneralPurposeBit(gpFlag);
        ze.setRawFlag(ZipShort.getValue(cfhBuf, off));

        off += SHORT;

        //noinspection MagicConstant
        ze.setMethod(ZipShort.getValue(cfhBuf, off));
        off += SHORT;

        final long time = ZipUtil.dosToJavaTime(ZipLong.getValue(cfhBuf, off));
        ze.setTime(time);
        off += WORD;

        ze.setCrc(ZipLong.getValue(cfhBuf, off));
        off += WORD;

        ze.setCompressedSize(ZipLong.getValue(cfhBuf, off));
        off += WORD;

        ze.setSize(ZipLong.getValue(cfhBuf, off));
        off += WORD;

        final int fileNameLen = ZipShort.getValue(cfhBuf, off);
        off += SHORT;

        final int extraLen = ZipShort.getValue(cfhBuf, off);
        off += SHORT;

        final int commentLen = ZipShort.getValue(cfhBuf, off);
        off += SHORT;

        final int diskStart = ZipShort.getValue(cfhBuf, off);
        off += SHORT;

        ze.setInternalAttributes(ZipShort.getValue(cfhBuf, off));
        off += SHORT;

        ze.setExternalAttributes(ZipLong.getValue(cfhBuf, off));
        off += WORD;

        final byte[] fileName = new byte[fileNameLen];
        IOUtils.readFully(archive, ByteBuffer.wrap(fileName));
        ze.setName(entryEncoding.decode(fileName), fileName);

        // LFH offset,
        ze.setLocalHeaderOffset(ZipLong.getValue(cfhBuf, off));
        // data offset will be filled later
        entries.add(ze);

        final byte[] cdExtraData = new byte[extraLen];
        IOUtils.readFully(archive, ByteBuffer.wrap(cdExtraData));
        ze.setCentralDirectoryExtra(cdExtraData);

        setSizesAndOffsetFromZip64Extra(ze, diskStart);

        final byte[] comment = new byte[commentLen];
        IOUtils.readFully(archive, ByteBuffer.wrap(comment));
        ze.setComment(entryEncoding.decode(comment));

        if (!hasUTF8Flag && useUnicodeExtraFields) {
            noUTF8Flag.put(ze, new NameAndComment(fileName, comment));
        }
    }

    /**
     * If the entry holds a Zip64 extended information extra field,
     * read sizes from there if the entry's sizes are set to
     * 0xFFFFFFFFF, do the same for the offset of the local file
     * header.
     *
     * <p>Ensures the Zip64 extra either knows both compressed and
     * uncompressed size or neither of both as the internal logic in
     * ExtraFieldUtils forces the field to create local header data
     * even if they are never used - and here a field with only one
     * size would be invalid.</p>
     */
    private void setSizesAndOffsetFromZip64Extra(final ZipArchiveEntry ze,
                                                 final int diskStart)
        throws IOException {
        final Zip64ExtendedInformationExtraField z64 =
            (Zip64ExtendedInformationExtraField)
            ze.getExtraField(Zip64ExtendedInformationExtraField.HEADER_ID);
        if (z64 != null) {
            final boolean hasUncompressedSize = ze.getSize() == ZIP64_MAGIC;
            final boolean hasCompressedSize = ze.getCompressedSize() == ZIP64_MAGIC;
            final boolean hasRelativeHeaderOffset =
                ze.getLocalHeaderOffset() == ZIP64_MAGIC;
            z64.reparseCentralDirectoryData(hasUncompressedSize,
                                            hasCompressedSize,
                                            hasRelativeHeaderOffset,
                                            diskStart == ZIP64_MAGIC_SHORT);

            if (hasUncompressedSize) {
                ze.setSize(z64.getSize().getLongValue());
            } else if (hasCompressedSize) {
                z64.setSize(new ZipEightByteInteger(ze.getSize()));
            }

            if (hasCompressedSize) {
                ze.setCompressedSize(z64.getCompressedSize().getLongValue());
            } else if (hasUncompressedSize) {
                z64.setCompressedSize(new ZipEightByteInteger(ze.getCompressedSize()));
            }

            if (hasRelativeHeaderOffset) {
                ze.setLocalHeaderOffset(z64.getRelativeHeaderOffset().getLongValue());
            }
        }
    }

    /**
     * Length of the "End of central directory record" - which is
     * supposed to be the last structure of the archive - without file
     * comment.
     */
    static final int MIN_EOCD_SIZE =
        /* end of central dir signature    */ WORD
        /* number of this disk             */ + SHORT
        /* number of the disk with the     */
        /* start of the central directory  */ + SHORT
        /* total number of entries in      */
        /* the central dir on this disk    */ + SHORT
        /* total number of entries in      */
        /* the central dir                 */ + SHORT
        /* size of the central directory   */ + WORD
        /* offset of start of central      */
        /* directory with respect to       */
        /* the starting disk number        */ + WORD
        /* zipfile comment length          */ + SHORT;

    /**
     * Maximum length of the "End of central directory record" with a
     * file comment.
     */
    private static final int MAX_EOCD_SIZE = MIN_EOCD_SIZE
        /* maximum length of zipfile comment */ + ZIP64_MAGIC_SHORT;

    /**
     * Offset of the field that holds the location of the first
     * central directory entry inside the "End of central directory
     * record" relative to the start of the "End of central directory
     * record".
     */
    private static final int CFD_LOCATOR_OFFSET =
        /* end of central dir signature    */ WORD
        /* number of this disk             */ + SHORT
        /* number of the disk with the     */
        /* start of the central directory  */ + SHORT
        /* total number of entries in      */
        /* the central dir on this disk    */ + SHORT
        /* total number of entries in      */
        /* the central dir                 */ + SHORT
        /* size of the central directory   */ + WORD;

    /**
     * Length of the "Zip64 end of central directory locator" - which
     * should be right in front of the "end of central directory
     * record" if one is present at all.
     */
    private static final int ZIP64_EOCDL_LENGTH =
        /* zip64 end of central dir locator sig */ WORD
        /* number of the disk with the start    */
        /* start of the zip64 end of            */
        /* central directory                    */ + WORD
        /* relative offset of the zip64         */
        /* end of central directory record      */ + DWORD
        /* total number of disks                */ + WORD;

    /**
     * Offset of the field that holds the location of the "Zip64 end
     * of central directory record" inside the "Zip64 end of central
     * directory locator" relative to the start of the "Zip64 end of
     * central directory locator".
     */
    private static final int ZIP64_EOCDL_LOCATOR_OFFSET =
        /* zip64 end of central dir locator sig */ WORD
        /* number of the disk with the start    */
        /* start of the zip64 end of            */
        /* central directory                    */ + WORD;

    /**
     * Offset of the field that holds the location of the first
     * central directory entry inside the "Zip64 end of central
     * directory record" relative to the start of the "Zip64 end of
     * central directory record".
     */
    private static final int ZIP64_EOCD_CFD_LOCATOR_OFFSET =
        /* zip64 end of central dir        */
        /* signature                       */ WORD
        /* size of zip64 end of central    */
        /* directory record                */ + DWORD
        /* version made by                 */ + SHORT
        /* version needed to extract       */ + SHORT
        /* number of this disk             */ + WORD
        /* number of the disk with the     */
        /* start of the central directory  */ + WORD
        /* total number of entries in the  */
        /* central directory on this disk  */ + DWORD
        /* total number of entries in the  */
        /* central directory               */ + DWORD
        /* size of the central directory   */ + DWORD;

    /**
     * Searches for either the &quot;Zip64 end of central directory
     * locator&quot; or the &quot;End of central dir record&quot;, parses
     * it and positions the stream at the first central directory
     * record.
     */
    private void positionAtCentralDirectory()
        throws IOException {
        positionAtEndOfCentralDirectoryRecord();
        boolean found = false;
        final boolean searchedForZip64EOCD =
            archive.position() > ZIP64_EOCDL_LENGTH;
        if (searchedForZip64EOCD) {
            archive.position(archive.position() - ZIP64_EOCDL_LENGTH);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            found = Arrays.equals(ZipArchiveOutputStream.ZIP64_EOCD_LOC_SIG,
                                  wordBuf);
        }
        if (!found) {
            // not a ZIP64 archive
            if (searchedForZip64EOCD) {
                skipBytes(ZIP64_EOCDL_LENGTH - WORD);
            }
            positionAtCentralDirectory32();
        } else {
            positionAtCentralDirectory64();
        }
    }

    /**
     * Parses the &quot;Zip64 end of central directory locator&quot;,
     * finds the &quot;Zip64 end of central directory record&quot; using the
     * parsed information, parses that and positions the stream at the
     * first central directory record.
     *
     * Expects stream to be positioned right behind the &quot;Zip64
     * end of central directory locator&quot;'s signature.
     */
    private void positionAtCentralDirectory64()
        throws IOException {
        skipBytes(ZIP64_EOCDL_LOCATOR_OFFSET
                  - WORD /* signature has already been read */);
        dwordBbuf.rewind();
        IOUtils.readFully(archive, dwordBbuf);
        archive.position(ZipEightByteInteger.getLongValue(dwordBuf));
        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        if (!Arrays.equals(wordBuf, ZipArchiveOutputStream.ZIP64_EOCD_SIG)) {
            throw new ZipException("archive's ZIP64 end of central "
                                   + "directory locator is corrupt.");
        }
        skipBytes(ZIP64_EOCD_CFD_LOCATOR_OFFSET
                  - WORD /* signature has already been read */);
        dwordBbuf.rewind();
        IOUtils.readFully(archive, dwordBbuf);
        archive.position(ZipEightByteInteger.getLongValue(dwordBuf));
    }

    /**
     * Parses the &quot;End of central dir record&quot; and positions
     * the stream at the first central directory record.
     *
     * Expects stream to be positioned at the beginning of the
     * &quot;End of central dir record&quot;.
     */
    private void positionAtCentralDirectory32()
        throws IOException {
        skipBytes(CFD_LOCATOR_OFFSET);
        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        archive.position(ZipLong.getValue(wordBuf));
    }

    /**
     * Searches for the and positions the stream at the start of the
     * &quot;End of central dir record&quot;.
     */
    private void positionAtEndOfCentralDirectoryRecord()
        throws IOException {
        final boolean found = tryToLocateSignature(MIN_EOCD_SIZE, MAX_EOCD_SIZE,
                                             ZipArchiveOutputStream.EOCD_SIG);
        if (!found) {
            throw new ZipException("archive is not a ZIP archive");
        }
    }

    /**
     * Searches the archive backwards from minDistance to maxDistance
     * for the given signature, positions the RandomaccessFile right
     * at the signature if it has been found.
     */
    private boolean tryToLocateSignature(final long minDistanceFromEnd,
                                         final long maxDistanceFromEnd,
                                         final byte[] sig) throws IOException {
        boolean found = false;
        long off = archive.size() - minDistanceFromEnd;
        final long stopSearching =
            Math.max(0L, archive.size() - maxDistanceFromEnd);
        if (off >= 0) {
            for (; off >= stopSearching; off--) {
                archive.position(off);
                try {
                    wordBbuf.rewind();
                    IOUtils.readFully(archive, wordBbuf);
                    wordBbuf.flip();
                } catch (EOFException ex) {
                    break;
                }
                int curr = wordBbuf.get();
                if (curr == sig[POS_0]) {
                    curr = wordBbuf.get();
                    if (curr == sig[POS_1]) {
                        curr = wordBbuf.get();
                        if (curr == sig[POS_2]) {
                            curr = wordBbuf.get();
                            if (curr == sig[POS_3]) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (found) {
            archive.position(off);
        }
        return found;
    }

    /**
     * Skips the given number of bytes or throws an EOFException if
     * skipping failed.
     */
    private void skipBytes(final int count) throws IOException {
        long currentPosition = archive.position();
        long newPosition = currentPosition + count;
        if (newPosition > archive.size()) {
            throw new EOFException();
        }
        archive.position(newPosition);
    }

    /**
     * Number of bytes in local file header up to the &quot;length of
     * filename&quot; entry.
     */
    private static final long LFH_OFFSET_FOR_FILENAME_LENGTH =
        /* local file header signature     */ WORD
        /* version needed to extract       */ + SHORT
        /* general purpose bit flag        */ + SHORT
        /* compression method              */ + SHORT
        /* last mod file time              */ + SHORT
        /* last mod file date              */ + SHORT
        /* crc-32                          */ + WORD
        /* compressed size                 */ + WORD
        /* uncompressed size               */ + (long) WORD;

    /**
     * Walks through all recorded entries and adds the data available
     * from the local file header.
     *
     * <p>Also records the offsets for the data to read from the
     * entries.</p>
     */
    private void resolveLocalFileHeaderData(final Map<ZipArchiveEntry, NameAndComment>
                                            entriesWithoutUTF8Flag)
        throws IOException {
        for (final ZipArchiveEntry zipArchiveEntry : entries) {
            // entries is filled in populateFromCentralDirectory and
            // never modified
            final Entry ze = (Entry) zipArchiveEntry;
            final long offset = ze.getLocalHeaderOffset();
            archive.position(offset + LFH_OFFSET_FOR_FILENAME_LENGTH);
            wordBbuf.rewind();
            IOUtils.readFully(archive, wordBbuf);
            wordBbuf.flip();
            wordBbuf.get(shortBuf);
            final int fileNameLen = ZipShort.getValue(shortBuf);
            wordBbuf.get(shortBuf);
            final int extraFieldLen = ZipShort.getValue(shortBuf);
            skipBytes(fileNameLen);
            final byte[] localExtraData = new byte[extraFieldLen];
            IOUtils.readFully(archive, ByteBuffer.wrap(localExtraData));
            ze.setExtra(localExtraData);
            ze.setDataOffset(offset + LFH_OFFSET_FOR_FILENAME_LENGTH
                + SHORT + SHORT + fileNameLen + extraFieldLen);
            ze.setStreamContiguous(true);

            if (entriesWithoutUTF8Flag.containsKey(ze)) {
                final NameAndComment nc = entriesWithoutUTF8Flag.get(ze);
                ZipUtil.setNameAndCommentFromExtraFields(ze, nc.name,
                                                         nc.comment);
            }

            final String name = ze.getName();
            LinkedList<ZipArchiveEntry> entriesOfThatName = nameMap.get(name);
            if (entriesOfThatName == null) {
                entriesOfThatName = new LinkedList<>();
                nameMap.put(name, entriesOfThatName);
            }
            entriesOfThatName.addLast(ze);
        }
    }

    /**
     * Checks whether the archive starts with a LFH.  If it doesn't,
     * it may be an empty archive.
     */
    private boolean startsWithLocalFileHeader() throws IOException {
        archive.position(0);
        wordBbuf.rewind();
        IOUtils.readFully(archive, wordBbuf);
        return Arrays.equals(wordBuf, ZipArchiveOutputStream.LFH_SIG);
    }

    /**
     * Creates new BoundedInputStream, according to implementation of
     * underlying archive channel.
     */
    private BoundedInputStream createBoundedInputStream(long start, long remaining) {
        return archive instanceof FileChannel ?
            new BoundedFileChannelInputStream(start, remaining) :
            new BoundedInputStream(start, remaining);
    }

    /**
     * InputStream that delegates requests to the underlying
     * SeekableByteChannel, making sure that only bytes from a certain
     * range can be read.
     */
    private class BoundedInputStream extends InputStream {
        private ByteBuffer singleByteBuffer;
        private final long end;
        private long loc;

        BoundedInputStream(final long start, final long remaining) {
            this.end = start+remaining;
            if (this.end < start) {
                // check for potential vulnerability due to overflow
                throw new IllegalArgumentException("Invalid length of stream at offset="+start+", length="+remaining);
            }
            loc = start;
        }

        @Override
        public synchronized int read() throws IOException {
            if (loc >= end) {
                return -1;
            }
            if (singleByteBuffer == null) {
                singleByteBuffer = ByteBuffer.allocate(1);
            }
            else {
                singleByteBuffer.rewind();
            }
            int read = read(loc, singleByteBuffer);
            if (read < 0) {
                return read;
            }
            loc++;
            return singleByteBuffer.get() & 0xff;
        }

        @Override
        public synchronized int read(final byte[] b, final int off, int len) throws IOException {
            if (len <= 0) {
                return 0;
            }

            if (len > end-loc) {
                if (loc >= end) {
                    return -1;
                }
                len = (int)(end-loc);
            }

            ByteBuffer buf;
            buf = ByteBuffer.wrap(b, off, len);
            int ret = read(loc, buf);
            if (ret > 0) {
                loc += ret;
                return ret;
            }
            return ret;
        }

        protected int read(long pos, ByteBuffer buf) throws IOException {
            int read;
            synchronized (archive) {
                archive.position(pos);
                read = archive.read(buf);
            }
            buf.flip();
            return read;
        }
    }

    /**
     * Lock-free implementation of BoundedInputStream. The
     * implementation uses positioned reads on the underlying archive
     * file channel and therefore performs significantly faster in
     * concurrent environment.
     */
    private class BoundedFileChannelInputStream extends BoundedInputStream {
        private final FileChannel archive;

        BoundedFileChannelInputStream(final long start, final long remaining) {
            super(start, remaining);
            archive = (FileChannel)ZipFile.this.archive;
        }

        @Override
        protected int read(long pos, ByteBuffer buf) throws IOException {
            int read = archive.read(buf, pos);
            buf.flip();
            return read;
        }
    }

    private static final class NameAndComment {
        private final byte[] name;
        private final byte[] comment;
        private NameAndComment(final byte[] name, final byte[] comment) {
            this.name = name;
            this.comment = comment;
        }
    }

    /**
     * Compares two ZipArchiveEntries based on their offset within the archive.
     *
     * <p>Won't return any meaningful results if one of the entries
     * isn't part of the archive at all.</p>
     *
     * @since 1.1
     */
    private final Comparator<ZipArchiveEntry> offsetComparator =
        new Comparator<ZipArchiveEntry>() {
        @Override
        public int compare(final ZipArchiveEntry e1, final ZipArchiveEntry e2) {
            if (e1 == e2) {
                return 0;
            }

            final Entry ent1 = e1 instanceof Entry ? (Entry) e1 : null;
            final Entry ent2 = e2 instanceof Entry ? (Entry) e2 : null;
            if (ent1 == null) {
                return 1;
            }
            if (ent2 == null) {
                return -1;
            }
            final long val = (ent1.getLocalHeaderOffset()
                        - ent2.getLocalHeaderOffset());
            return val == 0 ? 0 : val < 0 ? -1 : +1;
        }
    };

    /**
     * Extends ZipArchiveEntry to store the offset within the archive.
     */
    private static class Entry extends ZipArchiveEntry {

        Entry() {
        }

        @Override
        public int hashCode() {
            return 3 * super.hashCode()
                + (int) getLocalHeaderOffset()+(int)(getLocalHeaderOffset()>>32);
        }

        @Override
        public boolean equals(final Object other) {
            if (super.equals(other)) {
                // super.equals would return false if other were not an Entry
                final Entry otherEntry = (Entry) other;
                return getLocalHeaderOffset()
                        == otherEntry.getLocalHeaderOffset()
                    && getDataOffset()
                        == otherEntry.getDataOffset();
            }
            return false;
        }
    }

    private static class StoredStatisticsStream extends CountingInputStream implements InputStreamStatistics {
        StoredStatisticsStream(InputStream in) {
            super(in);
        }

        @Override
        public long getCompressedCount() {
            return super.getBytesRead();
        }

        @Override
        public long getUncompressedCount() {
            return getCompressedCount();
        }
    }
}
