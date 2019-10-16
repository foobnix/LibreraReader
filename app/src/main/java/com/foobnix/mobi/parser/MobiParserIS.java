package com.foobnix.mobi.parser;

import com.foobnix.android.utils.LOG;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobiParserIS {
    public static int COMPRESSION_NONE = 0;
    public static int COMPRESSION_PalmDOC = 2;
    public static int COMPRESSION_HUFF = 17480;

    public String name;

    public int recordsCount;
    List<Integer> recordsOffset = new ArrayList<Integer>();
    public int mobiType;
    public String encoding;
    public String fullName;
    public String locale;
    public InputStream raw, raw1;
    public int firstImageIndex;
    public int lastContentIndex;
    public EXTH exth = new EXTH();
    private int firstContentIndex;

    private int commpression;
    private int bookSize;

    // http://wiki.mobileread.com/wiki/MOBI#MOBI_Header
    // https://wiki.mobileread.com/wiki/MOBI#EXTH_Header
    public class EXTH {
        public String identifier;
        public int len;
        public int count;
        public Map<Integer, byte[]> headers = new HashMap<Integer, byte[]>();

        public EXTH parse(InputStream raw) throws IOException {
            int offset = indexOf(raw, "EXTH".getBytes());
            identifier = asString(raw, offset, 4);
            len = asInt(raw, offset + 4, 4);
            count = asInt(raw, offset + 8, 4);

            int rOffset = offset + 12;

            for (int i = 0; i < count - 1; i++) {
                int rType = asInt(raw, rOffset, 4);
                int rLen = asInt(raw, rOffset + 4, 4);
                byte[] data = copyOfRange(raw, rOffset + 8, rLen - 8);
                rOffset = rOffset + rLen;
                headers.put(rType, data);

            }

            return this;
        }

    }

    public static int byteArrayToInt(byte[] buffer) {
        int total = 0;
        int len = buffer.length;
        for (int i = 0; i < len; i++) {
            total = (total << 8) + (buffer[i] & 0xff);
        }
        return total;
    }

    public static int peekInt(byte[] src, int offset, ByteOrder order) {
        if (order == ByteOrder.BIG_ENDIAN) {
            return (((src[offset++] & 0xff) << 24) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset++] & 0xff) << 8) |
                    ((src[offset] & 0xff) << 0));
        } else {
            return (((src[offset++] & 0xff) << 0) |
                    ((src[offset++] & 0xff) << 8) |
                    ((src[offset++] & 0xff) << 16) |
                    ((src[offset] & 0xff) << 24));
        }
    }

    public static String asString(InputStream is, int offset, int len) throws IOException {
        is.reset();
        byte[] res = new byte[len];
        IOUtils.skip(is, offset);
        IOUtils.readFully(is, res);
        return new String(res);
    }

    public static int asInt(InputStream is, int offset, int len) throws IOException {
        is.reset();
        byte[] res = new byte[len];
        IOUtils.skip(is, offset);
        IOUtils.readFully(is, res);
        return byteArrayToInt(res);
    }

    public static int asInt_LITTLE_ENDIAN(InputStream is, int offset, int len) throws IOException {
        is.reset();
        byte[] res = new byte[len];
        IOUtils.skip(is, offset);
        IOUtils.readFully(is, res);
        return peekInt(res,0, ByteOrder.LITTLE_ENDIAN);

    }

    public static byte[] copyOfRange(InputStream is, long offset, int len) {
        LOG.d("copyOfRange", offset, (offset / 1024 / 1024));
        try {
            byte[] res = new byte[len];
            is.reset();
            IOUtils.skip(is, offset);
            IOUtils.readFully(is, res);
            return res;
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public static int toInt(byte bytes[], int pos) {
        if (bytes.length < 10) {
            return -1;
        }
        return (bytes[bytes.length - pos] & 0xff);
    }

    public int indexOf(InputStream is, byte[] search) {
        try {
            is.reset();
            byte[] res = new byte[search.length];
            int count = 0;
            while (is.read(res) != -1) {
                if (Arrays.equals(res, search)) {
                    return count;
                }
                count += search.length;
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return -1;
    }

    public void close() {
        try {
            raw1.close();
            raw.close();
        } catch (IOException e) {
            LOG.e(e);
        }
    }

    public MobiParserIS(InputStream is) throws Exception {
        raw1 = is;
        is = new BufferedInputStream(is);
        is.mark(16 * 1024);
        raw = is;

        name = asString(raw, 0, 32);

        recordsCount = asInt(raw, 76, 2);

        for (int i = 78; i < 78 + recordsCount * 8; i += 8) {
            int recordOffset = asInt(is, i, 4);
            recordsOffset.add(recordOffset);
        }
        int mobiOffset = recordsOffset.get(0);

        commpression = asInt(raw, mobiOffset, 2);
        bookSize = asInt(raw, mobiOffset + 4, 4);

        int encryption = asInt(raw, mobiOffset + 12, 2);

        mobiType = asInt(raw, mobiOffset + 24, 4);
        encoding = asInt(raw, mobiOffset + 28, 4) == 1252 ? "cp1251" : "UTF-8";

        int fullNameOffset = asInt(raw, mobiOffset + 84, 4);
        int fullNameLen = asInt(raw, mobiOffset + 88, 4);
        fullName = asString(raw, mobiOffset + fullNameOffset, fullNameLen);
        if (encryption != 0) {
            fullName = "(DRM) " + fullName;
        }

        locale = asString(raw, mobiOffset + 92, 4);

        // int huffmanRecord = asInt(raw, mobiOffset + 112, 4);
        // int huffmanCount = asInt(raw, mobiOffset + 116, 4);
        // int huffmanOffset = asInt(raw, mobiOffset + 120, 4);
        // int huffmanLen = asInt(raw, mobiOffset + 124, 4);
        // byte[] huffman = Arrays.copyOfRange(raw, mobiOffset + huffmanOffset,
        // mobiOffset + huffmanOffset + huffmanLen);

        firstImageIndex = asInt(raw, mobiOffset + 108, 4);
        boolean isEXTHFlag = (asInt(raw, mobiOffset + 128, 4) & 0x40) != 0;
        firstContentIndex = asInt(raw, mobiOffset + 192, 2);
        lastContentIndex = asInt(raw, mobiOffset + 194, 2);

        if (isEXTHFlag) {
            exth = new EXTH().parse(raw);
        }
    }

    byte[] INDX = "INDX".getBytes();

    public static String byteArrayToString(byte[] buffer, String encoding) {
        int len = buffer.length;
        int zeroIndex = -1;
        for (int i = 0; i < len; i++) {
            byte b = buffer[i];
            if (b == 0) {
                zeroIndex = i;
                break;
            }
        }

        if (encoding != null) {
            try {
                if (zeroIndex == -1)
                    return new String(buffer, encoding);
                else
                    return new String(buffer, 0, zeroIndex, encoding);
            } catch (java.io.UnsupportedEncodingException e) {
                // let it fall through and use the default encoding
            }
        }

        if (zeroIndex == -1)
            return new String(buffer);
        else
            return new String(buffer, 0, zeroIndex);
    }

    public String getTitle() {
        return fullName;
    }

    public String getLocale() {
        return locale;
    }

    public String getAuthor() {
        byte[] bytes = exth.headers.get(100);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public String getSubject() {
        byte[] bytes = exth.headers.get(105);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public String getPublisher() {
        byte[] bytes = exth.headers.get(101);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public String getIsbn() {
        byte[] bytes = exth.headers.get(104);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public String getPublishDate() {
        byte[] bytes = exth.headers.get(106);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public int getBookSize() {
        return bookSize;
    }

    public String getLanguage() {
        byte[] bytes = exth.headers.get(524);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public String getDescription() {
        byte[] bytes = exth.headers.get(103);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    public Map<Integer, byte[]> getHeaders() {
        return exth.headers;
    }

    public byte[] getCoverOrThumb() {
        byte[] imgNumber = exth.headers.get(201);
        if (imgNumber == null) {
            imgNumber = exth.headers.get(202);
        }

        if (imgNumber != null) {
            int index = byteArrayToInt(imgNumber);
            return getRecordByIndex(index + firstImageIndex);
        } else {
            for (int i = firstImageIndex; i < lastContentIndex; i++) {
                byte[] img = getRecordByIndex(i);
                if (img == null) {
                    return null;
                }
                if ((img[0] & 0xff) == 0xFF && (img[1] & 0xff) == 0xD8) {
                    return img;
                }
            }
        }
        return null;
    }

    public byte[] getRecordByIndex(int index) {
        if (index >= recordsOffset.size()) {
            return null;
        }

        Integer from = recordsOffset.get(index);
        int size = 0;
        if (index + 1 < recordsOffset.size()) {
            size = recordsOffset.get(index + 1) - from;
        }

        return copyOfRange(raw, from, size);
    }

}
