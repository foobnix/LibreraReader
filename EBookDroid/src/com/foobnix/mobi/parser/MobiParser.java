package com.foobnix.mobi.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.foobnix.android.utils.LOG;

public class MobiParser {
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
    private byte[] raw;
    public int firstImageIndex;
    public int lastContentIndex;
    public EXTH exth = new EXTH();
    private int firstContentIndex;

    private int commpression;
    private int bookSize;

    // http://wiki.mobileread.com/wiki/MOBI#MOBI_Header
    // https://wiki.mobileread.com/wiki/MOBI#EXTH_Header
    class EXTH {
        public String identifier;
        public int len;
        public int count;
        public Map<Integer, byte[]> headers = new HashMap<Integer, byte[]>();

        public EXTH parse(byte[] raw) {
            int offset = indexOf(raw, "EXTH".getBytes());
            identifier = asString(raw, offset, 4);
            len = asInt(raw, offset + 4, 4);
            count = asInt(raw, offset + 8, 4);

            int rOffset = offset + 12;

            for (int i = 0; i < count; i++) {
                int rType = asInt(raw, rOffset, 4);
                int rLen = asInt(raw, rOffset + 4, 4);
                byte[] data = Arrays.copyOfRange(raw, rOffset + 8, rOffset + rLen);
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

    public static String asString(byte[] raw, int offset, int len) {
        return new String(Arrays.copyOfRange(raw, offset, offset + len));
    }

    public static int asInt(byte[] raw, int offset, int len) {
        byte[] range = Arrays.copyOfRange(raw, offset, offset + len);
        return byteArrayToInt(range);
    }

    public static int toInt(byte bytes[], int pos) {
        if (bytes.length < 10) {
            return -1;
        }
        return (bytes[bytes.length - pos] & 0xff);
    }

    public static byte[] lz77(byte[] bytes) {
        ByteArrayBuffer outputStream = new ByteArrayBuffer(bytes.length);
        int i = 0;
        while (i < bytes.length - 4) {// try -2,4,8,10
            int b = bytes[i++] & 0x00FF;
            try {
                if (b == 0x0) {
                    outputStream.write(b);
                } else if (b <= 0x08) {
                    for (int j = 0; j < b; j++)
                        outputStream.write(bytes[i + j]);
                    i += b;
                }

                else if (b <= 0x7f) {
                    outputStream.write(b);
                } else if (b <= 0xbf) {
                    b = b << 8 | bytes[i++] & 0xFF;
                    int length = (b & 0x0007) + 3;
                    int location = (b >> 3) & 0x7FF;

                    for (int j = 0; j < length; j++)
                        outputStream.write(outputStream.getRawData()[outputStream.size() - location]);
                } else {
                    outputStream.write(' ');
                    outputStream.write(b ^ 0x80);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outputStream.getRawData();
    }

    public int indexOf(byte[] input, byte[] search) {
        for (int i = 0; i < input.length - search.length + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < search.length; ++j) {
                if (input[i + j] != search[j]) {
                    found = false;
                    break;
                }
            }
            if (found)
                return i;
        }
        return -1;
    }

    public MobiParser(byte[] file) throws IOException {
        raw = file;

        name = asString(raw, 0, 32);

        recordsCount = asInt(raw, 76, 2);

        for (int i = 78; i < 78 + recordsCount * 8; i += 8) {
            int recordOffset = byteArrayToInt(Arrays.copyOfRange(raw, i, i + 4));
            // int recordID = byteArrayToInt(Arrays.copyOfRange(raw, i + 5, i +
            // 5 + 3));
            recordsOffset.add(recordOffset);
        }
        int mobiOffset = recordsOffset.get(0);

        commpression = asInt(raw, mobiOffset, 2);
        bookSize = asInt(raw, mobiOffset + 4, 4);

        int encryption = asInt(raw, mobiOffset + 12, 2);
        LOG.d("MobiParser", "encryption", encryption);

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

    public String getTextContent() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (firstContentIndex == 0) {
            firstContentIndex = 1;
        }
        for (int i = firstContentIndex; i < lastContentIndex - 1 && i < firstImageIndex; i++) {
            int start = recordsOffset.get(i);
            int end = recordsOffset.get(i + 1);
            byte[] coded = Arrays.copyOfRange(raw, start, end);

            byte[] decoded = null;
            if (commpression == COMPRESSION_PalmDOC) {
                decoded = lz77(coded);
            } else if (commpression == COMPRESSION_NONE) {
                decoded = coded;
            } else if (commpression == COMPRESSION_HUFF) {
                try {
                    decoded = coded;
                } catch (Exception e) {
                    e.printStackTrace();
                    decoded = ("error").getBytes();
                }
            } else {
                decoded = ("Compression not supported " + commpression).getBytes();
            }

            byte[] header = Arrays.copyOfRange(decoded, 0, 4);
            if (Arrays.equals(INDX, header)) {
                continue;
            }

            for (int n = 0; n < decoded.length; n++) {
                if (decoded[n] != 0x00) {
                    outputStream.write(decoded[n]);
                }

            }
        }
        try {
            return outputStream.toString(encoding);
        } catch (UnsupportedEncodingException e) {
            return outputStream.toString();
        }
    }

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

    public String getPublisher()
    {
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
        Integer to = raw.length;
        if (index + 1 < recordsOffset.size()) {
            to = recordsOffset.get(index + 1);
        }

        return Arrays.copyOfRange(raw, from, to);
    }

}
