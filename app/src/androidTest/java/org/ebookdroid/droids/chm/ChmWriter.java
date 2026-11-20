package org.ebookdroid.droids.chm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a small but genuine CHM for the tests: an ITSF header, one PMGL directory chunk, the
 * stored files of section 0 and the LZX-compressed files of section 1, with the control data and
 * the reset table a help viewer reads them through.
 * <p>
 * The LZX stream is real, if plain: one verbatim block per reset interval whose main tree gives
 * every byte value an 8-bit code and no match a code at all, so the decoder builds its Huffman
 * tables, crosses 32 KB frame boundaries inside a block and resets between intervals as it would
 * on a book Microsoft's compiler wrote - the content is just not any smaller.
 */
final class ChmWriter {

    private static final int FRAME = 0x8000;
    private static final int FRAMES_PER_RESET = 2;
    private static final int WINDOW_BITS = 16;
    private static final int MAIN_ELEMENTS = 256 + (WINDOW_BITS << 1) * 8;

    private static final class File {
        final String name;
        final byte[] data;
        final boolean compressed;
        long offset;

        File(final String name, final byte[] data, final boolean compressed) {
            this.name = name;
            this.data = data;
            this.compressed = compressed;
        }
    }

    private final List<File> files = new ArrayList<File>();

    /** A file of the book, compressed into section 1 or stored in section 0. */
    ChmWriter add(final String name, final byte[] data, final boolean compressed) {
        files.add(new File(name, data, compressed));
        return this;
    }

    ChmWriter add(final String name, final String text, final boolean compressed) {
        return add(name, text.getBytes(Charset.forName("UTF-8")), compressed);
    }

    /** The #SYSTEM file: contents file, default topic, title and language id. */
    static byte[] system(final String hhc, final String topic, final String title, final int lcid) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        le32(out, 3);
        systemString(out, 0, hhc);
        systemString(out, 2, topic);
        systemString(out, 3, title);
        le16(out, 4);
        le16(out, 0x1C);
        le32(out, lcid);
        for (int i = 4; i < 0x1C; i++) {
            out.write(0);
        }
        return out.toByteArray();
    }

    private static void systemString(final ByteArrayOutputStream out, final int code, final String value) {
        final byte[] b = (value + "\0").getBytes(Charset.forName("UTF-8"));
        le16(out, code);
        le16(out, b.length);
        out.write(b, 0, b.length);
    }

    byte[] build() throws IOException {
        // Section 1: the compressed files laid end to end
        final ByteArrayOutputStream plain = new ByteArrayOutputStream();
        for (File f : files) {
            if (f.compressed) {
                f.offset = plain.size();
                plain.write(f.data);
            }
        }
        final byte[] uncompressed = plain.toByteArray();
        final List<Long> resets = new ArrayList<Long>();
        final byte[] content = lzx(uncompressed, resets);

        final ByteArrayOutputStream control = new ByteArrayOutputStream();
        le32(control, 6);
        control.write(new byte[]{'L', 'Z', 'X', 'C'});
        le32(control, 2);
        le32(control, FRAMES_PER_RESET);        // reset interval, in 32 KB units
        le32(control, (1 << WINDOW_BITS) / FRAME); // window, in 32 KB units
        le32(control, 1);
        le32(control, 0);

        final ByteArrayOutputStream reset = new ByteArrayOutputStream();
        le32(reset, 2);
        le32(reset, resets.size());
        le32(reset, 8);
        le32(reset, 0x28);
        le64(reset, uncompressed.length);
        le64(reset, content.length);
        le64(reset, FRAME);
        for (long r : resets) {
            le64(reset, r);
        }

        // Section 0: the stored files and section 1's own bookkeeping
        final List<File> stored = new ArrayList<File>();
        stored.add(new File("::DataSpace/Storage/MSCompressed/ControlData", control.toByteArray(), false));
        stored.add(new File("::DataSpace/Storage/MSCompressed/Transform/{7FC28940-9D31-11D0-9B27-00A0C91E9C7C}/InstanceData/ResetTable", reset.toByteArray(), false));
        stored.add(new File("::DataSpace/Storage/MSCompressed/Content", content, false));
        for (File f : files) {
            if (!f.compressed) {
                stored.add(f);
            }
        }
        final ByteArrayOutputStream section0 = new ByteArrayOutputStream();
        for (File f : stored) {
            f.offset = section0.size();
            section0.write(f.data);
        }

        // The directory: one listing chunk
        final int chunkSize = 0x1000;
        final ByteArrayOutputStream entries = new ByteArrayOutputStream();
        final List<File> all = new ArrayList<File>(stored);
        for (File f : files) {
            if (f.compressed) {
                all.add(f);
            }
        }
        for (File f : all) {
            final byte[] name = f.name.getBytes(Charset.forName("UTF-8"));
            encint(entries, name.length);
            entries.write(name);
            encint(entries, f.compressed ? 1 : 0);
            encint(entries, f.offset);
            encint(entries, f.data.length);
        }
        if (0x14 + entries.size() > chunkSize) {
            throw new IOException("too many files for one directory chunk");
        }
        final ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        chunk.write(new byte[]{'P', 'M', 'G', 'L'});
        le32(chunk, chunkSize - 0x14 - entries.size());
        le32(chunk, 0);
        le32(chunk, -1);
        le32(chunk, -1);
        chunk.write(entries.toByteArray());
        while (chunk.size() < chunkSize) {
            chunk.write(0);
        }

        final ByteArrayOutputStream itsp = new ByteArrayOutputStream();
        itsp.write(new byte[]{'I', 'T', 'S', 'P'});
        le32(itsp, 1);
        le32(itsp, 0x54);
        le32(itsp, 0x0A);
        le32(itsp, chunkSize);
        le32(itsp, 2);
        le32(itsp, 1);
        le32(itsp, -1);
        le32(itsp, 0);
        le32(itsp, 0);
        le32(itsp, -1);
        le32(itsp, 1);           // number of chunks
        le32(itsp, 0x409);
        itsp.write(new byte[16]);
        le32(itsp, 0x54);
        le32(itsp, -1);
        le32(itsp, -1);
        le32(itsp, -1);
        itsp.write(chunk.toByteArray());

        final long dirOffset = 0x60 + 0x18;
        final long dirLen = itsp.size();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{'I', 'T', 'S', 'F'});
        le32(out, 3);
        le32(out, 0x60);
        le32(out, 1);
        le32(out, 0);
        le32(out, 0x409);
        out.write(new byte[32]);
        le64(out, 0x60);
        le64(out, 0x18);
        le64(out, dirOffset);
        le64(out, dirLen);
        le64(out, dirOffset + dirLen);
        out.write(new byte[0x18]);
        out.write(itsp.toByteArray());
        out.write(section0.toByteArray());
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ LZX

    private static final class Bits {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int word;
        int used;

        void write(final int value, final int n) {
            for (int i = n - 1; i >= 0; i--) {
                word = (word << 1) | ((value >>> i) & 1);
                if (++used == 16) {
                    flushWord();
                }
            }
        }

        private void flushWord() {
            out.write(word & 0xFF);
            out.write((word >>> 8) & 0xFF);
            word = 0;
            used = 0;
        }

        /** Pads to the 16-bit boundary a frame ends on. */
        void align() {
            if (used > 0) {
                word <<= 16 - used;
                flushWord();
            }
        }
    }

    /**
     * Pretree with symbols 0 and 9 one bit long: 0 keeps a length as it was, 9 turns a 0 into
     * an 8 (the delta is taken modulo 17).
     */
    private static void pretree(final Bits bits) {
        for (int i = 0; i < 20; i++) {
            bits.write(i == 0 || i == 9 ? 1 : 0, 4);
        }
    }

    private static byte[] lzx(final byte[] data, final List<Long> resets) {
        final Bits bits = new Bits();
        final int frames = (data.length + FRAME - 1) / FRAME;
        for (int frame = 0; frame < frames; frame++) {
            resets.add((long) bits.out.size());
            if (frame % FRAMES_PER_RESET == 0) {
                // A reset: the intel header, then one verbatim block for the whole interval
                bits.write(0, 1);
                final int blockLen = Math.min(FRAMES_PER_RESET * FRAME, data.length - frame * FRAME);
                bits.write(1, 3);
                bits.write(blockLen >>> 8, 16);
                bits.write(blockLen & 0xFF, 8);
                pretree(bits);
                for (int i = 0; i < 256; i++) {
                    bits.write(1, 1); // symbol 9: length 8
                }
                pretree(bits);
                for (int i = 256; i < MAIN_ELEMENTS; i++) {
                    bits.write(0, 1); // symbol 0: no code
                }
                pretree(bits);
                for (int i = 0; i < 249; i++) {
                    bits.write(0, 1);
                }
            }
            final int end = Math.min(data.length, (frame + 1) * FRAME);
            for (int p = frame * FRAME; p < end; p++) {
                // Every byte value has an 8-bit code, which is the value itself
                bits.write(data[p] & 0xFF, 8);
            }
            bits.align();
        }
        return bits.out.toByteArray();
    }

    // ------------------------------------------------------------------ numbers

    private static void encint(final ByteArrayOutputStream out, long v) {
        final byte[] tmp = new byte[10];
        int n = 0;
        do {
            tmp[n++] = (byte) (v & 0x7F);
            v >>>= 7;
        } while (v != 0);
        for (int i = n - 1; i >= 0; i--) {
            out.write(tmp[i] | (i > 0 ? 0x80 : 0));
        }
    }

    private static void le16(final ByteArrayOutputStream out, final int v) {
        out.write(v & 0xFF);
        out.write((v >>> 8) & 0xFF);
    }

    private static void le32(final ByteArrayOutputStream out, final int v) {
        le16(out, v);
        le16(out, v >>> 16);
    }

    private static void le64(final ByteArrayOutputStream out, final long v) {
        le32(out, (int) v);
        le32(out, (int) (v >>> 32));
    }
}
