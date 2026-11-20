package com.foobnix.ext.chm;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How the text of a CHM is decoded. Help files were written in the Windows code page of their
 * language and rarely say which one in the page itself, so the order is: a byte order mark, the
 * page's own {@code charset}, UTF-8 when the bytes are valid UTF-8, and last the code page that
 * goes with the book's language id in its #SYSTEM file.
 */
final class ChmText {

    private static final Pattern META_CHARSET = Pattern.compile(
            "(?i)<meta[^>]+charset\\s*=\\s*[\"']?\\s*([A-Za-z0-9_.:\\-]+)");

    private ChmText() {
    }

    static String decode(final byte[] b, final int lcid) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
            return new String(b, 3, b.length - 3, Charset.forName("UTF-8"));
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
            return new String(b, 2, b.length - 2, Charset.forName("UTF-16LE"));
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
            return new String(b, 2, b.length - 2, Charset.forName("UTF-16BE"));
        }
        final String head = new String(b, 0, Math.min(b.length, 4096), Charset.forName("ISO-8859-1"));
        final Matcher m = META_CHARSET.matcher(head);
        if (m.find()) {
            final Charset declared = forName(m.group(1));
            if (declared != null && !declared.name().startsWith("UTF-16")) {
                // A page that says UTF-8 and is not is far more common than the other way round
                if (!"UTF-8".equals(declared.name()) || isUtf8(b)) {
                    return new String(b, declared);
                }
            }
        }
        if (isUtf8(b)) {
            return new String(b, Charset.forName("UTF-8"));
        }
        return new String(b, charsetOf(lcid));
    }

    private static boolean isUtf8(final byte[] b) {
        boolean high = false;
        for (byte x : b) {
            if (x < 0) {
                high = true;
                break;
            }
        }
        if (!high) {
            // Plain ASCII reads the same in every code page
            return true;
        }
        try {
            Charset.forName("UTF-8").newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static Charset forName(String name) {
        if (name == null) {
            return null;
        }
        name = name.trim().toLowerCase(Locale.US);
        // The names Windows pages use that Java does not know by that spelling
        if (name.equals("gb2312") || name.equals("x-gbk") || name.equals("cp936")) {
            name = "GBK";
        } else if (name.equals("shift-jis") || name.equals("x-sjis") || name.equals("sjis")) {
            name = "Shift_JIS";
        } else if (name.equals("ks_c_5601-1987") || name.equals("cp949")) {
            name = "EUC-KR";
        } else if (name.equals("x-cp1251")) {
            name = "windows-1251";
        }
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return null;
        }
    }

    /** The ANSI code page of a Windows language id. */
    static Charset charsetOf(final int lcid) {
        final int primary = lcid & 0x3FF;
        String name;
        switch (primary) {
            case 0x19: // Russian
            case 0x22: // Ukrainian
            case 0x23: // Belarusian
            case 0x02: // Bulgarian
            case 0x2F: // Macedonian
            case 0x44: // Tatar
            case 0x3F: // Kazakh
            case 0x50: // Mongolian
                name = "windows-1251";
                break;
            case 0x1A: // Serbian, Croatian, Bosnian
                name = (lcid == 0x0C1A || lcid == 0x1C1A) ? "windows-1251" : "windows-1250";
                break;
            case 0x05: // Czech
            case 0x15: // Polish
            case 0x0E: // Hungarian
            case 0x1B: // Slovak
            case 0x24: // Slovenian
            case 0x18: // Romanian
            case 0x1C: // Albanian
                name = "windows-1250";
                break;
            case 0x08:
                name = "windows-1253";
                break;
            case 0x1F: // Turkish
            case 0x2C: // Azeri
                name = "windows-1254";
                break;
            case 0x0D:
                name = "windows-1255";
                break;
            case 0x01: // Arabic
            case 0x29: // Farsi
            case 0x20: // Urdu
                name = "windows-1256";
                break;
            case 0x25: // Estonian
            case 0x26: // Latvian
            case 0x27: // Lithuanian
                name = "windows-1257";
                break;
            case 0x2A:
                name = "windows-1258";
                break;
            case 0x1E:
                name = "x-windows-874";
                break;
            case 0x11:
                name = "windows-31j";
                break;
            case 0x12:
                name = "x-windows-949";
                break;
            case 0x04: // Chinese: Simplified in China and Singapore, Traditional elsewhere
                name = (lcid == 0x0804 || lcid == 0x1004) ? "GBK" : "Big5";
                break;
            default:
                name = "windows-1252";
        }
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            try {
                return Charset.forName("windows-1252");
            } catch (Exception e2) {
                return Charset.forName("ISO-8859-1");
            }
        }
    }
}
