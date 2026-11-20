package com.foobnix.ext.lit;

/**
 * The two ciphers of Microsoft Reader's "sealed" (DRM level 1 and 3) books, both changed by
 * Microsoft from the standards, so the platform's DES and SHA-1 cannot be used:
 * <ul>
 * <li>{@link #msSha1} - SHA-1 with other initial values and some round functions swapped, as
 * calibre's mssha1.py has it;</li>
 * <li>{@link Des} - Richard Outerbridge's public domain d3des with the S-boxes of Dan A. Jackson's
 * ConvertLIT (spr.h), as calibre's msdes has it.</li>
 * </ul>
 */
final class LitCrypto {

    private LitCrypto() {
    }

    // ---------------------------------------------------------------- MS SHA-1

    private static final int[] K = {0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC, 0xCA62C1D6};
    // 0: (B & (C ^ D)) ^ D, 1: B ^ C ^ D, 2: ((B | C) & D) | (B & C), 3: (B + C) ^ C
    private static final int[] F = new int[80];

    static {
        for (int t = 0; t < 80; t++) {
            F[t] = t < 20 ? 0 : (t < 40 ? 1 : (t < 60 ? 2 : 1));
        }
        F[3] = 1;
        F[6] = 3;
        F[10] = 1;
        F[15] = 1;
        F[26] = 0;
        F[31] = 2;
        F[42] = 3;
        F[51] = 1;
        F[68] = 0;
    }

    static byte[] msSha1(byte[] data) {
        final int[] h = {0x32107654, 0x23016745, 0xC4E680A2, 0xDC679823, 0xD0857A34};
        final long bits = (long) data.length << 3;
        int padded = data.length + 1 + 8;
        padded = (padded + 63) & ~63;
        final byte[] msg = new byte[padded];
        System.arraycopy(data, 0, msg, 0, data.length);
        msg[data.length] = (byte) 0x80;
        for (int i = 0; i < 8; i++) {
            msg[padded - 1 - i] = (byte) (bits >>> (8 * i));
        }
        final int[] w = new int[80];
        for (int block = 0; block < padded; block += 64) {
            for (int t = 0; t < 16; t++) {
                final int p = block + t * 4;
                w[t] = ((msg[p] & 0xFF) << 24) | ((msg[p + 1] & 0xFF) << 16) | ((msg[p + 2] & 0xFF) << 8) | (msg[p + 3] & 0xFF);
            }
            for (int t = 16; t < 80; t++) {
                w[t] = Integer.rotateLeft(w[t - 3] ^ w[t - 8] ^ w[t - 14] ^ w[t - 16], 1);
            }
            int a = h[0], b = h[1], c = h[2], d = h[3], e = h[4];
            for (int t = 0; t < 80; t++) {
                final int f;
                switch (F[t]) {
                    case 0:
                        f = (b & (c ^ d)) ^ d;
                        break;
                    case 1:
                        f = b ^ c ^ d;
                        break;
                    case 2:
                        f = ((b | c) & d) | (b & c);
                        break;
                    default:
                        f = (b + c) ^ c;
                }
                final int temp = Integer.rotateLeft(a, 5) + f + e + w[t] + K[t / 20];
                e = d;
                d = c;
                c = Integer.rotateLeft(b, 30);
                b = a;
                a = temp;
            }
            h[0] += a;
            h[1] += b;
            h[2] += c;
            h[3] += d;
            h[4] += e;
        }
        final byte[] digest = new byte[20];
        for (int i = 0; i < 5; i++) {
            digest[i * 4] = (byte) (h[i] >>> 24);
            digest[i * 4 + 1] = (byte) (h[i] >>> 16);
            digest[i * 4 + 2] = (byte) (h[i] >>> 8);
            digest[i * 4 + 3] = (byte) h[i];
        }
        return digest;
    }

    // ---------------------------------------------------------------- MS DES

    private static final int[] BYTEBIT = {0200, 0100, 040, 020, 010, 04, 02, 01};
    private static final int[] BIGBYTE = {0x800000, 0x400000, 0x200000, 0x100000, 0x80000, 0x40000, 0x20000, 0x10000, 0x8000, 0x4000, 0x2000, 0x1000,
            0x800, 0x400, 0x200, 0x100, 0x80, 0x40, 0x20, 0x10, 0x8, 0x4, 0x2, 0x1};
    private static final int[] PC1 = {56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35,
            62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3};
    private static final int[] TOTROT = {1, 2, 4, 6, 8, 10, 12, 14, 15, 17, 19, 21, 23, 25, 27, 28};
    private static final int[] PC2 = {13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1,
            40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31};

    private static final int[] SP1 = {
            0x02080800, 0x00080000, 0x02000002, 0x02080802, 0x02000000, 0x00080802, 0x00080002, 0x02000002, 0x00080802, 0x02080800, 0x02080000, 0x00000802, 0x02000802, 0x02000000, 0x00000000, 0x00080002, 0x00080000, 0x00000002, 0x02000800, 0x00080800, 0x02080802, 0x02080000, 0x00000802, 0x02000800, 0x00000002, 0x00000800, 0x00080800, 0x02080002, 0x00000800, 0x02000802, 0x02080002, 0x00000000, 0x00000000, 0x02080802, 0x02000800, 0x00080002, 0x02080800, 0x00080000, 0x00000802, 0x02000800, 0x02080002, 0x00000800, 0x00080800, 0x02000002, 0x00080802, 0x00000002, 0x02000002, 0x02080000, 0x02080802, 0x00080800, 0x02080000, 0x02000802, 0x02000000, 0x00000802, 0x00080002, 0x00000000, 0x00080000, 0x02000000, 0x02000802, 0x02080800, 0x00000002, 0x02080002, 0x00000800, 0x00080802};;
    private static final int[] SP2 = {
            0x40108010, 0x00000000, 0x00108000, 0x40100000, 0x40000010, 0x00008010, 0x40008000, 0x00108000, 0x00008000, 0x40100010, 0x00000010, 0x40008000, 0x00100010, 0x40108000, 0x40100000, 0x00000010, 0x00100000, 0x40008010, 0x40100010, 0x00008000, 0x00108010, 0x40000000, 0x00000000, 0x00100010, 0x40008010, 0x00108010, 0x40108000, 0x40000010, 0x40000000, 0x00100000, 0x00008010, 0x40108010, 0x00100010, 0x40108000, 0x40008000, 0x00108010, 0x40108010, 0x00100010, 0x40000010, 0x00000000, 0x40000000, 0x00008010, 0x00100000, 0x40100010, 0x00008000, 0x40000000, 0x00108010, 0x40008010, 0x40108000, 0x00008000, 0x00000000, 0x40000010, 0x00000010, 0x40108010, 0x00108000, 0x40100000, 0x40100010, 0x00100000, 0x00008010, 0x40008000, 0x40008010, 0x00000010, 0x40100000, 0x00108000};;
    private static final int[] SP3 = {
            0x04000001, 0x04040100, 0x00000100, 0x04000101, 0x00040001, 0x04000000, 0x04000101, 0x00040100, 0x04000100, 0x00040000, 0x04040000, 0x00000001, 0x04040101, 0x00000101, 0x00000001, 0x04040001, 0x00000000, 0x00040001, 0x04040100, 0x00000100, 0x00000101, 0x04040101, 0x00040000, 0x04000001, 0x04040001, 0x04000100, 0x00040101, 0x04040000, 0x00040100, 0x00000000, 0x04000000, 0x00040101, 0x04040100, 0x00000100, 0x00000001, 0x00040000, 0x00000101, 0x00040001, 0x04040000, 0x04000101, 0x00000000, 0x04040100, 0x00040100, 0x04040001, 0x00040001, 0x04000000, 0x04040101, 0x00000001, 0x00040101, 0x04000001, 0x04000000, 0x04040101, 0x00040000, 0x04000100, 0x04000101, 0x00040100, 0x04000100, 0x00000000, 0x04040001, 0x00000101, 0x04000001, 0x00040101, 0x00000100, 0x04040000};;
    private static final int[] SP4 = {
            0x00401008, 0x10001000, 0x00000008, 0x10401008, 0x00000000, 0x10400000, 0x10001008, 0x00400008, 0x10401000, 0x10000008, 0x10000000, 0x00001008, 0x10000008, 0x00401008, 0x00400000, 0x10000000, 0x10400008, 0x00401000, 0x00001000, 0x00000008, 0x00401000, 0x10001008, 0x10400000, 0x00001000, 0x00001008, 0x00000000, 0x00400008, 0x10401000, 0x10001000, 0x10400008, 0x10401008, 0x00400000, 0x10400008, 0x00001008, 0x00400000, 0x10000008, 0x00401000, 0x10001000, 0x00000008, 0x10400000, 0x10001008, 0x00000000, 0x00001000, 0x00400008, 0x00000000, 0x10400008, 0x10401000, 0x00001000, 0x10000000, 0x10401008, 0x00401008, 0x00400000, 0x10401008, 0x00000008, 0x10001000, 0x00401008, 0x00400008, 0x00401000, 0x10400000, 0x10001008, 0x00001008, 0x10000000, 0x10000008, 0x10401000};;
    private static final int[] SP5 = {
            0x08000000, 0x00010000, 0x00000400, 0x08010420, 0x08010020, 0x08000400, 0x00010420, 0x08010000, 0x00010000, 0x00000020, 0x08000020, 0x00010400, 0x08000420, 0x08010020, 0x08010400, 0x00000000, 0x00010400, 0x08000000, 0x00010020, 0x00000420, 0x08000400, 0x00010420, 0x00000000, 0x08000020, 0x00000020, 0x08000420, 0x08010420, 0x00010020, 0x08010000, 0x00000400, 0x00000420, 0x08010400, 0x08010400, 0x08000420, 0x00010020, 0x08010000, 0x00010000, 0x00000020, 0x08000020, 0x08000400, 0x08000000, 0x00010400, 0x08010420, 0x00000000, 0x00010420, 0x08000000, 0x00000400, 0x00010020, 0x08000420, 0x00000400, 0x00000000, 0x08010420, 0x08010020, 0x08010400, 0x00000420, 0x00010000, 0x00010400, 0x08010020, 0x08000400, 0x00000420, 0x00000020, 0x00010420, 0x08010000, 0x08000020};;
    private static final int[] SP6 = {
            0x80000040, 0x00200040, 0x00000000, 0x80202000, 0x00200040, 0x00002000, 0x80002040, 0x00200000, 0x00002040, 0x80202040, 0x00202000, 0x80000000, 0x80002000, 0x80000040, 0x80200000, 0x00202040, 0x00200000, 0x80002040, 0x80200040, 0x00000000, 0x00002000, 0x00000040, 0x80202000, 0x80200040, 0x80202040, 0x80200000, 0x80000000, 0x00002040, 0x00000040, 0x00202000, 0x00202040, 0x80002000, 0x00002040, 0x80000000, 0x80002000, 0x00202040, 0x80202000, 0x00200040, 0x00000000, 0x80002000, 0x80000000, 0x00002000, 0x80200040, 0x00200000, 0x00200040, 0x80202040, 0x00202000, 0x00000040, 0x80202040, 0x00202000, 0x00200000, 0x80002040, 0x80000040, 0x80200000, 0x00202040, 0x00000000, 0x00002000, 0x80000040, 0x80002040, 0x80202000, 0x80200000, 0x00002040, 0x00000040, 0x80200040};;
    private static final int[] SP7 = {
            0x00004000, 0x00000200, 0x01000200, 0x01000004, 0x01004204, 0x00004004, 0x00004200, 0x00000000, 0x01000000, 0x01000204, 0x00000204, 0x01004000, 0x00000004, 0x01004200, 0x01004000, 0x00000204, 0x01000204, 0x00004000, 0x00004004, 0x01004204, 0x00000000, 0x01000200, 0x01000004, 0x00004200, 0x01004004, 0x00004204, 0x01004200, 0x00000004, 0x00004204, 0x01004004, 0x00000200, 0x01000000, 0x00004204, 0x01004000, 0x01004004, 0x00000204, 0x00004000, 0x00000200, 0x01000000, 0x01004004, 0x01000204, 0x00004204, 0x00004200, 0x00000000, 0x00000200, 0x01000004, 0x00000004, 0x01000200, 0x00000000, 0x01000204, 0x01000200, 0x00004200, 0x00000204, 0x00004000, 0x01004204, 0x01000000, 0x01004200, 0x00000004, 0x00004004, 0x01004204, 0x01000004, 0x01004200, 0x01004000, 0x00004004};;
    private static final int[] SP8 = {
            0x20800080, 0x20820000, 0x00020080, 0x00000000, 0x20020000, 0x00800080, 0x20800000, 0x20820080, 0x00000080, 0x20000000, 0x00820000, 0x00020080, 0x00820080, 0x20020080, 0x20000080, 0x20800000, 0x00020000, 0x00820080, 0x00800080, 0x20020000, 0x20820080, 0x20000080, 0x00000000, 0x00820000, 0x20000000, 0x00800000, 0x20020080, 0x20800080, 0x00800000, 0x00020000, 0x20820000, 0x00000080, 0x00800000, 0x00020000, 0x20000080, 0x20820080, 0x00020080, 0x20000000, 0x00000000, 0x00820000, 0x20800080, 0x20020080, 0x20020000, 0x00800080, 0x20820000, 0x00000080, 0x00800080, 0x20020000, 0x20820080, 0x00800000, 0x20800000, 0x20000080, 0x00820000, 0x00020080, 0x20020080, 0x20800000, 0x00000080, 0x20820000, 0x00820080, 0x00000000, 0x20000000, 0x20800080, 0x00020000, 0x00820080};;

    /**
     * Decryption with one key, block by block (ECB).
     */
    static final class Des {
        private final int[] keys = new int[32];

        Des(byte[] key) {
            final int[] pc1m = new int[56];
            final int[] pcr = new int[56];
            final int[] kn = new int[32];
            for (int j = 0; j < 56; j++) {
                final int l = PC1[j];
                pc1m[j] = (key[l >> 3] & BYTEBIT[l & 07]) != 0 ? 1 : 0;
            }
            for (int i = 0; i < 16; i++) {
                final int m = (15 - i) << 1; // decryption
                final int n = m + 1;
                kn[m] = kn[n] = 0;
                for (int j = 0; j < 28; j++) {
                    final int l = j + TOTROT[i];
                    pcr[j] = l < 28 ? pc1m[l] : pc1m[l - 28];
                }
                for (int j = 28; j < 56; j++) {
                    final int l = j + TOTROT[i];
                    pcr[j] = l < 56 ? pc1m[l] : pc1m[l - 28];
                }
                for (int j = 0; j < 24; j++) {
                    if (pcr[PC2[j]] != 0) {
                        kn[m] |= BIGBYTE[j];
                    }
                    if (pcr[PC2[j + 24]] != 0) {
                        kn[n] |= BIGBYTE[j];
                    }
                }
            }
            int c = 0;
            for (int i = 0; i < 32; i += 2) {
                final int raw0 = kn[i];
                final int raw1 = kn[i + 1];
                keys[c++] = ((raw0 & 0x00fc0000) << 6) | ((raw0 & 0x00000fc0) << 10) | ((raw1 & 0x00fc0000) >>> 10) | ((raw1 & 0x00000fc0) >>> 6);
                keys[c++] = ((raw0 & 0x0003f000) << 12) | ((raw0 & 0x0000003f) << 16) | ((raw1 & 0x0003f000) >>> 4) | (raw1 & 0x0000003f);
            }
        }

        /**
         * Decrypts {@code data}, whose length is padded with zeros to whole blocks.
         */
        byte[] crypt(byte[] data, int off, int len) {
            final byte[] out = new byte[(len + 7) & ~7];
            System.arraycopy(data, off, out, 0, len);
            for (int p = 0; p < out.length; p += 8) {
                block(out, p);
            }
            return out;
        }

        private void block(byte[] b, int p) {
            int leftt = ((b[p] & 0xFF) << 24) | ((b[p + 1] & 0xFF) << 16) | ((b[p + 2] & 0xFF) << 8) | (b[p + 3] & 0xFF);
            int right = ((b[p + 4] & 0xFF) << 24) | ((b[p + 5] & 0xFF) << 16) | ((b[p + 6] & 0xFF) << 8) | (b[p + 7] & 0xFF);
            int work;

            work = ((leftt >>> 4) ^ right) & 0x0f0f0f0f;
            right ^= work;
            leftt ^= (work << 4);
            work = ((leftt >>> 16) ^ right) & 0x0000ffff;
            right ^= work;
            leftt ^= (work << 16);
            work = ((right >>> 2) ^ leftt) & 0x33333333;
            leftt ^= work;
            right ^= (work << 2);
            work = ((right >>> 8) ^ leftt) & 0x00ff00ff;
            leftt ^= work;
            right ^= (work << 8);
            right = Integer.rotateLeft(right, 1);
            work = (leftt ^ right) & 0xaaaaaaaa;
            leftt ^= work;
            right ^= work;
            leftt = Integer.rotateLeft(leftt, 1);

            int k = 0;
            for (int round = 0; round < 8; round++) {
                work = Integer.rotateRight(right, 4) ^ keys[k++];
                int fval = SP7[work & 0x3f] | SP5[(work >>> 8) & 0x3f] | SP3[(work >>> 16) & 0x3f] | SP1[(work >>> 24) & 0x3f];
                work = right ^ keys[k++];
                fval |= SP8[work & 0x3f] | SP6[(work >>> 8) & 0x3f] | SP4[(work >>> 16) & 0x3f] | SP2[(work >>> 24) & 0x3f];
                leftt ^= fval;
                work = Integer.rotateRight(leftt, 4) ^ keys[k++];
                fval = SP7[work & 0x3f] | SP5[(work >>> 8) & 0x3f] | SP3[(work >>> 16) & 0x3f] | SP1[(work >>> 24) & 0x3f];
                work = leftt ^ keys[k++];
                fval |= SP8[work & 0x3f] | SP6[(work >>> 8) & 0x3f] | SP4[(work >>> 16) & 0x3f] | SP2[(work >>> 24) & 0x3f];
                right ^= fval;
            }

            right = Integer.rotateRight(right, 1);
            work = (leftt ^ right) & 0xaaaaaaaa;
            leftt ^= work;
            right ^= work;
            leftt = Integer.rotateRight(leftt, 1);
            work = ((leftt >>> 8) ^ right) & 0x00ff00ff;
            right ^= work;
            leftt ^= (work << 8);
            work = ((leftt >>> 2) ^ right) & 0x33333333;
            right ^= work;
            leftt ^= (work << 2);
            work = ((right >>> 16) ^ leftt) & 0x0000ffff;
            leftt ^= work;
            right ^= (work << 16);
            work = ((right >>> 4) ^ leftt) & 0x0f0f0f0f;
            leftt ^= work;
            right ^= (work << 4);

            b[p] = (byte) (right >>> 24);
            b[p + 1] = (byte) (right >>> 16);
            b[p + 2] = (byte) (right >>> 8);
            b[p + 3] = (byte) right;
            b[p + 4] = (byte) (leftt >>> 24);
            b[p + 5] = (byte) (leftt >>> 16);
            b[p + 6] = (byte) (leftt >>> 8);
            b[p + 7] = (byte) leftt;
        }
    }
}
