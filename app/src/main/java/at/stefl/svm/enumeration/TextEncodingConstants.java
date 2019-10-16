package at.stefl.svm.enumeration;

public class TextEncodingConstants {
    
    /******
     * Overview over the TextEncodings ***** # Arabic (Apple Macintosh)
     * RTL_TEXTENCODING_APPLE_ARABIC Arabic (DOS/OS2-864)
     * RTL_TEXTENCODING_IBM_864 Arabic (ISO-8859-6) RTL_TEXTENCODING_ISO_8859_6
     * Arabic (Windows-1256) RTL_TEXTENCODING_MS_1256
     * Baltic (DOS/OS2-775) RTL_TEXTENCODING_IBM_775 Baltic (ISO-8859-4)
     * RTL_TEXTENCODING_ISO_8859_4 Baltic (Windows-1257)
     * RTL_TEXTENCODING_MS_1257
     * Central European (Apple Macintosh) RTL_TEXTENCODING_APPLE_CENTEURO
     * Central European (Apple Macintosh/Croatian)
     * RTL_TEXTENCODING_APPLE_CROATIAN Central European (Apple
     * Macintosh/Romanian) RTL_TEXTENCODING_APPLE_ROMANIAN Central European
     * (DOS/OS2-852) RTL_TEXTENCODING_IBM_852 Central European (ISO-8859-2)
     * RTL_TEXTENCODING_ISO_8859_2 Central European (ISO-8859-10)
     * RTL_TEXTENCODING_ISO_8859_10 Central European (ISO-8859-13)
     * RTL_TEXTENCODING_ISO_8859_13 Central European (Windows-1250/WinLatin 2)
     * RTL_TEXTENCODING_MS_1250
     * Chinese Simplified (Apple Macintosh) RTL_TEXTENCODING_APPLE_CHINSIMP
     * Chinese Simplified (EUC-CN) RTL_TEXTENCODING_EUC_CN Chinese Simplified
     * (GB-2312) RTL_TEXTENCODING_GB_2312 Chinese Simplified (GBK/GB-2312-80)
     * RTL_TEXTENCODING_GBK # Chinese Simplified (ISO-2022-CN)
     * RTL_TEXTENCODING_ISO_2022_CN Chinese Simplified (Windows-936)
     * RTL_TEXTENCODING_MS_936 # Chinese Simplified (GB-18030)
     * RTL_TEXTENCODING_GB_18030
     * Chinese Traditional (Apple Macintosh) RTL_TEXTENCODING_APPLE_CHINTRAD
     * Chinese Traditional (BIG5) RTL_TEXTENCODING_BIG5 # Chinese Traditional
     * (EUC-TW) RTL_TEXTENCODING_EUC_TW Chinese Traditional (GBT-12345)
     * RTL_TEXTENCODING_GBT_12345 Chinese Traditional (Windows-950)
     * RTL_TEXTENCODING_MS_950 Chinese Traditional (BIG5-HKSCS)
     * RTL_TEXTENCODING_BIG5_HKSCS
     * Cyrillic (Apple Macintosh) RTL_TEXTENCODING_APPLE_CYRILLIC Cyrillic
     * (Apple Macintosh/Ukrainian) RTL_TEXTENCODING_APPLE_UKRAINIAN Cyrillic
     * (DOS/OS2-855) RTL_TEXTENCODING_IBM_855 Cyrillic (DOS/OS2-866/Russian)
     * RTL_TEXTENCODING_IBM_866 Cyrillic (ISO-8859-5)
     * RTL_TEXTENCODING_ISO_8859_5 Cyrillic (KOI8-R) RTL_TEXTENCODING_KOI8_R
     * Cyrillic (KOI8-U) RTL_TEXTENCODING_KOI8_U Cyrillic (Windows-1251)
     * RTL_TEXTENCODING_MS_1251
     * Greek (Apple Macintosh) RTL_TEXTENCODING_APPLE_GREEK Greek (DOS/OS2-737)
     * RTL_TEXTENCODING_IBM_737 Greek (DOS/OS2-869/Modern)
     * RTL_TEXTENCODING_IBM_869 Greek (ISO-8859-7) RTL_TEXTENCODING_ISO_8859_7
     * Greek (Windows-1253) RTL_TEXTENCODING_MS_1253
     * # Hebrew (Apple Macintosh) RTL_TEXTENCODING_APPLE_HEBREW Hebrew
     * (DOS/OS2-862) RTL_TEXTENCODING_IBM_862 Hebrew (ISO-8859-8)
     * RTL_TEXTENCODING_ISO_8859_8 Hebrew (Windows-1255)
     * RTL_TEXTENCODING_MS_1255
     * Korean (Apple Macintosh) RTL_TEXTENCODING_APPLE_KOREAN Korean (EUC-KR)
     * RTL_TEXTENCODING_EUC_KR # Korean (ISO-2022-KR)
     * RTL_TEXTENCODING_ISO_2022_KR Korean (Windows-Wansung-949)
     * RTL_TEXTENCODING_MS_949 Korean (Windows-Johab-1361)
     * RTL_TEXTENCODING_MS_1361
     * Latin 3 (ISO-8859-3) RTL_TEXTENCODING_ISO_8859_3
     * Indian (ISCII Devanagari) RTL_TEXTENCODING_ISCII_DEVANAGARI
     * Japanese (Apple Macintosh) RTL_TEXTENCODING_APPLE_JAPANESE Japanese
     * (EUC-JP) RTL_TEXTENCODING_EUC_JP # Japanese (ISO-2022-JP)
     * RTL_TEXTENCODING_ISO_2022_JP Japanese (Shift-JIS)
     * RTL_TEXTENCODING_SHIFT_JIS Japanese (Windows-932) RTL_TEXTENCODING_MS_932
     * Symbol RTL_TEXTENCODING_SYMBOL
     * # Thai (Apple Macintosh) RTL_TEXTENCODING_APPLE_THAI Thai
     * (Dos/Windows-874) RTL_TEXTENCODING_MS_874 Thai (TIS 620)
     * RTL_TEXTENCODING_TIS_620
     * Turkish (Apple Macintosh) RTL_TEXTENCODING_APPLE_TURKISH Turkish
     * (DOS/OS2-857) RTL_TEXTENCODING_IBM_857 Turkish (ISO-8859-9)
     * RTL_TEXTENCODING_ISO_8859_9 Turkish (Windows-1254)
     * RTL_TEXTENCODING_MS_1254
     * Unicode (UTF-7) RTL_TEXTENCODING_UTF7 Unicode (UTF-8)
     * RTL_TEXTENCODING_UTF8 Unicode (Java's modified UTF-8)
     * RTL_TEXTENCODING_JAVA_UTF8
     * Vietnamese (Windows-1258) RTL_TEXTENCODING_MS_1258
     * Western (Apple Macintosh) RTL_TEXTENCODING_APPLE_ROMAN Western (Apple
     * Macintosh/Icelandic) RTL_TEXTENCODING_APPLE_ICELAND Western (ASCII/US)
     * RTL_TEXTENCODING_ASCII_US Western (DOS/OS2-437/US)
     * RTL_TEXTENCODING_IBM_437 Western (DOS/OS2-850/International)
     * RTL_TEXTENCODING_IBM_850 Western (DOS/OS2-860/Portugese)
     * RTL_TEXTENCODING_IBM_860 Western (DOS/OS2-861/Icelandic)
     * RTL_TEXTENCODING_IBM_861 Western (DOS/OS2-863/Canadian-French)
     * RTL_TEXTENCODING_IBM_863 Western (DOS/OS2-865/Nordic)
     * RTL_TEXTENCODING_IBM_865 Western (ISO-8859-1) RTL_TEXTENCODING_ISO_8859_1
     * Western (ISO-8859-14) RTL_TEXTENCODING_ISO_8859_14 Western
     * (ISO-8859-15/EURO) RTL_TEXTENCODING_ISO_8859_15 Western
     * (Window-1252/WinLatin 1) RTL_TEXTENCODING_MS_1252
     * Not known and currently not supported # RTL_TEXTENCODING_APPLE_DEVANAGARI
     * # RTL_TEXTENCODING_APPLE_FARSI # RTL_TEXTENCODING_APPLE_GUJARATI #
     * RTL_TEXTENCODING_APPLE_GURMUKHI
     * Only for internal implementations and not useful for user interface.
     * These encodings are not used for text encodings, only used for
     * font-/textoutput encodings. Japanese (JIS 0201)
     * RTL_TEXTENCODING_JISX_0201 Japanese (JIS 0208) RTL_TEXTENCODING_JISX_0208
     * Japanese (JIS 0212) RTL_TEXTENCODING_JISX_0212
     * # Currently not implemented
     */
    
    public static final int RTL_TEXTENCODING_DONTKNOW = 0;
    public static final int RTL_TEXTENCODING_MS_1252 = 1;
    public static final int RTL_TEXTENCODING_APPLE_ROMAN = 2;
    public static final int RTL_TEXTENCODING_IBM_437 = 3;
    public static final int RTL_TEXTENCODING_IBM_850 = 4;
    public static final int RTL_TEXTENCODING_IBM_860 = 5;
    public static final int RTL_TEXTENCODING_IBM_861 = 6;
    public static final int RTL_TEXTENCODING_IBM_863 = 7;
    public static final int RTL_TEXTENCODING_IBM_865 = 8;
    /* Reserved: public static final int RTL_TEXTENCODING_SYSTEM = 9; */
    public static final int RTL_TEXTENCODING_SYMBOL = 10;
    public static final int RTL_TEXTENCODING_ASCII_US = 11;
    public static final int RTL_TEXTENCODING_ISO_8859_1 = 12;
    public static final int RTL_TEXTENCODING_ISO_8859_2 = 13;
    public static final int RTL_TEXTENCODING_ISO_8859_3 = 14;
    public static final int RTL_TEXTENCODING_ISO_8859_4 = 15;
    public static final int RTL_TEXTENCODING_ISO_8859_5 = 16;
    public static final int RTL_TEXTENCODING_ISO_8859_6 = 17;
    public static final int RTL_TEXTENCODING_ISO_8859_7 = 18;
    public static final int RTL_TEXTENCODING_ISO_8859_8 = 19;
    public static final int RTL_TEXTENCODING_ISO_8859_9 = 20;
    public static final int RTL_TEXTENCODING_ISO_8859_14 = 21;
    public static final int RTL_TEXTENCODING_ISO_8859_15 = 22;
    public static final int RTL_TEXTENCODING_IBM_737 = 23;
    public static final int RTL_TEXTENCODING_IBM_775 = 24;
    public static final int RTL_TEXTENCODING_IBM_852 = 25;
    public static final int RTL_TEXTENCODING_IBM_855 = 26;
    public static final int RTL_TEXTENCODING_IBM_857 = 27;
    public static final int RTL_TEXTENCODING_IBM_862 = 28;
    public static final int RTL_TEXTENCODING_IBM_864 = 29;
    public static final int RTL_TEXTENCODING_IBM_866 = 30;
    public static final int RTL_TEXTENCODING_IBM_869 = 31;
    public static final int RTL_TEXTENCODING_MS_874 = 32;
    public static final int RTL_TEXTENCODING_MS_1250 = 33;
    public static final int RTL_TEXTENCODING_MS_1251 = 34;
    public static final int RTL_TEXTENCODING_MS_1253 = 35;
    public static final int RTL_TEXTENCODING_MS_1254 = 36;
    public static final int RTL_TEXTENCODING_MS_1255 = 37;
    public static final int RTL_TEXTENCODING_MS_1256 = 38;
    public static final int RTL_TEXTENCODING_MS_1257 = 39;
    public static final int RTL_TEXTENCODING_MS_1258 = 40;
    public static final int RTL_TEXTENCODING_APPLE_ARABIC = 41;
    public static final int RTL_TEXTENCODING_APPLE_CENTEURO = 42;
    public static final int RTL_TEXTENCODING_APPLE_CROATIAN = 43;
    public static final int RTL_TEXTENCODING_APPLE_CYRILLIC = 44;
    public static final int RTL_TEXTENCODING_APPLE_DEVANAGARI = 45;
    public static final int RTL_TEXTENCODING_APPLE_FARSI = 46;
    public static final int RTL_TEXTENCODING_APPLE_GREEK = 47;
    public static final int RTL_TEXTENCODING_APPLE_GUJARATI = 48;
    public static final int RTL_TEXTENCODING_APPLE_GURMUKHI = 49;
    public static final int RTL_TEXTENCODING_APPLE_HEBREW = 50;
    public static final int RTL_TEXTENCODING_APPLE_ICELAND = 51;
    public static final int RTL_TEXTENCODING_APPLE_ROMANIAN = 52;
    public static final int RTL_TEXTENCODING_APPLE_THAI = 53;
    public static final int RTL_TEXTENCODING_APPLE_TURKISH = 54;
    public static final int RTL_TEXTENCODING_APPLE_UKRAINIAN = 55;
    public static final int RTL_TEXTENCODING_APPLE_CHINSIMP = 56;
    public static final int RTL_TEXTENCODING_APPLE_CHINTRAD = 57;
    public static final int RTL_TEXTENCODING_APPLE_JAPANESE = 58;
    public static final int RTL_TEXTENCODING_APPLE_KOREAN = 59;
    public static final int RTL_TEXTENCODING_MS_932 = 60;
    public static final int RTL_TEXTENCODING_MS_936 = 61;
    public static final int RTL_TEXTENCODING_MS_949 = 62;
    public static final int RTL_TEXTENCODING_MS_950 = 63;
    public static final int RTL_TEXTENCODING_SHIFT_JIS = 64;
    public static final int RTL_TEXTENCODING_GB_2312 = 65;
    public static final int RTL_TEXTENCODING_GBT_12345 = 66;
    public static final int RTL_TEXTENCODING_GBK = 67;
    public static final int RTL_TEXTENCODING_BIG5 = 68;
    public static final int RTL_TEXTENCODING_EUC_JP = 69;
    public static final int RTL_TEXTENCODING_EUC_CN = 70;
    public static final int RTL_TEXTENCODING_EUC_TW = 71;
    public static final int RTL_TEXTENCODING_ISO_2022_JP = 72;
    public static final int RTL_TEXTENCODING_ISO_2022_CN = 73;
    public static final int RTL_TEXTENCODING_KOI8_R = 74;
    public static final int RTL_TEXTENCODING_UTF7 = 75;
    public static final int RTL_TEXTENCODING_UTF8 = 76;
    public static final int RTL_TEXTENCODING_ISO_8859_10 = 77;
    public static final int RTL_TEXTENCODING_ISO_8859_13 = 78;
    public static final int RTL_TEXTENCODING_EUC_KR = 79;
    public static final int RTL_TEXTENCODING_ISO_2022_KR = 80;
    public static final int RTL_TEXTENCODING_JIS_X_0201 = 81;
    public static final int RTL_TEXTENCODING_JIS_X_0208 = 82;
    public static final int RTL_TEXTENCODING_JIS_X_0212 = 83;
    public static final int RTL_TEXTENCODING_MS_1361 = 84;
    public static final int RTL_TEXTENCODING_GB_18030 = 85;
    public static final int RTL_TEXTENCODING_BIG5_HKSCS = 86;
    public static final int RTL_TEXTENCODING_TIS_620 = 87;
    public static final int RTL_TEXTENCODING_KOI8_U = 88;
    public static final int RTL_TEXTENCODING_ISCII_DEVANAGARI = 89;
    public static final int RTL_TEXTENCODING_JAVA_UTF8 = 90;
    public static final int RTL_TEXTENCODING_ADOBE_STANDARD = 91;
    public static final int RTL_TEXTENCODING_ADOBE_SYMBOL = 92;
    public static final int RTL_TEXTENCODING_PT154 = 93;
    public static final int RTL_TEXTENCODING_ADOBE_DINGBATS = 94;
    
    public static final int RTL_TEXTENCODING_USER_START = 0x8000;
    public static final int RTL_TEXTENCODING_USER_END = 0xEFFF;
    
    public static final int RTL_TEXTENCODING_UCS4 = 0xFFFE;
    public static final int RTL_TEXTENCODING_UCS2 = 0xFFFF;
    
    private TextEncodingConstants() {}
    
}