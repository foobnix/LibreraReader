package com.foobnix.ext.office;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The streams of a Compound File (OLE2), the container of Word 97-2003 documents: the whole
 * file is read, its allocation tables followed, and a stream is handed out by name.
 *
 * Everything a file says about itself is checked against the file's size, so a damaged or
 * crafted one ends in an exception rather than a loop or an allocation it cannot meet.
 */
final class CompoundFile {

    private static final long SIGNATURE = 0xE11AB1A1E011CFD0L;
    private static final int FREE = -1;
    private static final int END = -2;

    private final byte[] data;
    private final int sectorSize;
    private final int miniSectorSize;
    private final int miniCutoff;
    private final int[] fat;
    private int[] miniFat = new int[0];
    private byte[] miniStream = new byte[0];
    private final Map<String, int[]> streams = new HashMap<String, int[]>(); // name -> start, size

    static boolean isCompoundFile(byte[] header) {
        return header != null && header.length >= 8 && readLong(header, 0) == SIGNATURE;
    }

    static CompoundFile open(String path) throws IOException {
        final RandomAccessFile file = new RandomAccessFile(path, "r");
        try {
            final long length = file.length();
            if (length < 512 || length > Integer.MAX_VALUE - 8) {
                throw new IOException("not a compound file of a size that can be read: " + length);
            }
            final byte[] data = new byte[(int) length];
            file.readFully(data);
            return new CompoundFile(data);
        } finally {
            file.close();
        }
    }

    private CompoundFile(byte[] data) throws IOException {
        this.data = data;
        if (!isCompoundFile(data)) {
            throw new IOException("not a compound file");
        }
        final int sectorShift = readShort(data, 0x1E);
        final int miniShift = readShort(data, 0x20);
        if (sectorShift != 9 && sectorShift != 12 || miniShift != 6) {
            throw new IOException("sector size " + sectorShift);
        }
        sectorSize = 1 << sectorShift;
        miniSectorSize = 1 << miniShift;
        miniCutoff = readInt(data, 0x38);

        final int fatSectors = readInt(data, 0x2C);
        final int directoryStart = readInt(data, 0x30);
        final int miniFatStart = readInt(data, 0x3C);
        final int miniFatSectors = readInt(data, 0x40);
        int difatSector = readInt(data, 0x44);
        final int difatSectors = readInt(data, 0x48);

        final int sectors = data.length / sectorSize;
        if (fatSectors < 0 || fatSectors > sectors) {
            throw new IOException("FAT sectors " + fatSectors);
        }

        // the sectors that hold the FAT: 109 listed in the header, the rest in DIFAT sectors
        final List<Integer> fatList = new ArrayList<Integer>();
        for (int i = 0; i < 109 && fatList.size() < fatSectors; i++) {
            final int sector = readInt(data, 0x4C + i * 4);
            if (sector >= 0) {
                fatList.add(sector);
            }
        }
        for (int n = 0; n < difatSectors && difatSector >= 0 && fatList.size() < fatSectors; n++) {
            final int offset = sectorOffset(difatSector);
            final int entries = sectorSize / 4 - 1;
            for (int i = 0; i < entries && fatList.size() < fatSectors; i++) {
                final int sector = readInt(data, offset + i * 4);
                if (sector >= 0) {
                    fatList.add(sector);
                }
            }
            difatSector = readInt(data, offset + entries * 4);
        }
        fat = new int[fatList.size() * (sectorSize / 4)];
        int k = 0;
        for (int sector : fatList) {
            final int offset = sectorOffset(sector);
            for (int i = 0; i < sectorSize / 4; i++) {
                fat[k++] = readInt(data, offset + i * 4);
            }
        }

        // the directory: names, kinds, where each stream starts and how long it is
        final byte[] directory = chain(directoryStart, -1);
        int rootStart = END;
        int rootSize = 0;
        for (int offset = 0; offset + 128 <= directory.length; offset += 128) {
            final int type = directory[offset + 0x42];
            if (type != 1 && type != 2 && type != 5) {
                continue;
            }
            final int nameLength = Math.min(64, readShort(directory, offset + 0x40));
            final String name = nameLength >= 2
                    ? new String(directory, offset, nameLength - 2, Charset.forName("UTF-16LE")) : "";
            final int start = readInt(directory, offset + 0x74);
            final int size = readInt(directory, offset + 0x78);
            if (type == 5) {
                rootStart = start;
                rootSize = size;
            } else if (type == 2 && !streams.containsKey(name)) {
                streams.put(name, new int[]{start, size});
            }
        }
        if (rootStart >= 0 && rootSize > 0) {
            miniStream = chain(rootStart, rootSize);
        }
        if (miniFatStart >= 0 && miniFatSectors > 0) {
            final byte[] table = chain(miniFatStart, -1);
            miniFat = new int[table.length / 4];
            for (int i = 0; i < miniFat.length; i++) {
                miniFat[i] = readInt(table, i * 4);
            }
        }
    }

    /** A stream by name, or null when there is none. */
    byte[] stream(String name) throws IOException {
        final int[] entry = streams.get(name);
        if (entry == null) {
            return null;
        }
        final int start = entry[0];
        final int size = entry[1];
        if (size < 0 || size > data.length + miniStream.length) {
            throw new IOException("stream size " + size);
        }
        if (size < miniCutoff) {
            return miniChain(start, size);
        }
        return chain(start, size);
    }

    private byte[] chain(int start, int size) throws IOException {
        final int limit = size >= 0 ? size : data.length;
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(Math.min(limit, 1 << 20));
        int sector = start;
        int steps = 0;
        while (sector >= 0 && out.size() < limit) {
            if (sector >= fat.length || ++steps > data.length / sectorSize + 1) {
                throw new IOException("broken sector chain");
            }
            final int offset = sectorOffset(sector);
            out.write(data, offset, Math.min(sectorSize, limit - out.size()));
            sector = fat[sector];
        }
        return out.toByteArray();
    }

    private byte[] miniChain(int start, int size) throws IOException {
        final byte[] out = new byte[size];
        int written = 0;
        int sector = start;
        int steps = 0;
        while (sector >= 0 && written < size) {
            if (sector >= miniFat.length || ++steps > miniFat.length + 1) {
                throw new IOException("broken mini sector chain");
            }
            final int offset = sector * miniSectorSize;
            if (offset + miniSectorSize > miniStream.length) {
                throw new IOException("mini sector out of the file");
            }
            final int n = Math.min(miniSectorSize, size - written);
            System.arraycopy(miniStream, offset, out, written, n);
            written += n;
            sector = miniFat[sector];
        }
        return out;
    }

    private int sectorOffset(int sector) throws IOException {
        final long offset = (long) (sector + 1) * sectorSize;
        if (sector < 0 || offset + sectorSize > data.length) {
            throw new IOException("sector out of the file: " + sector);
        }
        return (int) offset;
    }

    static int readShort(byte[] b, int offset) {
        return (b[offset] & 0xFF) | (b[offset + 1] & 0xFF) << 8;
    }

    static int readInt(byte[] b, int offset) {
        return (b[offset] & 0xFF) | (b[offset + 1] & 0xFF) << 8 | (b[offset + 2] & 0xFF) << 16 | (b[offset + 3] & 0xFF) << 24;
    }

    static long readLong(byte[] b, int offset) {
        return (readInt(b, offset) & 0xFFFFFFFFL) | ((long) readInt(b, offset + 4)) << 32;
    }
}
