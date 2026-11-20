package com.foobnix.ext.chm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads a Microsoft Compiled HTML Help file (.chm): the ITSF header, the PMGL directory chunks
 * that list the files, and the files themselves - stored ones from section 0, compressed ones from
 * the LZX stream of section 1 (see {@link ChmLzx}).
 * <p>
 * The layout follows chmlib (Jed Wing) and the descriptions of the format by Pabs and Matthew
 * Russotto. Names are looked up without regard to case, as Windows' help viewer does: a page that
 * links to {@code Images/Logo.GIF} finds {@code /images/logo.gif}.
 */
public final class ChmFile implements Closeable {

    private static final String CONTENT = "::DataSpace/Storage/MSCompressed/Content";
    private static final String CONTROL = "::DataSpace/Storage/MSCompressed/ControlData";
    private static final String RESET_TABLE = "::DataSpace/Storage/MSCompressed/Transform/"
            + "{7FC28940-9D31-11D0-9B27-00A0C91E9C7C}/InstanceData/ResetTable";

    /** A file never grows past this: a broken directory entry must not ask for gigabytes. */
    private static final long MAX_FILE = 64L * 1024 * 1024;

    /** How many decoded 32 KB blocks are kept, so that files read in order decode each block once. */
    private static final int CACHE_BLOCKS = 32;

    public static final class Entry {
        public final String name;
        final int space;
        final long offset;
        public final long length;

        Entry(final String name, final int space, final long offset, final long length) {
            this.name = name;
            this.space = space;
            this.offset = offset;
            this.length = length;
        }
    }

    private final RandomAccessFile raf;
    private final long length;
    private long dataOffset;

    /** In directory order, which is sorted by name. */
    private final List<Entry> entries = new ArrayList<Entry>();
    private final Map<String, Entry> byName = new LinkedHashMap<String, Entry>();

    // Section 1
    private boolean lzxReady;
    private Entry content;
    private long[] resetTable;
    private long blockLen;
    private long uncompressedLen;
    private long compressedLen;
    private int resetBlockCount;
    private int windowBits;
    private ChmLzx lzx;
    private long lastBlock = -1;
    private final LinkedHashMap<Long, byte[]> blocks = new LinkedHashMap<Long, byte[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<Long, byte[]> eldest) {
            return size() > CACHE_BLOCKS;
        }
    };

    // #SYSTEM
    private String title;
    private String contentsFile;
    private String indexFile;
    private String defaultTopic;
    private int lcid;

    public ChmFile(final File file) throws IOException {
        raf = new RandomAccessFile(file, "r");
        try {
            length = raf.length();
            readHeaders();
            readSystem();
        } catch (IOException e) {
            raf.close();
            throw e;
        } catch (RuntimeException e) {
            raf.close();
            throw new IOException(e);
        }
    }

    /** Whether the file starts as a CHM does. */
    public static boolean isChm(final File file) {
        try {
            final RandomAccessFile f = new RandomAccessFile(file, "r");
            try {
                final byte[] b = new byte[4];
                f.readFully(b);
                return b[0] == 'I' && b[1] == 'T' && b[2] == 'S' && b[3] == 'F';
            } finally {
                f.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }

    // ------------------------------------------------------------------ headers

    private void readHeaders() throws IOException {
        final byte[] itsf = readAt(0, 0x60 > length ? (int) length : 0x60);
        if (itsf.length < 0x58 || !sig(itsf, 0, "ITSF")) {
            throw new IOException("not a CHM file");
        }
        final int version = le32(itsf, 4);
        final long dirOffset = le64(itsf, 0x48);
        final long dirLen = le64(itsf, 0x50);
        if (version >= 3 && le32(itsf, 8) >= 0x60 && itsf.length >= 0x60) {
            dataOffset = le64(itsf, 0x58);
        } else {
            dataOffset = dirOffset + dirLen;
        }
        if (dirOffset <= 0 || dirOffset >= length || dataOffset < 0 || dataOffset > length) {
            throw new IOException("CHM header points outside the file");
        }

        final byte[] itsp = readAt(dirOffset, 0x54);
        if (!sig(itsp, 0, "ITSP")) {
            throw new IOException("CHM has no ITSP directory");
        }
        final int headerLen = le32(itsp, 8);
        final int blockSize = le32(itsp, 0x10);
        final int numBlocks = le32(itsp, 0x2C);
        if (blockSize < 0x20 || blockSize > 1 << 20 || numBlocks < 0 || headerLen < 0) {
            throw new IOException("CHM directory is broken");
        }
        final long chunks = dirOffset + headerLen;
        for (int i = 0; i < numBlocks; i++) {
            final long at = chunks + (long) i * blockSize;
            if (at + blockSize > length) {
                break;
            }
            final byte[] chunk = readAt(at, blockSize);
            if (sig(chunk, 0, "PMGL")) {
                readListingChunk(chunk);
            }
        }
        if (entries.isEmpty()) {
            throw new IOException("CHM directory lists no files");
        }
    }

    private void readListingChunk(final byte[] chunk) {
        final int freeSpace = le32(chunk, 4);
        final int end = chunk.length - freeSpace;
        final int[] pos = {0x14};
        while (pos[0] < end && end <= chunk.length) {
            final long nameLen = encint(chunk, pos, end);
            if (nameLen <= 0 || pos[0] + nameLen > end) {
                break;
            }
            final String name = new String(chunk, pos[0], (int) nameLen, Charset.forName("UTF-8"));
            pos[0] += (int) nameLen;
            final long space = encint(chunk, pos, end);
            final long offset = encint(chunk, pos, end);
            final long len = encint(chunk, pos, end);
            if (space < 0 || offset < 0 || len < 0) {
                break;
            }
            final Entry e = new Entry(name, (int) space, offset, len);
            entries.add(e);
            byName.put(name.toLowerCase(Locale.US), e);
        }
    }

    /** A CHM's variable length number: 7 bits a byte, most significant first, high bit = more. */
    private static long encint(final byte[] b, final int[] pos, final int end) {
        long v = 0;
        for (int n = 0; n < 9; n++) {
            if (pos[0] >= end) {
                return -1;
            }
            final int c = b[pos[0]++] & 0xFF;
            v = (v << 7) | (c & 0x7F);
            if ((c & 0x80) == 0) {
                return v;
            }
        }
        return -1;
    }

    private void readSystem() {
        final Entry system = find("/#SYSTEM");
        byte[] data = null;
        if (system != null) {
            try {
                data = read(system);
            } catch (IOException e) {
                data = null;
            }
        }
        final List<byte[]> strings = new ArrayList<byte[]>();
        final int[] codes = new int[64];
        int count = 0;
        if (data != null && data.length > 4) {
            int p = 4;
            while (p + 4 <= data.length && count < codes.length) {
                final int code = le16(data, p);
                final int len = le16(data, p + 2);
                p += 4;
                if (p + len > data.length) {
                    break;
                }
                final byte[] value = new byte[len];
                System.arraycopy(data, p, value, 0, len);
                p += len;
                if (code == 4 && len >= 4) {
                    lcid = le32(value, 0);
                }
                codes[count++] = code;
                strings.add(value);
            }
        }
        final Charset charset = ChmText.charsetOf(lcid);
        for (int i = 0; i < count; i++) {
            final int code = codes[i];
            if (code != 0 && code != 1 && code != 2 && code != 3) {
                continue;
            }
            final String value = cString(strings.get(i), charset);
            if (value.length() == 0) {
                continue;
            }
            if (code == 0) {
                contentsFile = value;
            } else if (code == 1) {
                indexFile = value;
            } else if (code == 2) {
                defaultTopic = value;
            } else {
                title = value;
            }
        }
    }

    private static String cString(final byte[] b, final Charset charset) {
        int n = 0;
        while (n < b.length && b[n] != 0) {
            n++;
        }
        return new String(b, 0, n, charset).trim();
    }

    // ------------------------------------------------------------------ what is in it

    public List<Entry> getEntries() {
        return entries;
    }

    /** The entry with this name, ignoring case; a name without its leading '/' is found too. */
    public Entry find(String name) {
        if (name == null) {
            return null;
        }
        Entry e = byName.get(name.toLowerCase(Locale.US));
        if (e == null && !name.startsWith("/")) {
            e = byName.get(("/" + name).toLowerCase(Locale.US));
        }
        return e;
    }

    public String getTitle() {
        return title;
    }

    /** The .hhc the #SYSTEM file names, or the first one in the directory. */
    public String getContentsFile() {
        if (contentsFile != null && find(contentsFile) != null) {
            return find(contentsFile).name;
        }
        for (Entry e : entries) {
            if (e.name.toLowerCase(Locale.US).endsWith(".hhc") && e.name.startsWith("/")) {
                return e.name;
            }
        }
        return null;
    }

    public String getIndexFile() {
        return indexFile;
    }

    public String getDefaultTopic() {
        if (defaultTopic != null) {
            String topic = defaultTopic;
            final int hash = topic.indexOf('#');
            if (hash >= 0) {
                topic = topic.substring(0, hash);
            }
            final Entry e = find(topic);
            if (e != null) {
                return e.name;
            }
        }
        return null;
    }

    /** The Windows language id of the book, which is also what tells its code page. */
    public int getLcid() {
        return lcid;
    }

    // ------------------------------------------------------------------ reading

    public byte[] read(final Entry e) throws IOException {
        if (e.length > MAX_FILE) {
            throw new IOException("CHM file too large: " + e.name + " " + e.length);
        }
        if (e.length == 0) {
            return new byte[0];
        }
        if (e.space == 0) {
            final long at = dataOffset + e.offset;
            if (at < 0 || at + e.length > length) {
                throw new IOException("CHM file outside the archive: " + e.name);
            }
            return readAt(at, (int) e.length);
        }
        if (e.space == 1) {
            return readCompressed(e.offset, (int) e.length);
        }
        throw new IOException("CHM section " + e.space);
    }

    private void initLzx() throws IOException {
        if (lzxReady) {
            return;
        }
        content = find(CONTENT);
        final Entry control = find(CONTROL);
        final Entry reset = find(RESET_TABLE);
        if (content == null || control == null || reset == null
                || content.space != 0 || control.space != 0 || reset.space != 0) {
            throw new IOException("CHM has no LZX section");
        }
        final byte[] c = read(control);
        if (c.length < 0x18 || !sig(c, 4, "LZXC")) {
            throw new IOException("CHM LZX control data is broken");
        }
        final int version = le32(c, 8);
        long resetInterval = le32(c, 0x0C) & 0xFFFFFFFFL;
        long windowSize = le32(c, 0x10) & 0xFFFFFFFFL;
        if (version == 2) {
            resetInterval *= 0x8000;
            windowSize *= 0x8000;
        }
        if (windowSize == 0 || resetInterval == 0) {
            throw new IOException("CHM LZX window " + windowSize + " reset " + resetInterval);
        }
        windowBits = 0;
        while ((1L << windowBits) < windowSize && windowBits < 32) {
            windowBits++;
        }
        if ((1L << windowBits) != windowSize || windowBits < 15 || windowBits > 21) {
            throw new IOException("CHM LZX window size " + windowSize);
        }
        if (resetInterval % (windowSize / 2) != 0) {
            throw new IOException("CHM LZX reset interval " + resetInterval);
        }

        final byte[] r = read(reset);
        if (r.length < 0x28) {
            throw new IOException("CHM reset table is broken");
        }
        final int blockCount = le32(r, 4);
        final int tableOffset = le32(r, 0x0C);
        uncompressedLen = le64(r, 0x10);
        compressedLen = le64(r, 0x18);
        blockLen = le64(r, 0x20);
        if (blockLen <= 0 || blockLen > 1 << windowBits || blockCount < 0
                || tableOffset < 0 || tableOffset + 8L * blockCount > r.length) {
            throw new IOException("CHM reset table is broken");
        }
        // The stream is reset every resetInterval bytes of output, as libmspack and 7-Zip read
        // it (chmlib's half-window arithmetic agrees for the usual 64 KB window)
        resetBlockCount = (int) Math.max(1, resetInterval / blockLen);
        resetTable = new long[blockCount];
        for (int i = 0; i < blockCount; i++) {
            resetTable[i] = le64(r, tableOffset + 8 * i);
        }
        lzx = new ChmLzx(windowBits);
        lzxReady = true;
    }

    private byte[] readCompressed(final long offset, final int len) throws IOException {
        initLzx();
        if (offset + len > uncompressedLen) {
            throw new IOException("CHM file past the end of the LZX section");
        }
        final byte[] out = new byte[len];
        int done = 0;
        while (done < len) {
            final long at = offset + done;
            final long block = at / blockLen;
            final byte[] data = block(block);
            final int inBlock = (int) (at % blockLen);
            final int n = Math.min(len - done, data.length - inBlock);
            if (n <= 0) {
                throw new IOException("CHM block " + block + " is short");
            }
            System.arraycopy(data, inBlock, out, done, n);
            done += n;
        }
        return out;
    }

    private byte[] block(final long block) throws IOException {
        final byte[] cached = blocks.get(block);
        if (cached != null) {
            return cached;
        }
        if (block >= resetTable.length) {
            throw new IOException("CHM block " + block + " of " + resetTable.length);
        }
        final long start = block - block % resetBlockCount;
        long from = start;
        if (lastBlock >= start && lastBlock < block) {
            from = lastBlock + 1;
        }
        byte[] data = null;
        for (long b = from; b <= block; b++) {
            if (b % resetBlockCount == 0) {
                lzx.reset();
            }
            data = decode(b);
            lastBlock = b;
            blocks.put(b, data);
        }
        return data;
    }

    private byte[] decode(final long block) throws IOException {
        final long cmpStart = resetTable[(int) block];
        final long cmpEnd = block + 1 < resetTable.length ? resetTable[(int) block + 1] : compressedLen;
        final long cmpLen = cmpEnd - cmpStart;
        if (cmpStart < 0 || cmpLen < 0 || cmpEnd > content.length || cmpLen > 2 * blockLen + 6144) {
            lastBlock = -1;
            throw new IOException("CHM compressed block " + block + " is broken");
        }
        final long outLen = Math.min(blockLen, uncompressedLen - block * blockLen);
        if (outLen <= 0) {
            throw new IOException("CHM block " + block + " is past the end");
        }
        final byte[] src = readAt(dataOffset + content.offset + cmpStart, (int) cmpLen);
        final byte[] out = new byte[(int) outLen];
        try {
            lzx.decompress(src, 0, src.length, out, 0, (int) outLen);
        } catch (IOException e) {
            lastBlock = -1;
            throw new IOException("CHM block " + block + ": " + e.getMessage(), e);
        }
        return out;
    }

    // ------------------------------------------------------------------ bytes

    private byte[] readAt(final long at, final int len) throws IOException {
        if (at < 0 || len < 0 || at + len > length) {
            throw new IOException("CHM read outside the file");
        }
        final byte[] b = new byte[len];
        raf.seek(at);
        raf.readFully(b);
        return b;
    }

    private static boolean sig(final byte[] b, final int at, final String s) {
        if (b.length < at + s.length()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (b[at + i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    static int le16(final byte[] b, final int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }

    static int le32(final byte[] b, final int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8) | ((b[p + 2] & 0xFF) << 16) | ((b[p + 3] & 0xFF) << 24);
    }

    static long le64(final byte[] b, final int p) {
        return (le32(b, p) & 0xFFFFFFFFL) | ((long) le32(b, p + 4) << 32);
    }
}
