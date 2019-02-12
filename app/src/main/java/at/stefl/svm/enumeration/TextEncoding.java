package at.stefl.svm.enumeration;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

import at.stefl.commons.util.collection.CollectionUtil;
import at.stefl.commons.util.object.ObjectTransformer;

// TODO: fix unsupported
public enum TextEncoding {
    
    DONTKNOW(TextEncodingConstants.RTL_TEXTENCODING_DONTKNOW),
    MS_1252(TextEncodingConstants.RTL_TEXTENCODING_MS_1252, "windows-1252"),
    APPLE_ROMAN(TextEncodingConstants.RTL_TEXTENCODING_APPLE_ROMAN,
            "x-MacRoman"),
    IBM_437(TextEncodingConstants.RTL_TEXTENCODING_IBM_437, "IBM437"),
    IBM_850(TextEncodingConstants.RTL_TEXTENCODING_IBM_850, "IBM850"),
    IBM_860(TextEncodingConstants.RTL_TEXTENCODING_IBM_860, "IBM860"),
    IBM_861(TextEncodingConstants.RTL_TEXTENCODING_IBM_861, "IBM861"),
    IBM_863(TextEncodingConstants.RTL_TEXTENCODING_IBM_863, "IBM863"),
    IBM_865(TextEncodingConstants.RTL_TEXTENCODING_IBM_865, "IBM865"),
    /* Reserved: SYSTEM(TextEncodingConstants.RTL_TEXTENCODING_SYSTEM), */
    SYMBOL(TextEncodingConstants.RTL_TEXTENCODING_SYMBOL, "Symbol"),
    ASCII_US(TextEncodingConstants.RTL_TEXTENCODING_ASCII_US, "US-ASCII"),
    ISO_8859_1(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_1, "ISO-8859-1"),
    ISO_8859_2(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_2, "ISO-8859-2"),
    ISO_8859_3(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_3, "ISO-8859-3"),
    ISO_8859_4(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_4, "ISO-8859-4"),
    ISO_8859_5(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_5, "ISO-8859-5"),
    ISO_8859_6(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_6, "ISO-8859-6"),
    ISO_8859_7(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_7, "ISO-8859-7"),
    ISO_8859_8(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_8, "ISO-8859-8"),
    ISO_8859_9(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_9, "ISO-8859-9"),
    ISO_8859_14(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_14,
            "ISO-8859-14"),
    ISO_8859_15(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_15,
            "ISO-8859-15"),
    IBM_737(TextEncodingConstants.RTL_TEXTENCODING_IBM_737, "IBM-737"),
    IBM_775(TextEncodingConstants.RTL_TEXTENCODING_IBM_775, "IBM-775"),
    IBM_852(TextEncodingConstants.RTL_TEXTENCODING_IBM_852, "IBM-852"),
    IBM_855(TextEncodingConstants.RTL_TEXTENCODING_IBM_855, "IBM-855"),
    IBM_857(TextEncodingConstants.RTL_TEXTENCODING_IBM_857, "IBM-857"),
    IBM_862(TextEncodingConstants.RTL_TEXTENCODING_IBM_862, "IBM-862"),
    IBM_864(TextEncodingConstants.RTL_TEXTENCODING_IBM_864, "IBM-864"),
    IBM_866(TextEncodingConstants.RTL_TEXTENCODING_IBM_866, "IBM-866"),
    IBM_869(TextEncodingConstants.RTL_TEXTENCODING_IBM_869, "IBM-869"),
    MS_874(TextEncodingConstants.RTL_TEXTENCODING_MS_874, "x-windows-874"),
    MS_1250(TextEncodingConstants.RTL_TEXTENCODING_MS_1250, "windows-1250"),
    MS_1251(TextEncodingConstants.RTL_TEXTENCODING_MS_1251, "windows-1251"),
    MS_1253(TextEncodingConstants.RTL_TEXTENCODING_MS_1253, "windows-1253"),
    MS_1254(TextEncodingConstants.RTL_TEXTENCODING_MS_1254, "windows-1254"),
    MS_1255(TextEncodingConstants.RTL_TEXTENCODING_MS_1255, "windows-1255"),
    MS_1256(TextEncodingConstants.RTL_TEXTENCODING_MS_1256, "windows-1256"),
    MS_1257(TextEncodingConstants.RTL_TEXTENCODING_MS_1257, "windows-1257"),
    MS_1258(TextEncodingConstants.RTL_TEXTENCODING_MS_1258, "windows-1258"),
    APPLE_ARABIC(TextEncodingConstants.RTL_TEXTENCODING_APPLE_ARABIC,
            "x-MacArabic"),
    APPLE_CENTEURO(TextEncodingConstants.RTL_TEXTENCODING_APPLE_CENTEURO,
            "x-MacCentralEurope"),
    APPLE_CROATIAN(TextEncodingConstants.RTL_TEXTENCODING_APPLE_CROATIAN,
            "x-MacCroatian"),
    APPLE_CYRILLIC(TextEncodingConstants.RTL_TEXTENCODING_APPLE_CYRILLIC,
            "x-MacCyrillic"),
    APPLE_DEVANAGARI(TextEncodingConstants.RTL_TEXTENCODING_APPLE_DEVANAGARI,
            "x-MacDevanagari"),
    APPLE_FARSI(TextEncodingConstants.RTL_TEXTENCODING_APPLE_FARSI,
            "x-MacFarsi"),
    APPLE_GREEK(TextEncodingConstants.RTL_TEXTENCODING_APPLE_GREEK,
            "x-MacGreek"),
    APPLE_GUJARATI(TextEncodingConstants.RTL_TEXTENCODING_APPLE_GUJARATI,
            "x-MacGujarati"),
    APPLE_GURMUKHI(TextEncodingConstants.RTL_TEXTENCODING_APPLE_GURMUKHI,
            "x-MacGurmukhi"),
    APPLE_HEBREW(TextEncodingConstants.RTL_TEXTENCODING_APPLE_HEBREW,
            "x-MacHebrew"),
    APPLE_ICELAND(TextEncodingConstants.RTL_TEXTENCODING_APPLE_ICELAND,
            "x-MacIceland"),
    APPLE_ROMANIAN(TextEncodingConstants.RTL_TEXTENCODING_APPLE_ROMANIAN,
            "x-MacRomania"),
    APPLE_THAI(TextEncodingConstants.RTL_TEXTENCODING_APPLE_THAI, "x-MacThai"),
    APPLE_TURKISH(TextEncodingConstants.RTL_TEXTENCODING_APPLE_TURKISH,
            "x-MacTurkish"),
    APPLE_UKRAINIAN(TextEncodingConstants.RTL_TEXTENCODING_APPLE_UKRAINIAN,
            "x-MacUkraine"),
    APPLE_CHINSIMP(TextEncodingConstants.RTL_TEXTENCODING_APPLE_CHINSIMP,
            "x-MacChinsimp"),
    APPLE_CHINTRAD(TextEncodingConstants.RTL_TEXTENCODING_APPLE_CHINTRAD,
            "x-MacChintrad"),
    APPLE_JAPANESE(TextEncodingConstants.RTL_TEXTENCODING_APPLE_JAPANESE,
            "x-MacJapanese"),
    APPLE_KOREAN(TextEncodingConstants.RTL_TEXTENCODING_APPLE_KOREAN,
            "x-MacKorean"),
    MS_932(TextEncodingConstants.RTL_TEXTENCODING_MS_932, "x-windows-932"),
    MS_936(TextEncodingConstants.RTL_TEXTENCODING_MS_936, "x-windows-936"),
    MS_949(TextEncodingConstants.RTL_TEXTENCODING_MS_949, "x-windows-949"),
    MS_950(TextEncodingConstants.RTL_TEXTENCODING_MS_950, "x-windows-950"),
    SHIFT_JIS(TextEncodingConstants.RTL_TEXTENCODING_SHIFT_JIS, "Shift_JIS"),
    GB_2312(TextEncodingConstants.RTL_TEXTENCODING_GB_2312, "GB2312"),
    GBT_12345(TextEncodingConstants.RTL_TEXTENCODING_GBT_12345, "GB12345"),
    GBK(TextEncodingConstants.RTL_TEXTENCODING_GBK, "GBK"),
    BIG5(TextEncodingConstants.RTL_TEXTENCODING_BIG5, "Big5"),
    EUC_JP(TextEncodingConstants.RTL_TEXTENCODING_EUC_JP, "EUC-JP"),
    EUC_CN(TextEncodingConstants.RTL_TEXTENCODING_EUC_CN, "GB2312"),
    EUC_TW(TextEncodingConstants.RTL_TEXTENCODING_EUC_TW, "x-EUC-TW"),
    ISO_2022_JP(TextEncodingConstants.RTL_TEXTENCODING_ISO_2022_JP,
            "ISO-2022-JP"),
    ISO_2022_CN(TextEncodingConstants.RTL_TEXTENCODING_ISO_2022_CN,
            "ISO-2022-CN"),
    KOI8_R(TextEncodingConstants.RTL_TEXTENCODING_KOI8_R, "KOI8_R"),
    UTF7(TextEncodingConstants.RTL_TEXTENCODING_UTF7, "UTF-7"),
    UTF8(TextEncodingConstants.RTL_TEXTENCODING_UTF8, "UTF-8"),
    ISO_8859_10(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_10,
            "ISO-8859-10"),
    ISO_8859_13(TextEncodingConstants.RTL_TEXTENCODING_ISO_8859_13,
            "ISO-8859-13"),
    EUC_KR(TextEncodingConstants.RTL_TEXTENCODING_EUC_KR, "EUC-KR"),
    ISO_2022_KR(TextEncodingConstants.RTL_TEXTENCODING_ISO_2022_KR,
            "ISO-2022-KR"),
    JIS_X_0201(TextEncodingConstants.RTL_TEXTENCODING_JIS_X_0201, "EUC-JP"),
    JIS_X_0208(TextEncodingConstants.RTL_TEXTENCODING_JIS_X_0208, "EUC-JP"),
    JIS_X_0212(TextEncodingConstants.RTL_TEXTENCODING_JIS_X_0212, "EUC-JP"),
    MS_1361(TextEncodingConstants.RTL_TEXTENCODING_MS_1361, "windows-1361"),
    GB_18030(TextEncodingConstants.RTL_TEXTENCODING_GB_18030, "GB18030"),
    BIG5_HKSCS(TextEncodingConstants.RTL_TEXTENCODING_BIG5_HKSCS, "Big5-HKSCS"),
    TIS_620(TextEncodingConstants.RTL_TEXTENCODING_TIS_620, "TIS-620"),
    KOI8_U(TextEncodingConstants.RTL_TEXTENCODING_KOI8_U, "KOI8-U"),
    ISCII_DEVANAGARI(TextEncodingConstants.RTL_TEXTENCODING_ISCII_DEVANAGARI,
            "x-ISCII91"), // sure?
    JAVA_UTF8(TextEncodingConstants.RTL_TEXTENCODING_JAVA_UTF8, "UTF-8"),
    ADOBE_STANDARD(TextEncodingConstants.RTL_TEXTENCODING_ADOBE_STANDARD,
            "adobe-standard"), // ?
    ADOBE_SYMBOL(TextEncodingConstants.RTL_TEXTENCODING_ADOBE_SYMBOL,
            "adobe-symbol"), // ?
    PT154(TextEncodingConstants.RTL_TEXTENCODING_PT154, "PT154"), // ?
    ADOBE_DINGBATS(TextEncodingConstants.RTL_TEXTENCODING_ADOBE_DINGBATS,
            "adobe-dingbats"), USER_START(
            TextEncodingConstants.RTL_TEXTENCODING_USER_START), USER_END(
            TextEncodingConstants.RTL_TEXTENCODING_USER_END), UCS4(
            TextEncodingConstants.RTL_TEXTENCODING_UCS4, "UTF-32"), UCS2(
            TextEncodingConstants.RTL_TEXTENCODING_UCS2, "UTF-16");
    
    private static final ObjectTransformer<TextEncoding, Integer> CODE_KEY_GENERATOR = new ObjectTransformer<TextEncoding, Integer>() {
        
        @Override
        public Integer transform(TextEncoding value) {
            return value.code;
        }
    };
    
    private static final ObjectTransformer<TextEncoding, Charset> CHARSET_KEY_GENERATOR = new ObjectTransformer<TextEncoding, Charset>() {
        
        @Override
        public Charset transform(TextEncoding value) {
            return value.charset;
        }
    };
    
    private static final Map<Integer, TextEncoding> BY_CODE_MAP;
    private static final Map<Charset, TextEncoding> BY_CHARSET_MAP;
    
    static {
        BY_CODE_MAP = CollectionUtil.toHashMap(CODE_KEY_GENERATOR, values());
        BY_CHARSET_MAP = CollectionUtil.toHashMapNotNull(CHARSET_KEY_GENERATOR,
                values());
    }
    
    public static TextEncoding getByCode(int code) {
        return BY_CODE_MAP.get(code);
    }
    
    public static TextEncoding getByCharset(Charset charset) {
        if (charset == null) throw new NullPointerException();
        return BY_CHARSET_MAP.get(charset);
    }
    
    private final int code;
    private final Charset charset;
    private final boolean isSupported;
    
    private TextEncoding(int code) {
        this.code = code;
        this.charset = null;
        this.isSupported = false;
    }
    
    private TextEncoding(int code, String charsetName) {
        if (charsetName == null) throw new NullPointerException();
        
        this.code = code;
        
        Charset charset = null;
        boolean isSupported = false;
        
        try {
            charset = Charset.forName(charsetName);
            isSupported = true;
        } catch (UnsupportedCharsetException e) {
        }
        
        this.charset = charset;
        this.isSupported = isSupported;
    }
    
    private TextEncoding(int code, TextEncoding encoding) {
        this.code = code;
        this.charset = encoding.charset;
        this.isSupported = encoding.isSupported;
    }
    
    public int getCode() {
        return code;
    }
    
    public Charset getCharset() {
        return charset;
    }
    
    public boolean isSupported() {
        return isSupported;
    }
    
    public boolean hasCharset() {
        return charset != null;
    }
    
}