package com.foobnix.ext.lit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A Microsoft Reader (.lit) book: an ITOLITLS container (a cousin of CHM) of LZX-compressed and,
 * for "sealed" books, DES-encrypted sections holding binary HTML, CSS and images.
 * <p>
 * After calibre's LitFile (ebooks/lit/reader.py). Books sealed to an owner (DRM level 5) cannot be
 * read; levels 1 and 3 carry their own key.
 */
public final class LitFile {

    static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset UTF16LE = Charset.forName("UTF-16LE");

    private static final String DESENCRYPT_GUID = "{67F6E4A2-60BF-11D3-8540-00C04F58C3CF}";
    private static final String LZXCOMPRESS_GUID = "{0A9007C6-4076-11D3-8789-0000F8105754}";

    /** The book is sealed to its owner: Reader's DRM level 5. */
    public static final class DrmException extends IOException {
        DrmException() {
            super("DRM-protected LIT book");
        }
    }

    static final class Entry {
        final String name;
        final int section;
        final long offset;
        final long size;

        Entry(String name, int section, long offset, long size) {
            this.name = name;
            this.section = section;
            this.offset = offset;
            this.size = size;
        }
    }

    /** A file of the book: {@link #state} is spine, not spine, css or images. */
    public static final class Item {
        public final String internal;
        public final String original;
        public final String mimeType;
        public final String state;
        public String path;

        Item(String original, String internal, String mimeType, String state) {
            this.original = original;
            this.internal = internal;
            this.mimeType = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
            this.state = state;
            String p = original.replace('\\', '/');
            if (p.length() >= 3 && p.substring(1, 3).equals(":/")) {
                p = p.substring(2);
            }
            p = normpath(p);
            while (p.startsWith("../")) {
                p = p.substring(3);
            }
            this.path = p;
        }

        /** Binary HTML, decoded by {@link #getHtml}; the other files are kept as they are. */
        public boolean isHtml() {
            return state.contains("spine");
        }
    }

    private final byte[] data;
    private long contentOffset = -1;
    private final Map<String, Entry> entries = new HashMap<String, Entry>();
    private String[] sectionNames = new String[0];
    private byte[][] sectionData = new byte[0][];
    private final Map<String, Item> manifest = new LinkedHashMap<String, Item>();
    private int drmLevel;
    private byte[] bookKey;
    /** Compressed windows that did not decode: their bytes are left as zeros. */
    int damagedWindows;
    String lastDamage;

    public LitFile(File file) throws IOException {
        this(readAll(file));
    }

    public LitFile(byte[] data) throws IOException {
        this.data = data;
        try {
            if (data.length < 40 || !"ITOLITLS".equals(new String(data, 0, 8, UTF8))) {
                throw new IOException("Not a valid LIT file");
            }
            if (u32(data, 8) != 1) {
                throw new IOException("Unknown LIT version " + u32(data, 8));
            }
            readHeaders();
            readSectionNames();
            readManifest();
            readDrm();
        } catch (IndexOutOfBoundsException e) {
            throw new IOException("Truncated LIT file", e);
        }
    }

    static byte[] readAll(File file) throws IOException {
        final InputStream in = new FileInputStream(file);
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream((int) Math.min(file.length(), Integer.MAX_VALUE - 8));
            final byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    // ------------------------------------------------------------------ binary helpers

    static long u32(byte[] b, int p) {
        return (b[p] & 0xFFL) | ((b[p + 1] & 0xFFL) << 8) | ((b[p + 2] & 0xFFL) << 16) | ((b[p + 3] & 0xFFL) << 24);
    }

    static int int32(byte[] b, int p) {
        return (int) u32(b, p);
    }

    static int u16(byte[] b, int p) {
        return (b[p] & 0xFF) | ((b[p + 1] & 0xFF) << 8);
    }

    private static String msguid(byte[] b, int p) {
        return String.format(Locale.US, "{%08X-%04X-%04X-%02X%02X-%02X%02X%02X%02X%02X%02X}",
                u32(b, p), u16(b, p + 4), u16(b, p + 6),
                b[p + 8] & 0xFF, b[p + 9] & 0xFF, b[p + 10] & 0xFF, b[p + 11] & 0xFF,
                b[p + 12] & 0xFF, b[p + 13] & 0xFF, b[p + 14] & 0xFF, b[p + 15] & 0xFF);
    }

    private static boolean tag(byte[] b, int p, String tag) {
        for (int i = 0; i < tag.length(); i++) {
            if (p + i >= b.length || b[p + i] != tag.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static byte[] slice(byte[] b, long from, long to) {
        final int f = (int) Math.max(0, Math.min(from, b.length));
        final int t = (int) Math.max(f, Math.min(to, b.length));
        final byte[] r = new byte[t - f];
        System.arraycopy(b, f, r, 0, r.length);
        return r;
    }

    // ------------------------------------------------------------------ container

    private void readHeaders() throws IOException {
        final int hdrLen = int32(data, 12);
        final int numPieces = int32(data, 16);
        final int secHdrLen = int32(data, 20);

        // the secondary header: CAOL and ITSF blocks
        final int secStart = hdrLen + numPieces * 16;
        final byte[] sec = slice(data, secStart, (long) secStart + secHdrLen);
        int offset = int32(sec, 4);
        while (offset >= 0 && offset + 8 <= sec.length) {
            final long blockVer = u32(sec, offset + 4);
            if (tag(sec, offset, "CAOL")) {
                if (blockVer != 2) {
                    throw new IOException("Unknown CAOL block format " + blockVer);
                }
                offset += 48;
            } else if (tag(sec, offset, "ITSF")) {
                if (blockVer != 4) {
                    throw new IOException("Unknown ITSF block format " + blockVer);
                }
                if (u32(sec, offset + 20) != 0) {
                    throw new IOException("This file has a 64bit content offset");
                }
                contentOffset = u32(sec, offset + 16);
                offset += 48;
            } else {
                break;
            }
        }
        if (contentOffset < 0) {
            throw new IOException("Could not figure out the content offset");
        }

        // piece 1 is the directory
        if (numPieces < 2) {
            throw new IOException("No directory in LIT file");
        }
        final int p = hdrLen + 16;
        if (u32(data, p + 4) != 0 || u32(data, p + 12) != 0) {
            throw new IOException("Directory piece has 64bit value");
        }
        readDirectory(slice(data, u32(data, p), u32(data, p) + int32(data, p + 8)));
    }

    private void readDirectory(byte[] piece) throws IOException {
        if (!tag(piece, 0, "IFCM")) {
            throw new IOException("Header piece #1 is not main directory");
        }
        final int chunkSize = int32(piece, 8);
        final int numChunks = int32(piece, 24);
        if (chunkSize <= 48 || 32L + (long) numChunks * chunkSize > piece.length) {
            throw new IOException("IFCM header has incorrect length");
        }
        for (int i = 0; i < numChunks; i++) {
            final int chunk = 32 + i * chunkSize;
            if (!tag(piece, chunk, "AOLL")) {
                continue;
            }
            int remaining = int32(piece, chunk + 4);
            if (remaining >= chunkSize) {
                throw new IOException("AOLL remaining count is negative");
            }
            remaining = chunkSize - (remaining + 48);
            int entriesCount = u16(piece, chunk + chunkSize - 2);
            if (entriesCount == 0) {
                entriesCount = 0xFFFF;
            }
            int pos = chunk + 48;
            for (int j = 0; j < entriesCount && remaining > 0; j++) {
                int[] r = {pos, remaining};
                final long nameLen = encint(piece, r);
                pos = r[0];
                remaining = r[1];
                if (nameLen > remaining - 3) {
                    throw new IOException("Read past end of directory chunk");
                }
                final String name = new String(piece, pos, (int) nameLen, UTF8);
                pos += (int) nameLen;
                remaining -= (int) nameLen;
                r = new int[]{pos, remaining};
                final long section = encint(piece, r);
                final long off = encint(piece, r);
                final long size = encint(piece, r);
                pos = r[0];
                remaining = r[1];
                entries.put(name, new Entry(name, (int) section, off, size));
            }
        }
    }

    /** A 7-bit variable length integer; {@code r} is {position, bytes remaining}, both advanced. */
    private static long encint(byte[] b, int[] r) {
        long val = 0;
        while (r[1] > 0) {
            final int c = b[r[0]++] & 0xFF;
            r[1]--;
            val = (val << 7) | (c & 0x7F);
            if ((c & 0x80) == 0) {
                break;
            }
        }
        return val;
    }

    private void readSectionNames() throws IOException {
        if (!entries.containsKey("::DataSpace/NameList")) {
            throw new IOException("LIT file does not have a valid NameList");
        }
        final byte[] raw = getFile("::DataSpace/NameList");
        if (raw.length < 4) {
            throw new IOException("Invalid NameList section");
        }
        final int num = u16(raw, 2);
        sectionNames = new String[num];
        sectionData = new byte[num][];
        int pos = 4;
        for (int s = 0; s < num; s++) {
            int size = u16(raw, pos);
            pos += 2;
            size = size * 2 + 2;
            if (pos + size > raw.length) {
                throw new IOException("Invalid NameList section");
            }
            String name = new String(raw, pos, size, UTF16LE);
            int z = name.length();
            while (z > 0 && name.charAt(z - 1) == 0) {
                z--;
            }
            sectionNames[s] = name.substring(0, z);
            pos += size;
        }
    }

    byte[] getFile(String name) throws IOException {
        final Entry entry = entries.get(name);
        if (entry == null) {
            throw new IOException("No " + name + " in LIT file");
        }
        if (entry.section == 0) {
            return slice(data, contentOffset + entry.offset, contentOffset + entry.offset + entry.size);
        }
        final byte[] section = getSection(entry.section);
        return slice(section, entry.offset, entry.offset + entry.size);
    }

    boolean hasFile(String name) {
        return entries.containsKey(name);
    }

    private byte[] getSection(int section) throws IOException {
        if (section < 0 || section >= sectionData.length) {
            throw new IOException("No section " + section);
        }
        if (sectionData[section] == null) {
            sectionData[section] = getSectionUncached(section);
        }
        return sectionData[section];
    }

    private byte[] getSectionUncached(int section) throws IOException {
        final String name = sectionNames[section];
        final String path = "::DataSpace/Storage/" + name;
        final byte[] transform = getFile(path + "/Transform/List");
        byte[] content = getFile(path + "/Content");
        final byte[] control = getFile(path + "/ControlData");
        int t = 0;
        int c = 0;
        while (transform.length - t >= 16) {
            final int csize = (int32(control, c) + 1) * 4;
            if (csize > control.length - c || csize <= 0) {
                throw new IOException("ControlData is too short");
            }
            final String guid = msguid(transform, t);
            if (DESENCRYPT_GUID.equals(guid)) {
                content = decrypt(content);
            } else if (LZXCOMPRESS_GUID.equals(guid)) {
                final byte[] resetTable = getFile(path + "/Transform/" + LZXCOMPRESS_GUID + "/InstanceData/ResetTable");
                content = decompress(content, slice(control, c, control.length), resetTable);
            } else {
                throw new IOException("Unrecognized transform: " + guid);
            }
            c += csize;
            t += 16;
        }
        return content;
    }

    private byte[] decrypt(byte[] content) throws IOException {
        if (bookKey == null) {
            throw new IOException("Encrypted section without a book key");
        }
        return new LitCrypto.Des(bookKey).crypt(content, 0, content.length);
    }

    private byte[] decompress(byte[] content, byte[] control, byte[] resetTable) throws IOException {
        if (control.length < 32 || !tag(control, 4, "LZXC")) {
            throw new IOException("Invalid ControlData tag value");
        }
        if (resetTable.length < 40) {
            throw new IOException("Reset table is too short");
        }
        if (u32(resetTable, 20) != 0) {
            throw new IOException("Reset table has 64bit value for UCLENGTH");
        }

        int windowBits = 14;
        long u = u32(control, 12);
        while (u > 0) {
            u >>= 1;
            windowBits++;
        }
        if (windowBits < 15 || windowBits > 21) {
            throw new IOException("Invalid window in ControlData");
        }
        final LzxDecoder lzx = new LzxDecoder(windowBits);

        int ofsEntry = int32(resetTable, 12) + 8;
        final int uclength = int32(resetTable, 16);
        if (uclength < 0) {
            throw new IOException("Section too large");
        }
        final int interval = int32(resetTable, 32);
        long accum = interval;
        int bytesRemaining = uclength;
        final int windowBytes = 1 << windowBits;
        final byte[] result = new byte[uclength];
        int at = 0;
        int base = 0;

        while (ofsEntry + 8 <= resetTable.length) {
            if (accum >= windowBytes) {
                accum = 0;
                final int size = int32(resetTable, ofsEntry);
                if (int32(resetTable, ofsEntry + 4) != 0) {
                    throw new IOException("Reset table entry greater than 32 bits");
                }
                if (bytesRemaining >= windowBytes) {
                    chunk(lzx, content, base, size, result, at, windowBytes);
                    at += windowBytes;
                    bytesRemaining -= windowBytes;
                    base = size;
                }
            }
            accum += interval;
            ofsEntry += 8;
        }
        if (bytesRemaining > 0 && bytesRemaining < windowBytes) {
            chunk(lzx, content, base, content.length, result, at, bytesRemaining);
            bytesRemaining = 0;
        }
        if (bytesRemaining > 0) {
            throw new IOException("Failed to completely decompress section");
        }
        return result;
    }

    /** One window of a section; a damaged one is left as zeros, as calibre skips it. */
    private void chunk(LzxDecoder lzx, byte[] content, int from, int to, byte[] out, int at, int len) {
        from = Math.max(0, Math.min(from, content.length));
        to = Math.max(from, Math.min(to, content.length));
        try {
            lzx.decompress(content, from, to - from, out, at, len);
        } catch (LzxDecoder.LzxException e) {
            // keep what is readable
            damagedWindows++;
            lastDamage = e.getMessage() + " at " + at + " (" + from + ".." + to + ")";
        }
    }

    // ------------------------------------------------------------------ manifest

    private void readManifest() throws IOException {
        if (!entries.containsKey("/manifest")) {
            throw new IOException("LIT file does not have a valid manifest");
        }
        final byte[] raw = getFile("/manifest");
        final String[] states = {"spine", "not spine", "css", "images"};
        int pos = 0;
        while (pos < raw.length) {
            final int slen = raw[pos++] & 0xFF;
            if (slen == 0) {
                break;
            }
            pos += slen; // root
            if (pos >= raw.length) {
                throw new IOException("Truncated manifest");
            }
            for (String state : states) {
                final int numFiles = int32(raw, pos);
                pos += 4;
                for (int i = 0; i < numFiles; i++) {
                    if (raw.length - pos < 5) {
                        throw new IOException("Truncated manifest");
                    }
                    pos += 4; // offset
                    final int[] p = {pos};
                    final String internal = sizedUtf8(raw, p, false);
                    String original = sizedUtf8(raw, p, false);
                    final String mime = sizedUtf8(raw, p, true);
                    pos = p[0];
                    try {
                        original = URLDecoder.decode(original.replace("+", "%2B"), "UTF-8");
                    } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                        // keep it as it is
                    }
                    manifest.put(internal, new Item(original, internal, mime, state));
                }
            }
        }

        final List<Item> items = new ArrayList<Item>(manifest.values());
        // remove the directories every path shares
        if (items.size() > 1) {
            String shared = items.get(0).path;
            for (int i = 1; i < items.size() && shared != null; i++) {
                final String path = items.get(i).path;
                while (shared != null && !shared.isEmpty() && !path.startsWith(shared)) {
                    final int slash = shared.length() - 3 >= 0 ? shared.lastIndexOf('/', shared.length() - 3) : -1;
                    shared = slash < 0 ? null : shared.substring(0, slash + 1);
                }
                if (shared == null || shared.isEmpty()) {
                    shared = null;
                }
            }
            if (shared != null) {
                for (Item item : items) {
                    item.path = item.path.substring(shared.length());
                }
            }
        }
        for (Item item : items) {
            if (item.path.startsWith("/")) {
                item.path = item.path.substring(item.path.lastIndexOf('/') + 1);
            }
            if (item.path.isEmpty()) {
                item.path = item.internal;
            }
        }
    }

    private static int utf8Char(byte[] b, int[] p) throws IOException {
        int c = b[p[0]] & 0xFF;
        int mask = 0x80;
        int size = 1;
        if ((c & mask) != 0) {
            size = 0;
            while ((c & mask) != 0) {
                mask >>= 1;
                size++;
            }
            if (mask <= 1 || mask == 0x40 || p[0] + size > b.length) {
                throw new IOException("Invalid UTF8 character");
            }
            c &= mask - 1;
            for (int i = 1; i < size; i++) {
                c = (c << 6) | (b[p[0] + i] & 0x3F);
            }
        }
        p[0] += size;
        return c;
    }

    private static String sizedUtf8(byte[] b, int[] p, boolean zpad) throws IOException {
        final int len = utf8Char(b, p);
        final StringBuilder s = new StringBuilder();
        for (int i = 0; i < len; i++) {
            s.appendCodePoint(utf8Char(b, p));
        }
        if (zpad && p[0] < b.length && b[p[0]] == 0) {
            p[0]++;
        }
        return s.toString();
    }

    /** os.path.normpath for / paths. */
    static String normpath(String path) {
        if (path.isEmpty()) {
            return ".";
        }
        final boolean abs = path.startsWith("/");
        final List<String> parts = new ArrayList<String>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..") && !parts.isEmpty() && !parts.get(parts.size() - 1).equals("..")) {
                parts.remove(parts.size() - 1);
            } else if (part.equals("..") && abs) {
                continue;
            } else {
                parts.add(part);
            }
        }
        final StringBuilder s = new StringBuilder(abs ? "/" : "");
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                s.append('/');
            }
            s.append(parts.get(i));
        }
        return s.length() == 0 ? "." : s.toString();
    }

    // ------------------------------------------------------------------ DRM

    private void readDrm() throws IOException {
        if (entries.containsKey("/DRMStorage/Licenses/EUL")) {
            drmLevel = 5;
        } else if (entries.containsKey("/DRMStorage/DRMBookplate")) {
            drmLevel = 3;
        } else if (entries.containsKey("/DRMStorage/DRMSealed")) {
            drmLevel = 1;
        } else {
            return;
        }
        if (drmLevel == 5) {
            throw new DrmException();
        }
        final byte[] sealed = getFile("/DRMStorage/DRMSealed");
        final byte[] key = new LitCrypto.Des(desKey()).crypt(sealed, 0, sealed.length);
        if (key.length < 9 || key[0] != 0) {
            throw new IOException("Unable to decrypt title key");
        }
        bookKey = slice(key, 1, 9);
    }

    private byte[] desKey() throws IOException {
        final List<String> files = new ArrayList<String>();
        Collections.addAll(files, "/meta", "/DRMStorage/DRMSource");
        if (drmLevel == 3) {
            files.add("/DRMStorage/DRMBookplate");
        }
        final ByteArrayOutputStream all = new ByteArrayOutputStream();
        int prepad = 2;
        for (String name : files) {
            final byte[] d = getFile(name);
            final int len = d.length + prepad;
            for (int i = 0; i < prepad; i++) {
                all.write(0);
            }
            prepad = 0;
            all.write(d, 0, d.length);
            final int postpad = 64 - (len % 64);
            if (postpad < 64) {
                for (int i = 0; i < postpad; i++) {
                    all.write(0);
                }
            }
        }
        final byte[] digest = LitCrypto.msSha1(all.toByteArray());
        final byte[] key = new byte[8];
        for (int i = 0; i < digest.length; i++) {
            key[i % 8] ^= digest[i];
        }
        return key;
    }

    public int getDrmLevel() {
        return drmLevel;
    }

    /** How many compressed windows did not decode - a damaged or cut file - and the last error. */
    public String getDamage() {
        return damagedWindows == 0 ? null : damagedWindows + " windows, " + lastDamage;
    }

    // ------------------------------------------------------------------ content

    public List<Item> getItems() {
        return new ArrayList<Item>(manifest.values());
    }

    public Item getItem(String internal) {
        return manifest.get(internal);
    }

    /** The bytes of a CSS, image or other file, as stored. */
    public byte[] getRaw(Item item) throws IOException {
        return getFile("/data/" + item.internal);
    }

    /** A spine document as XHTML text, its links relative to it. */
    public String getHtml(final Item item) throws IOException {
        final byte[] raw = getFile("/data/" + item.internal + "/content");
        final Map<Integer, String>[] atoms = getAtoms(item);
        final String dir = item.path.lastIndexOf('/') >= 0 ? item.path.substring(0, item.path.lastIndexOf('/')) : "";
        return LitUnBinary.decode(raw, LitMaps.HTML, atoms[0], atoms[1], new LitUnBinary.Paths() {
            @Override
            public String relative(String internalId) {
                return relativePath(dir, internalId);
            }
        });
    }

    /** The book's own OPF (OEB 1.0) as text: hrefs are the files' paths. */
    public String getOpf() throws IOException {
        byte[] raw = getFile("/meta");
        final LitUnBinary.Paths paths = new LitUnBinary.Paths() {
            @Override
            public String relative(String internalId) {
                return relativePath("", internalId);
            }
        };
        try {
            return LitUnBinary.decode(raw, LitMaps.OPF, null, null, paths);
        } catch (IOException e) {
            final String s = new String(raw, Charset.forName("ISO-8859-1"));
            final int at = s.indexOf("PENGUIN group");
            if (at < 0) {
                throw e;
            }
            final byte[] fixed = new byte[raw.length + 4];
            System.arraycopy(raw, 0, fixed, 0, at);
            fixed[at] = 0;
            fixed[at + 1] = 1;
            fixed[at + 2] = 0x18;
            fixed[at + 3] = 0;
            System.arraycopy(raw, at, fixed, at + 4, raw.length - at);
            return LitUnBinary.decode(fixed, LitMaps.OPF, null, null, paths);
        }
    }

    private String relativePath(String dir, String internalId) {
        final Item item = manifest.get(internalId);
        if (item == null) {
            return internalId;
        }
        final String target = item.path;
        if (dir.isEmpty()) {
            return target;
        }
        final String[] t = target.split("/");
        final String[] base = dir.split("/");
        int index = 0;
        final int min = Math.min(base.length, t.length);
        while (index < min && base[index].equals(t[index])) {
            index++;
        }
        final StringBuilder s = new StringBuilder();
        for (int i = index; i < base.length; i++) {
            s.append("../");
        }
        for (int i = index; i < t.length; i++) {
            if (i > index) {
                s.append('/');
            }
            s.append(t[i]);
        }
        return s.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, String>[] getAtoms(Item item) throws IOException {
        final Map<Integer, String> tags = new HashMap<Integer, String>();
        final Map<Integer, String> attrs = new HashMap<Integer, String>();
        final String name = "/data/" + item.internal + "/atom";
        if (entries.containsKey(name)) {
            final byte[] d = getFile(name);
            int pos = 0;
            if (d.length >= 4) {
                final long n = u32(d, 0);
                pos = 4;
                for (int i = 1; i <= n; i++) {
                    if (d.length - pos <= 1) {
                        break;
                    }
                    final int size = d[pos++] & 0xFF;
                    if (size == 0 || d.length - pos < size) {
                        break;
                    }
                    tags.put(i, new String(d, pos, size, UTF8));
                    pos += size;
                }
                if (d.length - pos >= 4) {
                    final long m = u32(d, pos);
                    pos += 4;
                    for (int i = 1; i <= m; i++) {
                        if (d.length - pos <= 4) {
                            break;
                        }
                        final long size = u32(d, pos);
                        pos += 4;
                        if (size == 0 || d.length - pos < size) {
                            break;
                        }
                        attrs.put(i, new String(d, pos, (int) size, UTF8));
                        pos += (int) size;
                    }
                }
            }
        }
        return new Map[]{tags, attrs};
    }

    /** A path as an href: characters a URL may not hold are %-escaped, / and # are kept. */
    static String quoteHref(String path) {
        final StringBuilder s = new StringBuilder();
        final byte[] bytes = path.getBytes(UTF8);
        for (byte b : bytes) {
            final int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "-._~/#%:?=&+,;!$'()*@".indexOf(c) >= 0) {
                s.append((char) c);
            } else {
                s.append('%').append(String.format(Locale.US, "%02X", c));
            }
        }
        return s.toString();
    }
}
