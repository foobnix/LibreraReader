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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import static org.apache.commons.compress.archivers.zip.ZipConstants.DATA_DESCRIPTOR_MIN_VERSION;
import static org.apache.commons.compress.archivers.zip.ZipConstants.DEFLATE_MIN_VERSION;
import static org.apache.commons.compress.archivers.zip.ZipConstants.DWORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.INITIAL_VERSION;
import static org.apache.commons.compress.archivers.zip.ZipConstants.SHORT;
import static org.apache.commons.compress.archivers.zip.ZipConstants.WORD;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MAGIC_SHORT;
import static org.apache.commons.compress.archivers.zip.ZipConstants.ZIP64_MIN_VERSION;
import static org.apache.commons.compress.archivers.zip.ZipLong.putLong;
import static org.apache.commons.compress.archivers.zip.ZipShort.putShort;

/**
 * Reimplementation of {@link java.util.zip.ZipOutputStream
 * java.util.zip.ZipOutputStream} that does handle the extended
 * functionality of this package, especially internal/external file
 * attributes and extra fields with different layouts for local file
 * data and central directory entries.
 *
 * <p>This class will try to use {@link
 * java.nio.channels.SeekableByteChannel} when it knows that the
 * output is going to go to a file.</p>
 *
 * <p>If SeekableByteChannel cannot be used, this implementation will use
 * a Data Descriptor to store size and CRC information for {@link
 * #DEFLATED DEFLATED} entries, this means, you don't need to
 * calculate them yourself.  Unfortunately this is not possible for
 * the {@link #STORED STORED} method, here setting the CRC and
 * uncompressed size information is required before {@link
 * #putArchiveEntry(ArchiveEntry)} can be called.</p>
 *
 * <p>As of Apache Commons Compress 1.3 it transparently supports Zip64
 * extensions and thus individual entries and archives larger than 4
 * GB or with more than 65536 entries in most cases but explicit
 * control is provided via {@link #setUseZip64}.  If the stream can not
 * use SeekableByteChannel and you try to write a ZipArchiveEntry of
 * unknown size then Zip64 extensions will be disabled by default.</p>
 *
 * @NotThreadSafe
 */
public class ZipArchiveOutputStream extends ArchiveOutputStream {

    static final int BUFFER_SIZE = 512;
    private static final int LFH_SIG_OFFSET = 0;
    private static final int LFH_VERSION_NEEDED_OFFSET = 4;
    private static final int LFH_GPB_OFFSET = 6;
    private static final int LFH_METHOD_OFFSET = 8;
    private static final int LFH_TIME_OFFSET = 10;
    private static final int LFH_CRC_OFFSET = 14;
    private static final int LFH_COMPRESSED_SIZE_OFFSET = 18;
    private static final int LFH_ORIGINAL_SIZE_OFFSET = 22;
    private static final int LFH_FILENAME_LENGTH_OFFSET = 26;
    private static final int LFH_EXTRA_LENGTH_OFFSET = 28;
    private static final int LFH_FILENAME_OFFSET = 30;
    private static final int CFH_SIG_OFFSET = 0;
    private static final int CFH_VERSION_MADE_BY_OFFSET = 4;
    private static final int CFH_VERSION_NEEDED_OFFSET = 6;
    private static final int CFH_GPB_OFFSET = 8;
    private static final int CFH_METHOD_OFFSET = 10;
    private static final int CFH_TIME_OFFSET = 12;
    private static final int CFH_CRC_OFFSET = 16;
    private static final int CFH_COMPRESSED_SIZE_OFFSET = 20;
    private static final int CFH_ORIGINAL_SIZE_OFFSET = 24;
    private static final int CFH_FILENAME_LENGTH_OFFSET = 28;
    private static final int CFH_EXTRA_LENGTH_OFFSET = 30;
    private static final int CFH_COMMENT_LENGTH_OFFSET = 32;
    private static final int CFH_DISK_NUMBER_OFFSET = 34;
    private static final int CFH_INTERNAL_ATTRIBUTES_OFFSET = 36;
    private static final int CFH_EXTERNAL_ATTRIBUTES_OFFSET = 38;
    private static final int CFH_LFH_OFFSET = 42;
    private static final int CFH_FILENAME_OFFSET = 46;

    /** indicates if this archive is finished. protected for use in Jar implementation */
    protected boolean finished = false;

    /**
     * Compression method for deflated entries.
     */
    public static final int DEFLATED = java.util.zip.ZipEntry.DEFLATED;

    /**
     * Default compression level for deflated entries.
     */
    public static final int DEFAULT_COMPRESSION = Deflater.DEFAULT_COMPRESSION;

    /**
     * Compression method for stored entries.
     */
    public static final int STORED = java.util.zip.ZipEntry.STORED;

    /**
     * default encoding for file names and comment.
     */
    static final String DEFAULT_ENCODING = ZipEncodingHelper.UTF8;

    /**
     * General purpose flag, which indicates that filenames are
     * written in UTF-8.
     * @deprecated use {@link GeneralPurposeBit#UFT8_NAMES_FLAG} instead
     */
    @Deprecated
    public static final int EFS_FLAG = GeneralPurposeBit.UFT8_NAMES_FLAG;

    private static final byte[] EMPTY = new byte[0];

    /**
     * Current entry.
     */
    private CurrentEntry entry;

    /**
     * The file comment.
     */
    private String comment = "";

    /**
     * Compression level for next entry.
     */
    private int level = DEFAULT_COMPRESSION;

    /**
     * Has the compression level changed when compared to the last
     * entry?
     */
    private boolean hasCompressionLevelChanged = false;

    /**
     * Default compression method for next entry.
     */
    private int method = java.util.zip.ZipEntry.DEFLATED;

    /**
     * List of ZipArchiveEntries written so far.
     */
    private final List<ZipArchiveEntry> entries =
        new LinkedList<>();

    private final StreamCompressor streamCompressor;

    /**
     * Start of central directory.
     */
    private long cdOffset = 0;

    /**
     * Length of central directory.
     */
    private long cdLength = 0;

    /**
     * Helper, a 0 as ZipShort.
     */
    private static final byte[] ZERO = {0, 0};

    /**
     * Helper, a 0 as ZipLong.
     */
    private static final byte[] LZERO = {0, 0, 0, 0};

    private static final byte[] ONE = ZipLong.getBytes(1L);

    /**
     * Holds some book-keeping data for each entry.
     */
    private final Map<ZipArchiveEntry, EntryMetaData> metaData =
        new HashMap<>();

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to UTF-8.</p>
     */
    private String encoding = DEFAULT_ENCODING;

    /**
     * The zip encoding to use for filenames and the file comment.
     *
     * This field is of internal use and will be set in {@link
     * #setEncoding(String)}.
     */
    private ZipEncoding zipEncoding =
        ZipEncodingHelper.getZipEncoding(DEFAULT_ENCODING);


    /**
     * This Deflater object is used for output.
     *
     */
    protected final Deflater def;
    /**
     * Optional random access output.
     */
    private final SeekableByteChannel channel;

    private final OutputStream out;

    /**
     * whether to use the general purpose bit flag when writing UTF-8
     * filenames or not.
     */
    private boolean useUTF8Flag = true;

    /**
     * Whether to encode non-encodable file names as UTF-8.
     */
    private boolean fallbackToUTF8 = false;

    /**
     * whether to create UnicodePathExtraField-s for each entry.
     */
    private UnicodeExtraFieldPolicy createUnicodeExtraFields = UnicodeExtraFieldPolicy.NEVER;

    /**
     * Whether anything inside this archive has used a ZIP64 feature.
     *
     * @since 1.3
     */
    private boolean hasUsedZip64 = false;

    private Zip64Mode zip64Mode = Zip64Mode.AsNeeded;

    private final byte[] copyBuffer = new byte[32768];
    private final Calendar calendarInstance = Calendar.getInstance();

    /**
     * Creates a new ZIP OutputStream filtering the underlying stream.
     * @param out the outputstream to zip
     */
    public ZipArchiveOutputStream(final OutputStream out) {
        this.out = out;
        this.channel = null;
        def = new Deflater(level, true);
        streamCompressor = StreamCompressor.create(out, def);
    }

    /**
     * Creates a new ZIP OutputStream writing to a File.  Will use
     * random access if possible.
     * @param file the file to zip to
     * @throws IOException on error
     */
    public ZipArchiveOutputStream(final File file) throws IOException {
        def = new Deflater(level, true);
        OutputStream o = null;
        SeekableByteChannel _channel = null;
        StreamCompressor _streamCompressor = null;
        try {
            _channel = Files.newByteChannel(file.toPath(),
                EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                           StandardOpenOption.READ,
                           StandardOpenOption.TRUNCATE_EXISTING));
            // will never get opened properly when an exception is thrown so doesn't need to get closed
            _streamCompressor = StreamCompressor.create(_channel, def); //NOSONAR
        } catch (final IOException e) {
            IOUtils.closeQuietly(_channel);
            _channel = null;
            o = new FileOutputStream(file);
            _streamCompressor = StreamCompressor.create(o, def);
        }
        out = o;
        channel = _channel;
        streamCompressor = _streamCompressor;
    }

    /**
     * Creates a new ZIP OutputStream writing to a SeekableByteChannel.
     *
     * <p>{@link
     * org.apache.commons.compress.utils.SeekableInMemoryByteChannel}
     * allows you to write to an in-memory archive using random
     * access.</p>
     *
     * @param channel the channel to zip to
     * @throws IOException on error
     * @since 1.13
     */
    public ZipArchiveOutputStream(SeekableByteChannel channel) throws IOException {
        this.channel = channel;
        def = new Deflater(level, true);
        streamCompressor = StreamCompressor.create(channel, def);
        out = null;
    }

    /**
     * This method indicates whether this archive is writing to a
     * seekable stream (i.e., to a random access file).
     *
     * <p>For seekable streams, you don't need to calculate the CRC or
     * uncompressed size for {@link #STORED} entries before
     * invoking {@link #putArchiveEntry(ArchiveEntry)}.
     * @return true if seekable
     */
    public boolean isSeekable() {
        return channel != null;
    }

    /**
     * The encoding to use for filenames and the file comment.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.
     * Defaults to UTF-8.</p>
     * @param encoding the encoding to use for file names, use null
     * for the platform's default encoding
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
        this.zipEncoding = ZipEncodingHelper.getZipEncoding(encoding);
        if (useUTF8Flag && !ZipEncodingHelper.isUTF8(encoding)) {
            useUTF8Flag = false;
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
     * Whether to set the language encoding flag if the file name
     * encoding is UTF-8.
     *
     * <p>Defaults to true.</p>
     *
     * @param b whether to set the language encoding flag if the file
     * name encoding is UTF-8
     */
    public void setUseLanguageEncodingFlag(final boolean b) {
        useUTF8Flag = b && ZipEncodingHelper.isUTF8(encoding);
    }

    /**
     * Whether to create Unicode Extra Fields.
     *
     * <p>Defaults to NEVER.</p>
     *
     * @param b whether to create Unicode Extra Fields.
     */
    public void setCreateUnicodeExtraFields(final UnicodeExtraFieldPolicy b) {
        createUnicodeExtraFields = b;
    }

    /**
     * Whether to fall back to UTF and the language encoding flag if
     * the file name cannot be encoded using the specified encoding.
     *
     * <p>Defaults to false.</p>
     *
     * @param b whether to fall back to UTF and the language encoding
     * flag if the file name cannot be encoded using the specified
     * encoding.
     */
    public void setFallbackToUTF8(final boolean b) {
        fallbackToUTF8 = b;
    }

    /**
     * Whether Zip64 extensions will be used.
     *
     * <p>When setting the mode to {@link Zip64Mode#Never Never},
     * {@link #putArchiveEntry}, {@link #closeArchiveEntry}, {@link
     * #finish} or {@link #close} may throw a {@link
     * Zip64RequiredException} if the entry's size or the total size
     * of the archive exceeds 4GB or there are more than 65536 entries
     * inside the archive.  Any archive created in this mode will be
     * readable by implementations that don't support Zip64.</p>
     *
     * <p>When setting the mode to {@link Zip64Mode#Always Always},
     * Zip64 extensions will be used for all entries.  Any archive
     * created in this mode may be unreadable by implementations that
     * don't support Zip64 even if all its contents would be.</p>
     *
     * <p>When setting the mode to {@link Zip64Mode#AsNeeded
     * AsNeeded}, Zip64 extensions will transparently be used for
     * those entries that require them.  This mode can only be used if
     * the uncompressed size of the {@link ZipArchiveEntry} is known
     * when calling {@link #putArchiveEntry} or the archive is written
     * to a seekable output (i.e. you have used the {@link
     * #ZipArchiveOutputStream(java.io.File) File-arg constructor}) -
     * this mode is not valid when the output stream is not seekable
     * and the uncompressed size is unknown when {@link
     * #putArchiveEntry} is called.</p>
     *
     * <p>If no entry inside the resulting archive requires Zip64
     * extensions then {@link Zip64Mode#Never Never} will create the
     * smallest archive.  {@link Zip64Mode#AsNeeded AsNeeded} will
     * create a slightly bigger archive if the uncompressed size of
     * any entry has initially been unknown and create an archive
     * identical to {@link Zip64Mode#Never Never} otherwise.  {@link
     * Zip64Mode#Always Always} will create an archive that is at
     * least 24 bytes per entry bigger than the one {@link
     * Zip64Mode#Never Never} would create.</p>
     *
     * <p>Defaults to {@link Zip64Mode#AsNeeded AsNeeded} unless
     * {@link #putArchiveEntry} is called with an entry of unknown
     * size and data is written to a non-seekable stream - in this
     * case the default is {@link Zip64Mode#Never Never}.</p>
     *
     * @since 1.3
     * @param mode Whether Zip64 extensions will be used.
     */
    public void setUseZip64(final Zip64Mode mode) {
        zip64Mode = mode;
    }

    /**
     * {@inheritDoc}
     * @throws Zip64RequiredException if the archive's size exceeds 4
     * GByte or there are more than 65535 entries inside the archive
     * and {@link #setUseZip64} is {@link Zip64Mode#Never}.
     */
    @Override
    public void finish() throws IOException {
        if (finished) {
            throw new IOException("This archive has already been finished");
        }

        if (entry != null) {
            throw new IOException("This archive contains unclosed entries.");
        }

        cdOffset = streamCompressor.getTotalBytesWritten();
        writeCentralDirectoryInChunks();

        cdLength = streamCompressor.getTotalBytesWritten() - cdOffset;
        writeZip64CentralDirectory();
        writeCentralDirectoryEnd();
        metaData.clear();
        entries.clear();
        streamCompressor.close();
        finished = true;
    }

    private void writeCentralDirectoryInChunks() throws IOException {
        final int NUM_PER_WRITE = 1000;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(70 * NUM_PER_WRITE);
        int count = 0;
        for (final ZipArchiveEntry ze : entries) {
            byteArrayOutputStream.write(createCentralFileHeader(ze));
            if (++count > NUM_PER_WRITE){
                writeCounted(byteArrayOutputStream.toByteArray());
                byteArrayOutputStream.reset();
                count = 0;
            }
        }
        writeCounted(byteArrayOutputStream.toByteArray());
    }

    /**
     * Writes all necessary data for this entry.
     * @throws IOException on error
     * @throws Zip64RequiredException if the entry's uncompressed or
     * compressed size exceeds 4 GByte and {@link #setUseZip64}
     * is {@link Zip64Mode#Never}.
     */
    @Override
    public void closeArchiveEntry() throws IOException {
        preClose();

        flushDeflater();

        final long bytesWritten = streamCompressor.getTotalBytesWritten() - entry.dataStart;
        final long realCrc = streamCompressor.getCrc32();
        entry.bytesRead = streamCompressor.getBytesRead();
        final Zip64Mode effectiveMode = getEffectiveZip64Mode(entry.entry);
        final boolean actuallyNeedsZip64 = handleSizesAndCrc(bytesWritten, realCrc, effectiveMode);
        closeEntry(actuallyNeedsZip64, false);
        streamCompressor.reset();
    }

    /**
     * Writes all necessary data for this entry.
     *
     * @param phased              This entry is second phase of a 2-phase zip creation, size, compressed size and crc
     *                            are known in ZipArchiveEntry
     * @throws IOException            on error
     * @throws Zip64RequiredException if the entry's uncompressed or
     *                                compressed size exceeds 4 GByte and {@link #setUseZip64}
     *                                is {@link Zip64Mode#Never}.
     */
    private void closeCopiedEntry(final boolean phased) throws IOException {
        preClose();
        entry.bytesRead = entry.entry.getSize();
        final Zip64Mode effectiveMode = getEffectiveZip64Mode(entry.entry);
        final boolean actuallyNeedsZip64 = checkIfNeedsZip64(effectiveMode);
        closeEntry(actuallyNeedsZip64, phased);
    }

    private void closeEntry(final boolean actuallyNeedsZip64, final boolean phased) throws IOException {
        if (!phased && channel != null) {
            rewriteSizesAndCrc(actuallyNeedsZip64);
        }

        if (!phased) {
            writeDataDescriptor(entry.entry);
        }
        entry = null;
    }

    private void preClose() throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }

        if (entry == null) {
            throw new IOException("No current entry to close");
        }

        if (!entry.hasWritten) {
            write(EMPTY, 0, 0);
        }
    }

    /**
     * Adds an archive entry with a raw input stream.
     *
     * If crc, size and compressed size are supplied on the entry, these values will be used as-is.
     * Zip64 status is re-established based on the settings in this stream, and the supplied value
     * is ignored.
     *
     * The entry is put and closed immediately.
     *
     * @param entry The archive entry to add
     * @param rawStream The raw input stream of a different entry. May be compressed/encrypted.
     * @throws IOException If copying fails
     */
    public void addRawArchiveEntry(final ZipArchiveEntry entry, final InputStream rawStream)
            throws IOException {
        final ZipArchiveEntry ae = new ZipArchiveEntry(entry);
        if (hasZip64Extra(ae)) {
            // Will be re-added as required. this may make the file generated with this method
            // somewhat smaller than standard mode,
            // since standard mode is unable to remove the zip 64 header.
            ae.removeExtraField(Zip64ExtendedInformationExtraField.HEADER_ID);
        }
        final boolean is2PhaseSource = ae.getCrc() != ZipArchiveEntry.CRC_UNKNOWN
                && ae.getSize() != ArchiveEntry.SIZE_UNKNOWN
                && ae.getCompressedSize() != ArchiveEntry.SIZE_UNKNOWN;
        putArchiveEntry(ae, is2PhaseSource);
        copyFromZipInputStream(rawStream);
        closeCopiedEntry(is2PhaseSource);
    }

    /**
     * Ensures all bytes sent to the deflater are written to the stream.
     */
    private void flushDeflater() throws IOException {
        if (entry.entry.getMethod() == DEFLATED) {
            streamCompressor.flushDeflater();
        }
    }

    /**
     * Ensures the current entry's size and CRC information is set to
     * the values just written, verifies it isn't too big in the
     * Zip64Mode.Never case and returns whether the entry would
     * require a Zip64 extra field.
     */
    private boolean handleSizesAndCrc(final long bytesWritten, final long crc,
                                      final Zip64Mode effectiveMode)
        throws ZipException {
        if (entry.entry.getMethod() == DEFLATED) {
            /* It turns out def.getBytesRead() returns wrong values if
             * the size exceeds 4 GB on Java < Java7
            entry.entry.setSize(def.getBytesRead());
            */
            entry.entry.setSize(entry.bytesRead);
            entry.entry.setCompressedSize(bytesWritten);
            entry.entry.setCrc(crc);

        } else if (channel == null) {
            if (entry.entry.getCrc() != crc) {
                throw new ZipException("bad CRC checksum for entry "
                                       + entry.entry.getName() + ": "
                                       + Long.toHexString(entry.entry.getCrc())
                                       + " instead of "
                                       + Long.toHexString(crc));
            }

            if (entry.entry.getSize() != bytesWritten) {
                throw new ZipException("bad size for entry "
                                       + entry.entry.getName() + ": "
                                       + entry.entry.getSize()
                                       + " instead of "
                                       + bytesWritten);
            }
        } else { /* method is STORED and we used SeekableByteChannel */
            entry.entry.setSize(bytesWritten);
            entry.entry.setCompressedSize(bytesWritten);
            entry.entry.setCrc(crc);
        }

        return checkIfNeedsZip64(effectiveMode);
    }

    /**
     * Verifies the sizes aren't too big in the Zip64Mode.Never case
     * and returns whether the entry would require a Zip64 extra
     * field.
     */
    private boolean checkIfNeedsZip64(final Zip64Mode effectiveMode)
            throws ZipException {
        final boolean actuallyNeedsZip64 = isZip64Required(entry.entry, effectiveMode);
        if (actuallyNeedsZip64 && effectiveMode == Zip64Mode.Never) {
            throw new Zip64RequiredException(Zip64RequiredException.getEntryTooBigMessage(entry.entry));
        }
        return actuallyNeedsZip64;
    }

    private boolean isZip64Required(final ZipArchiveEntry entry1, final Zip64Mode requestedMode) {
        return requestedMode == Zip64Mode.Always || isTooLageForZip32(entry1);
    }

    private boolean isTooLageForZip32(final ZipArchiveEntry zipArchiveEntry){
        return zipArchiveEntry.getSize() >= ZIP64_MAGIC || zipArchiveEntry.getCompressedSize() >= ZIP64_MAGIC;
    }

    /**
     * When using random access output, write the local file header
     * and potentiall the ZIP64 extra containing the correct CRC and
     * compressed/uncompressed sizes.
     */
    private void rewriteSizesAndCrc(final boolean actuallyNeedsZip64)
        throws IOException {
        final long save = channel.position();

        channel.position(entry.localDataStart);
        writeOut(ZipLong.getBytes(entry.entry.getCrc()));
        if (!hasZip64Extra(entry.entry) || !actuallyNeedsZip64) {
            writeOut(ZipLong.getBytes(entry.entry.getCompressedSize()));
            writeOut(ZipLong.getBytes(entry.entry.getSize()));
        } else {
            writeOut(ZipLong.ZIP64_MAGIC.getBytes());
            writeOut(ZipLong.ZIP64_MAGIC.getBytes());
        }

        if (hasZip64Extra(entry.entry)) {
            final ByteBuffer name = getName(entry.entry);
            final int nameLen = name.limit() - name.position();
            // seek to ZIP64 extra, skip header and size information
            channel.position(entry.localDataStart + 3 * WORD + 2 * SHORT
                             + nameLen + 2 * SHORT);
            // inside the ZIP64 extra uncompressed size comes
            // first, unlike the LFH, CD or data descriptor
            writeOut(ZipEightByteInteger.getBytes(entry.entry.getSize()));
            writeOut(ZipEightByteInteger.getBytes(entry.entry.getCompressedSize()));

            if (!actuallyNeedsZip64) {
                // do some cleanup:
                // * rewrite version needed to extract
                channel.position(entry.localDataStart  - 5 * SHORT);
                writeOut(ZipShort.getBytes(versionNeededToExtract(entry.entry.getMethod(), false, false)));

                // * remove ZIP64 extra so it doesn't get written
                //   to the central directory
                entry.entry.removeExtraField(Zip64ExtendedInformationExtraField
                                             .HEADER_ID);
                entry.entry.setExtra();

                // * reset hasUsedZip64 if it has been set because
                //   of this entry
                if (entry.causedUseOfZip64) {
                    hasUsedZip64 = false;
                }
            }
        }
        channel.position(save);
    }

    /**
     * {@inheritDoc}
     * @throws ClassCastException if entry is not an instance of ZipArchiveEntry
     * @throws Zip64RequiredException if the entry's uncompressed or
     * compressed size is known to exceed 4 GByte and {@link #setUseZip64}
     * is {@link Zip64Mode#Never}.
     */
    @Override
    public void putArchiveEntry(final ArchiveEntry archiveEntry) throws IOException {
        putArchiveEntry(archiveEntry, false);
    }

    /**
     * Writes the headers for an archive entry to the output stream.
     * The caller must then write the content to the stream and call
     * {@link #closeArchiveEntry()} to complete the process.

     * @param archiveEntry The archiveEntry
     * @param phased If true size, compressedSize and crc required to be known up-front in the archiveEntry
     * @throws ClassCastException if entry is not an instance of ZipArchiveEntry
     * @throws Zip64RequiredException if the entry's uncompressed or
     * compressed size is known to exceed 4 GByte and {@link #setUseZip64}
     * is {@link Zip64Mode#Never}.
     */
    private void putArchiveEntry(final ArchiveEntry archiveEntry, final boolean phased) throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }

        if (entry != null) {
            closeArchiveEntry();
        }

        entry = new CurrentEntry((ZipArchiveEntry) archiveEntry);
        entries.add(entry.entry);

        setDefaults(entry.entry);

        final Zip64Mode effectiveMode = getEffectiveZip64Mode(entry.entry);
        validateSizeInformation(effectiveMode);

        if (shouldAddZip64Extra(entry.entry, effectiveMode)) {

            final Zip64ExtendedInformationExtraField z64 = getZip64Extra(entry.entry);

            ZipEightByteInteger size;
            ZipEightByteInteger compressedSize;
            if (phased) {
                // sizes are already known
                size = new ZipEightByteInteger(entry.entry.getSize());
                compressedSize = new ZipEightByteInteger(entry.entry.getCompressedSize());
            } else if (entry.entry.getMethod() == STORED
                    && entry.entry.getSize() != ArchiveEntry.SIZE_UNKNOWN) {
                // actually, we already know the sizes
                compressedSize = size = new ZipEightByteInteger(entry.entry.getSize());
            } else {
                // just a placeholder, real data will be in data
                // descriptor or inserted later via SeekableByteChannel
                compressedSize = size = ZipEightByteInteger.ZERO;
            }
            z64.setSize(size);
            z64.setCompressedSize(compressedSize);
            entry.entry.setExtra();
        }

        if (entry.entry.getMethod() == DEFLATED && hasCompressionLevelChanged) {
            def.setLevel(level);
            hasCompressionLevelChanged = false;
        }
        writeLocalFileHeader((ZipArchiveEntry) archiveEntry, phased);
    }

    /**
     * Provides default values for compression method and last
     * modification time.
     */
    private void setDefaults(final ZipArchiveEntry entry) {
        if (entry.getMethod() == -1) { // not specified
            entry.setMethod(method);
        }

        if (entry.getTime() == -1) { // not specified
            entry.setTime(System.currentTimeMillis());
        }
    }

    /**
     * Throws an exception if the size is unknown for a stored entry
     * that is written to a non-seekable output or the entry is too
     * big to be written without Zip64 extra but the mode has been set
     * to Never.
     */
    private void validateSizeInformation(final Zip64Mode effectiveMode)
        throws ZipException {
        // Size/CRC not required if SeekableByteChannel is used
        if (entry.entry.getMethod() == STORED && channel == null) {
            if (entry.entry.getSize() == ArchiveEntry.SIZE_UNKNOWN) {
                throw new ZipException("uncompressed size is required for"
                                       + " STORED method when not writing to a"
                                       + " file");
            }
            if (entry.entry.getCrc() == ZipArchiveEntry.CRC_UNKNOWN) {
                throw new ZipException("crc checksum is required for STORED"
                                       + " method when not writing to a file");
            }
            entry.entry.setCompressedSize(entry.entry.getSize());
        }

        if ((entry.entry.getSize() >= ZIP64_MAGIC
             || entry.entry.getCompressedSize() >= ZIP64_MAGIC)
            && effectiveMode == Zip64Mode.Never) {
            throw new Zip64RequiredException(Zip64RequiredException
                                             .getEntryTooBigMessage(entry.entry));
        }
    }

    /**
     * Whether to addd a Zip64 extended information extra field to the
     * local file header.
     *
     * <p>Returns true if</p>
     *
     * <ul>
     * <li>mode is Always</li>
     * <li>or we already know it is going to be needed</li>
     * <li>or the size is unknown and we can ensure it won't hurt
     * other implementations if we add it (i.e. we can erase its
     * usage</li>
     * </ul>
     */
    private boolean shouldAddZip64Extra(final ZipArchiveEntry entry, final Zip64Mode mode) {
        return mode == Zip64Mode.Always
            || entry.getSize() >= ZIP64_MAGIC
            || entry.getCompressedSize() >= ZIP64_MAGIC
            || (entry.getSize() == ArchiveEntry.SIZE_UNKNOWN
                && channel != null && mode != Zip64Mode.Never);
    }

    /**
     * Set the file comment.
     * @param comment the comment
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * Sets the compression level for subsequent entries.
     *
     * <p>Default is Deflater.DEFAULT_COMPRESSION.</p>
     * @param level the compression level.
     * @throws IllegalArgumentException if an invalid compression
     * level is specified.
     */
    public void setLevel(final int level) {
        if (level < Deflater.DEFAULT_COMPRESSION
            || level > Deflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException("Invalid compression level: "
                                               + level);
        }
        hasCompressionLevelChanged = (this.level != level);
        this.level = level;
    }

    /**
     * Sets the default compression method for subsequent entries.
     *
     * <p>Default is DEFLATED.</p>
     * @param method an <code>int</code> from java.util.zip.ZipEntry
     */
    public void setMethod(final int method) {
        this.method = method;
    }

    /**
     * Whether this stream is able to write the given entry.
     *
     * <p>May return false if it is set up to use encryption or a
     * compression method that hasn't been implemented yet.</p>
     * @since 1.1
     */
    @Override
    public boolean canWriteEntryData(final ArchiveEntry ae) {
        if (ae instanceof ZipArchiveEntry) {
            final ZipArchiveEntry zae = (ZipArchiveEntry) ae;
            return zae.getMethod() != ZipMethod.IMPLODING.getCode()
                && zae.getMethod() != ZipMethod.UNSHRINKING.getCode()
                && ZipUtil.canHandleEntryData(zae);
        }
        return false;
    }

    /**
     * Writes bytes to ZIP entry.
     * @param b the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     */
    @Override
    public void write(final byte[] b, final int offset, final int length) throws IOException {
        if (entry == null) {
            throw new IllegalStateException("No current entry");
        }
        ZipUtil.checkRequestedFeatures(entry.entry);
        final long writtenThisTime = streamCompressor.write(b, offset, length, entry.entry.getMethod());
        count(writtenThisTime);
    }

    /**
     * Write bytes to output or random access file.
     * @param data the byte array to write
     * @throws IOException on error
     */
    private void writeCounted(final byte[] data) throws IOException {
        streamCompressor.writeCounted(data);
    }

    private void copyFromZipInputStream(final InputStream src) throws IOException {
        if (entry == null) {
            throw new IllegalStateException("No current entry");
        }
        ZipUtil.checkRequestedFeatures(entry.entry);
        entry.hasWritten = true;
        int length;
        while ((length = src.read(copyBuffer)) >= 0 )
        {
            streamCompressor.writeCounted(copyBuffer, 0, length);
            count( length );
        }
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with the stream.
     *
     * @throws  IOException  if an I/O error occurs.
     * @throws Zip64RequiredException if the archive's size exceeds 4
     * GByte or there are more than 65535 entries inside the archive
     * and {@link #setUseZip64} is {@link Zip64Mode#Never}.
     */
    @Override
    public void close() throws IOException {
        if (!finished) {
            finish();
        }
        destroy();
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out to the stream.
     *
     * @throws  IOException  if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /*
     * Various ZIP constants shared between this class, ZipArchiveInputStream and ZipFile
     */
    /**
     * local file header signature
     */
    static final byte[] LFH_SIG = ZipLong.LFH_SIG.getBytes(); //NOSONAR
    /**
     * data descriptor signature
     */
    static final byte[] DD_SIG = ZipLong.DD_SIG.getBytes(); //NOSONAR
    /**
     * central file header signature
     */
    static final byte[] CFH_SIG = ZipLong.CFH_SIG.getBytes(); //NOSONAR
    /**
     * end of central dir signature
     */
    static final byte[] EOCD_SIG = ZipLong.getBytes(0X06054B50L); //NOSONAR
    /**
     * ZIP64 end of central dir signature
     */
    static final byte[] ZIP64_EOCD_SIG = ZipLong.getBytes(0X06064B50L); //NOSONAR
    /**
     * ZIP64 end of central dir locator signature
     */
    static final byte[] ZIP64_EOCD_LOC_SIG = ZipLong.getBytes(0X07064B50L); //NOSONAR

    /**
     * Writes next block of compressed data to the output stream.
     * @throws IOException on error
     */
    protected final void deflate() throws IOException {
        streamCompressor.deflate();
    }

    /**
     * Writes the local file header entry
     * @param ze the entry to write
     * @throws IOException on error
     */
    protected void writeLocalFileHeader(final ZipArchiveEntry ze) throws IOException {
        writeLocalFileHeader(ze, false);
    }

    private void writeLocalFileHeader(final ZipArchiveEntry ze, final boolean phased) throws IOException {
        final boolean encodable = zipEncoding.canEncode(ze.getName());
        final ByteBuffer name = getName(ze);

        if (createUnicodeExtraFields != UnicodeExtraFieldPolicy.NEVER) {
            addUnicodeExtraFields(ze, encodable, name);
        }

        final long localHeaderStart = streamCompressor.getTotalBytesWritten();
        final byte[] localHeader = createLocalFileHeader(ze, name, encodable, phased, localHeaderStart);
        metaData.put(ze, new EntryMetaData(localHeaderStart, usesDataDescriptor(ze.getMethod(), phased)));
        entry.localDataStart = localHeaderStart + LFH_CRC_OFFSET; // At crc offset
        writeCounted(localHeader);
        entry.dataStart = streamCompressor.getTotalBytesWritten();
    }


    private byte[] createLocalFileHeader(final ZipArchiveEntry ze, final ByteBuffer name, final boolean encodable,
                                         final boolean phased, long archiveOffset) {
        ResourceAlignmentExtraField oldAlignmentEx =
            (ResourceAlignmentExtraField) ze.getExtraField(ResourceAlignmentExtraField.ID);
        if (oldAlignmentEx != null) {
            ze.removeExtraField(ResourceAlignmentExtraField.ID);
        }

        int alignment = ze.getAlignment();
        if (alignment <= 0 && oldAlignmentEx != null) {
            alignment = oldAlignmentEx.getAlignment();
        }

        if (alignment > 1 || (oldAlignmentEx != null && !oldAlignmentEx.allowMethodChange())) {
            int oldLength = LFH_FILENAME_OFFSET +
                            name.limit() - name.position() +
                            ze.getLocalFileDataExtra().length;

            int padding = (int) ((-archiveOffset - oldLength - ZipExtraField.EXTRAFIELD_HEADER_SIZE
                            - ResourceAlignmentExtraField.BASE_SIZE) &
                            (alignment - 1));
            ze.addExtraField(new ResourceAlignmentExtraField(alignment,
                            oldAlignmentEx != null && oldAlignmentEx.allowMethodChange(), padding));
        }

        final byte[] extra = ze.getLocalFileDataExtra();
        final int nameLen = name.limit() - name.position();
        final int len = LFH_FILENAME_OFFSET + nameLen + extra.length;
        final byte[] buf = new byte[len];

        System.arraycopy(LFH_SIG,  0, buf, LFH_SIG_OFFSET, WORD);

        //store method in local variable to prevent multiple method calls
        final int zipMethod = ze.getMethod();
        final boolean dataDescriptor = usesDataDescriptor(zipMethod, phased);

        putShort(versionNeededToExtract(zipMethod, hasZip64Extra(ze), dataDescriptor), buf, LFH_VERSION_NEEDED_OFFSET);

        final GeneralPurposeBit generalPurposeBit = getGeneralPurposeBits(!encodable && fallbackToUTF8, dataDescriptor);
        generalPurposeBit.encode(buf, LFH_GPB_OFFSET);

        // compression method
        putShort(zipMethod, buf, LFH_METHOD_OFFSET);

        ZipUtil.toDosTime(calendarInstance, ze.getTime(), buf, LFH_TIME_OFFSET);

        // CRC
        if (phased){
            putLong(ze.getCrc(), buf, LFH_CRC_OFFSET);
        } else if (zipMethod == DEFLATED || channel != null) {
            System.arraycopy(LZERO, 0, buf, LFH_CRC_OFFSET, WORD);
        } else {
            putLong(ze.getCrc(), buf, LFH_CRC_OFFSET);
        }

        // compressed length
        // uncompressed length
        if (hasZip64Extra(entry.entry)){
            // point to ZIP64 extended information extra field for
            // sizes, may get rewritten once sizes are known if
            // stream is seekable
            ZipLong.ZIP64_MAGIC.putLong(buf, LFH_COMPRESSED_SIZE_OFFSET);
            ZipLong.ZIP64_MAGIC.putLong(buf, LFH_ORIGINAL_SIZE_OFFSET);
        } else if (phased) {
            putLong(ze.getCompressedSize(), buf, LFH_COMPRESSED_SIZE_OFFSET);
            putLong(ze.getSize(), buf, LFH_ORIGINAL_SIZE_OFFSET);
        } else if (zipMethod == DEFLATED || channel != null) {
            System.arraycopy(LZERO, 0, buf, LFH_COMPRESSED_SIZE_OFFSET, WORD);
            System.arraycopy(LZERO, 0, buf, LFH_ORIGINAL_SIZE_OFFSET, WORD);
        } else { // Stored
            putLong(ze.getSize(), buf, LFH_COMPRESSED_SIZE_OFFSET);
            putLong(ze.getSize(), buf, LFH_ORIGINAL_SIZE_OFFSET);
        }
        // file name length
        putShort(nameLen, buf, LFH_FILENAME_LENGTH_OFFSET);

        // extra field length
        putShort(extra.length, buf, LFH_EXTRA_LENGTH_OFFSET);

        // file name
        System.arraycopy( name.array(), name.arrayOffset(), buf, LFH_FILENAME_OFFSET, nameLen);

        // extra fields
        System.arraycopy(extra, 0, buf, LFH_FILENAME_OFFSET + nameLen, extra.length);

        return buf;
    }


    /**
     * Adds UnicodeExtra fields for name and file comment if mode is
     * ALWAYS or the data cannot be encoded using the configured
     * encoding.
     */
    private void addUnicodeExtraFields(final ZipArchiveEntry ze, final boolean encodable,
                                       final ByteBuffer name)
        throws IOException {
        if (createUnicodeExtraFields == UnicodeExtraFieldPolicy.ALWAYS
            || !encodable) {
            ze.addExtraField(new UnicodePathExtraField(ze.getName(),
                                                       name.array(),
                                                       name.arrayOffset(),
                                                       name.limit()
                                                       - name.position()));
        }

        final String comm = ze.getComment();
        if (comm != null && !"".equals(comm)) {

            final boolean commentEncodable = zipEncoding.canEncode(comm);

            if (createUnicodeExtraFields == UnicodeExtraFieldPolicy.ALWAYS
                || !commentEncodable) {
                final ByteBuffer commentB = getEntryEncoding(ze).encode(comm);
                ze.addExtraField(new UnicodeCommentExtraField(comm,
                                                              commentB.array(),
                                                              commentB.arrayOffset(),
                                                              commentB.limit()
                                                              - commentB.position())
                                 );
            }
        }
    }

    /**
     * Writes the data descriptor entry.
     * @param ze the entry to write
     * @throws IOException on error
     */
    protected void writeDataDescriptor(final ZipArchiveEntry ze) throws IOException {
        if (!usesDataDescriptor(ze.getMethod(), false)) {
            return;
        }
        writeCounted(DD_SIG);
        writeCounted(ZipLong.getBytes(ze.getCrc()));
        if (!hasZip64Extra(ze)) {
            writeCounted(ZipLong.getBytes(ze.getCompressedSize()));
            writeCounted(ZipLong.getBytes(ze.getSize()));
        } else {
            writeCounted(ZipEightByteInteger.getBytes(ze.getCompressedSize()));
            writeCounted(ZipEightByteInteger.getBytes(ze.getSize()));
        }
    }

    /**
     * Writes the central file header entry.
     * @param ze the entry to write
     * @throws IOException on error
     * @throws Zip64RequiredException if the archive's size exceeds 4
     * GByte and {@link Zip64Mode #setUseZip64} is {@link
     * Zip64Mode#Never}.
     */
    protected void writeCentralFileHeader(final ZipArchiveEntry ze) throws IOException {
        final byte[] centralFileHeader = createCentralFileHeader(ze);
        writeCounted(centralFileHeader);
    }

    private byte[] createCentralFileHeader(final ZipArchiveEntry ze) throws IOException {

        final EntryMetaData entryMetaData = metaData.get(ze);
        final boolean needsZip64Extra = hasZip64Extra(ze)
                || ze.getCompressedSize() >= ZIP64_MAGIC
                || ze.getSize() >= ZIP64_MAGIC
                || entryMetaData.offset >= ZIP64_MAGIC
                || zip64Mode == Zip64Mode.Always;

        if (needsZip64Extra && zip64Mode == Zip64Mode.Never) {
            // must be the offset that is too big, otherwise an
            // exception would have been throw in putArchiveEntry or
            // closeArchiveEntry
            throw new Zip64RequiredException(Zip64RequiredException
                    .ARCHIVE_TOO_BIG_MESSAGE);
        }


        handleZip64Extra(ze, entryMetaData.offset, needsZip64Extra);

        return createCentralFileHeader(ze, getName(ze), entryMetaData, needsZip64Extra);
    }

    /**
     * Writes the central file header entry.
     * @param ze the entry to write
     * @param name The encoded name
     * @param entryMetaData meta data for this file
     * @throws IOException on error
     */
    private byte[] createCentralFileHeader(final ZipArchiveEntry ze, final ByteBuffer name,
                                           final EntryMetaData entryMetaData,
                                           final boolean needsZip64Extra) throws IOException {
        final byte[] extra = ze.getCentralDirectoryExtra();

        // file comment length
        String comm = ze.getComment();
        if (comm == null) {
            comm = "";
        }

        final ByteBuffer commentB = getEntryEncoding(ze).encode(comm);
        final int nameLen = name.limit() - name.position();
        final int commentLen = commentB.limit() - commentB.position();
        final int len= CFH_FILENAME_OFFSET + nameLen + extra.length + commentLen;
        final byte[] buf = new byte[len];

        System.arraycopy(CFH_SIG,  0, buf, CFH_SIG_OFFSET, WORD);

        // version made by
        // CheckStyle:MagicNumber OFF
        putShort((ze.getPlatform() << 8) | (!hasUsedZip64 ? DATA_DESCRIPTOR_MIN_VERSION : ZIP64_MIN_VERSION),
                buf, CFH_VERSION_MADE_BY_OFFSET);

        final int zipMethod = ze.getMethod();
        final boolean encodable = zipEncoding.canEncode(ze.getName());
        putShort(versionNeededToExtract(zipMethod, needsZip64Extra, entryMetaData.usesDataDescriptor),
            buf, CFH_VERSION_NEEDED_OFFSET);
        getGeneralPurposeBits(!encodable && fallbackToUTF8, entryMetaData.usesDataDescriptor).encode(buf, CFH_GPB_OFFSET);

        // compression method
        putShort(zipMethod, buf, CFH_METHOD_OFFSET);


        // last mod. time and date
        ZipUtil.toDosTime(calendarInstance, ze.getTime(), buf, CFH_TIME_OFFSET);

        // CRC
        // compressed length
        // uncompressed length
        putLong(ze.getCrc(), buf, CFH_CRC_OFFSET);
        if (ze.getCompressedSize() >= ZIP64_MAGIC
                || ze.getSize() >= ZIP64_MAGIC
                || zip64Mode == Zip64Mode.Always) {
            ZipLong.ZIP64_MAGIC.putLong(buf, CFH_COMPRESSED_SIZE_OFFSET);
            ZipLong.ZIP64_MAGIC.putLong(buf, CFH_ORIGINAL_SIZE_OFFSET);
        } else {
            putLong(ze.getCompressedSize(), buf, CFH_COMPRESSED_SIZE_OFFSET);
            putLong(ze.getSize(), buf, CFH_ORIGINAL_SIZE_OFFSET);
        }

        putShort(nameLen, buf, CFH_FILENAME_LENGTH_OFFSET);

        // extra field length
        putShort(extra.length, buf, CFH_EXTRA_LENGTH_OFFSET);

        putShort(commentLen, buf, CFH_COMMENT_LENGTH_OFFSET);

        // disk number start
        System.arraycopy(ZERO, 0, buf, CFH_DISK_NUMBER_OFFSET, SHORT);

        // internal file attributes
        putShort(ze.getInternalAttributes(), buf, CFH_INTERNAL_ATTRIBUTES_OFFSET);

        // external file attributes
        putLong(ze.getExternalAttributes(), buf, CFH_EXTERNAL_ATTRIBUTES_OFFSET);

        // relative offset of LFH
        if (entryMetaData.offset >= ZIP64_MAGIC || zip64Mode == Zip64Mode.Always) {
            putLong(ZIP64_MAGIC, buf, CFH_LFH_OFFSET);
        } else {
            putLong(Math.min(entryMetaData.offset, ZIP64_MAGIC), buf, CFH_LFH_OFFSET);
        }

        // file name
        System.arraycopy(name.array(), name.arrayOffset(), buf, CFH_FILENAME_OFFSET, nameLen);

        final int extraStart = CFH_FILENAME_OFFSET + nameLen;
        System.arraycopy(extra, 0, buf, extraStart, extra.length);

        final int commentStart = extraStart + extra.length;

        // file comment
        System.arraycopy(commentB.array(), commentB.arrayOffset(), buf, commentStart, commentLen);
        return buf;
    }

    /**
     * If the entry needs Zip64 extra information inside the central
     * directory then configure its data.
     */
    private void handleZip64Extra(final ZipArchiveEntry ze, final long lfhOffset,
                                  final boolean needsZip64Extra) {
        if (needsZip64Extra) {
            final Zip64ExtendedInformationExtraField z64 = getZip64Extra(ze);
            if (ze.getCompressedSize() >= ZIP64_MAGIC
                || ze.getSize() >= ZIP64_MAGIC
                || zip64Mode == Zip64Mode.Always) {
                z64.setCompressedSize(new ZipEightByteInteger(ze.getCompressedSize()));
                z64.setSize(new ZipEightByteInteger(ze.getSize()));
            } else {
                // reset value that may have been set for LFH
                z64.setCompressedSize(null);
                z64.setSize(null);
            }
            if (lfhOffset >= ZIP64_MAGIC || zip64Mode == Zip64Mode.Always) {
                z64.setRelativeHeaderOffset(new ZipEightByteInteger(lfhOffset));
            }
            ze.setExtra();
        }
    }

    /**
     * Writes the &quot;End of central dir record&quot;.
     * @throws IOException on error
     * @throws Zip64RequiredException if the archive's size exceeds 4
     * GByte or there are more than 65535 entries inside the archive
     * and {@link Zip64Mode #setUseZip64} is {@link Zip64Mode#Never}.
     */
    protected void writeCentralDirectoryEnd() throws IOException {
        writeCounted(EOCD_SIG);

        // disk numbers
        writeCounted(ZERO);
        writeCounted(ZERO);

        // number of entries
        final int numberOfEntries = entries.size();
        if (numberOfEntries > ZIP64_MAGIC_SHORT
            && zip64Mode == Zip64Mode.Never) {
            throw new Zip64RequiredException(Zip64RequiredException
                                             .TOO_MANY_ENTRIES_MESSAGE);
        }
        if (cdOffset > ZIP64_MAGIC && zip64Mode == Zip64Mode.Never) {
            throw new Zip64RequiredException(Zip64RequiredException
                                             .ARCHIVE_TOO_BIG_MESSAGE);
        }

        final byte[] num = ZipShort.getBytes(Math.min(numberOfEntries,
                                                ZIP64_MAGIC_SHORT));
        writeCounted(num);
        writeCounted(num);

        // length and location of CD
        writeCounted(ZipLong.getBytes(Math.min(cdLength, ZIP64_MAGIC)));
        writeCounted(ZipLong.getBytes(Math.min(cdOffset, ZIP64_MAGIC)));

        // ZIP file comment
        final ByteBuffer data = this.zipEncoding.encode(comment);
        final int dataLen = data.limit() - data.position();
        writeCounted(ZipShort.getBytes(dataLen));
        streamCompressor.writeCounted(data.array(), data.arrayOffset(), dataLen);
    }

    /**
     * Writes the &quot;ZIP64 End of central dir record&quot; and
     * &quot;ZIP64 End of central dir locator&quot;.
     * @throws IOException on error
     * @since 1.3
     */
    protected void writeZip64CentralDirectory() throws IOException {
        if (zip64Mode == Zip64Mode.Never) {
            return;
        }

        if (!hasUsedZip64
            && (cdOffset >= ZIP64_MAGIC || cdLength >= ZIP64_MAGIC
                || entries.size() >= ZIP64_MAGIC_SHORT)) {
            // actually "will use"
            hasUsedZip64 = true;
        }

        if (!hasUsedZip64) {
            return;
        }

        final long offset = streamCompressor.getTotalBytesWritten();

        writeOut(ZIP64_EOCD_SIG);
        // size, we don't have any variable length as we don't support
        // the extensible data sector, yet
        writeOut(ZipEightByteInteger
                 .getBytes(SHORT   /* version made by */
                           + SHORT /* version needed to extract */
                           + WORD  /* disk number */
                           + WORD  /* disk with central directory */
                           + DWORD /* number of entries in CD on this disk */
                           + DWORD /* total number of entries */
                           + DWORD /* size of CD */
                           + (long) DWORD /* offset of CD */
                           ));

        // version made by and version needed to extract
        writeOut(ZipShort.getBytes(ZIP64_MIN_VERSION));
        writeOut(ZipShort.getBytes(ZIP64_MIN_VERSION));

        // disk numbers - four bytes this time
        writeOut(LZERO);
        writeOut(LZERO);

        // number of entries
        final byte[] num = ZipEightByteInteger.getBytes(entries.size());
        writeOut(num);
        writeOut(num);

        // length and location of CD
        writeOut(ZipEightByteInteger.getBytes(cdLength));
        writeOut(ZipEightByteInteger.getBytes(cdOffset));

        // no "zip64 extensible data sector" for now

        // and now the "ZIP64 end of central directory locator"
        writeOut(ZIP64_EOCD_LOC_SIG);

        // disk number holding the ZIP64 EOCD record
        writeOut(LZERO);
        // relative offset of ZIP64 EOCD record
        writeOut(ZipEightByteInteger.getBytes(offset));
        // total number of disks
        writeOut(ONE);
    }

    /**
     * Write bytes to output or random access file.
     * @param data the byte array to write
     * @throws IOException on error
     */
    protected final void writeOut(final byte[] data) throws IOException {
        streamCompressor.writeOut(data, 0, data.length);
    }


    /**
     * Write bytes to output or random access file.
     * @param data the byte array to write
     * @param offset the start position to write from
     * @param length the number of bytes to write
     * @throws IOException on error
     */
    protected final void writeOut(final byte[] data, final int offset, final int length)
            throws IOException {
        streamCompressor.writeOut(data, offset, length);
    }


    private GeneralPurposeBit getGeneralPurposeBits(final boolean utfFallback, boolean usesDataDescriptor) {
        final GeneralPurposeBit b = new GeneralPurposeBit();
        b.useUTF8ForNames(useUTF8Flag || utfFallback);
        if (usesDataDescriptor) {
            b.useDataDescriptor(true);
        }
        return b;
    }

    private int versionNeededToExtract(final int zipMethod, final boolean zip64, final boolean usedDataDescriptor) {
        if (zip64) {
            return ZIP64_MIN_VERSION;
        }
        if (usedDataDescriptor) {
            return DATA_DESCRIPTOR_MIN_VERSION;
        }
        return versionNeededToExtractMethod(zipMethod);
    }

    private boolean usesDataDescriptor(final int zipMethod, boolean phased) {
        return !phased && zipMethod == DEFLATED && channel == null;
    }

    private int versionNeededToExtractMethod(int zipMethod) {
        return zipMethod == DEFLATED ? DEFLATE_MIN_VERSION : INITIAL_VERSION;
    }

    /**
     * Creates a new zip entry taking some information from the given
     * file and using the provided name.
     *
     * <p>The name will be adjusted to end with a forward slash "/" if
     * the file is a directory.  If the file is not a directory a
     * potential trailing forward slash will be stripped from the
     * entry name.</p>
     *
     * <p>Must not be used if the stream has already been closed.</p>
     */
    @Override
    public ArchiveEntry createArchiveEntry(final File inputFile, final String entryName)
        throws IOException {
        if (finished) {
            throw new IOException("Stream has already been finished");
        }
        return new ZipArchiveEntry(inputFile, entryName);
    }

    /**
     * Get the existing ZIP64 extended information extra field or
     * create a new one and add it to the entry.
     *
     * @since 1.3
     */
    private Zip64ExtendedInformationExtraField
        getZip64Extra(final ZipArchiveEntry ze) {
        if (entry != null) {
            entry.causedUseOfZip64 = !hasUsedZip64;
        }
        hasUsedZip64 = true;
        Zip64ExtendedInformationExtraField z64 =
            (Zip64ExtendedInformationExtraField)
            ze.getExtraField(Zip64ExtendedInformationExtraField
                             .HEADER_ID);
        if (z64 == null) {
            /*
              System.err.println("Adding z64 for " + ze.getName()
              + ", method: " + ze.getMethod()
              + " (" + (ze.getMethod() == STORED) + ")"
              + ", channel: " + (channel != null));
            */
            z64 = new Zip64ExtendedInformationExtraField();
        }

        // even if the field is there already, make sure it is the first one
        ze.addAsFirstExtraField(z64);

        return z64;
    }

    /**
     * Is there a ZIP64 extended information extra field for the
     * entry?
     *
     * @since 1.3
     */
    private boolean hasZip64Extra(final ZipArchiveEntry ze) {
        return ze.getExtraField(Zip64ExtendedInformationExtraField
                                .HEADER_ID)
            != null;
    }

    /**
     * If the mode is AsNeeded and the entry is a compressed entry of
     * unknown size that gets written to a non-seekable stream then
     * change the default to Never.
     *
     * @since 1.3
     */
    private Zip64Mode getEffectiveZip64Mode(final ZipArchiveEntry ze) {
        if (zip64Mode != Zip64Mode.AsNeeded
            || channel != null
            || ze.getMethod() != DEFLATED
            || ze.getSize() != ArchiveEntry.SIZE_UNKNOWN) {
            return zip64Mode;
        }
        return Zip64Mode.Never;
    }

    private ZipEncoding getEntryEncoding(final ZipArchiveEntry ze) {
        final boolean encodable = zipEncoding.canEncode(ze.getName());
        return !encodable && fallbackToUTF8
            ? ZipEncodingHelper.UTF8_ZIP_ENCODING : zipEncoding;
    }

    private ByteBuffer getName(final ZipArchiveEntry ze) throws IOException {
        return getEntryEncoding(ze).encode(ze.getName());
    }

    /**
     * Closes the underlying stream/file without finishing the
     * archive, the result will likely be a corrupt archive.
     *
     * <p>This method only exists to support tests that generate
     * corrupt archives so they can clean up any temporary files.</p>
     */
    void destroy() throws IOException {
        if (channel != null) {
            channel.close();
        }
        if (out != null) {
            out.close();
        }
    }

    /**
     * enum that represents the possible policies for creating Unicode
     * extra fields.
     */
    public static final class UnicodeExtraFieldPolicy {
        /**
         * Always create Unicode extra fields.
         */
        public static final UnicodeExtraFieldPolicy ALWAYS = new UnicodeExtraFieldPolicy("always");
        /**
         * Never create Unicode extra fields.
         */
        public static final UnicodeExtraFieldPolicy NEVER = new UnicodeExtraFieldPolicy("never");
        /**
         * Create Unicode extra fields for filenames that cannot be
         * encoded using the specified encoding.
         */
        public static final UnicodeExtraFieldPolicy NOT_ENCODEABLE =
            new UnicodeExtraFieldPolicy("not encodeable");

        private final String name;
        private UnicodeExtraFieldPolicy(final String n) {
            name = n;
        }
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Structure collecting information for the entry that is
     * currently being written.
     */
    private static final class CurrentEntry {
        private CurrentEntry(final ZipArchiveEntry entry) {
            this.entry = entry;
        }
        /**
         * Current ZIP entry.
         */
        private final ZipArchiveEntry entry;
        /**
         * Offset for CRC entry in the local file header data for the
         * current entry starts here.
         */
        private long localDataStart = 0;
        /**
         * Data for local header data
         */
        private long dataStart = 0;
        /**
         * Number of bytes read for the current entry (can't rely on
         * Deflater#getBytesRead) when using DEFLATED.
         */
        private long bytesRead = 0;
        /**
         * Whether current entry was the first one using ZIP64 features.
         */
        private boolean causedUseOfZip64 = false;
        /**
         * Whether write() has been called at all.
         *
         * <p>In order to create a valid archive {@link
         * #closeArchiveEntry closeArchiveEntry} will write an empty
         * array to get the CRC right if nothing has been written to
         * the stream at all.</p>
         */
        private boolean hasWritten;
    }

    private static final class EntryMetaData {
        private final long offset;
        private final boolean usesDataDescriptor;
        private EntryMetaData(long offset, boolean usesDataDescriptor) {
            this.offset = offset;
            this.usesDataDescriptor = usesDataDescriptor;
        }
    }
}
