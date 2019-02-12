package net.arnx.wmf2svg.gdi;

import java.io.UnsupportedEncodingException;

public final class GdiUtils {
	public static String convertString(byte[] chars, int charset) {
		String str = null;

		int length = 0;
		while (length < chars.length && chars[length] != 0) {
			length++;
		}
		
		try {
			str = new String(chars, 0, length, getCharset(charset));
		} catch (UnsupportedEncodingException e) {
			try {
				str = new String(chars, 0, length, "US-ASCII");
			} catch (UnsupportedEncodingException e2) {
				throw new IllegalStateException(e2);
			}
		}
		return str;
	}
	
	public static String getCharset(int charset) {
		switch (charset) {
		case GdiFont.ANSI_CHARSET:
			return "Cp1252";
		case GdiFont.SYMBOL_CHARSET:
			return "Cp1252";
		case GdiFont.MAC_CHARSET:
			return "MacRoman";
		case GdiFont.SHIFTJIS_CHARSET:
			return "MS932";
		case GdiFont.HANGUL_CHARSET:
			return "MS949";
		case GdiFont.JOHAB_CHARSET:
			return "Johab";
		case GdiFont.GB2312_CHARSET:
			return "MS936";
		case GdiFont.CHINESEBIG5_CHARSET:
			return "MS950";
		case GdiFont.GREEK_CHARSET:
			return "Cp1253";
		case GdiFont.TURKISH_CHARSET:
			return "Cp1254";
		case GdiFont.VIETNAMESE_CHARSET:
			return "Cp1258";
		case GdiFont.HEBREW_CHARSET:
			return "Cp1255";
		case GdiFont.ARABIC_CHARSET:
			return "Cp1256";
		case GdiFont.BALTIC_CHARSET:
			return "Cp1257";
		case GdiFont.RUSSIAN_CHARSET:
			return "Cp1251";
		case GdiFont.THAI_CHARSET:
			return "MS874";
		case GdiFont.EASTEUROPE_CHARSET:
			return "Cp1250";
		case GdiFont.OEM_CHARSET:
			return "Cp1252";
		default:
			return "Cp1252";
		}
	}
	
	public static String getLanguage(int charset) {
		switch (charset) {
		case GdiFont.ANSI_CHARSET:
			return "en";
		case GdiFont.SYMBOL_CHARSET:
			return "en";
		case GdiFont.MAC_CHARSET:
			return "en";
		case GdiFont.SHIFTJIS_CHARSET:
			return "ja";
		case GdiFont.HANGUL_CHARSET:
			return "ko";
		case GdiFont.JOHAB_CHARSET:
			return "ko";
		case GdiFont.GB2312_CHARSET:
			return "zh-CN";
		case GdiFont.CHINESEBIG5_CHARSET:
			return "zh-TW";
		case GdiFont.GREEK_CHARSET:
			return "el";
		case GdiFont.TURKISH_CHARSET:
			return "tr";
		case GdiFont.VIETNAMESE_CHARSET:
			return "vi";
		case GdiFont.HEBREW_CHARSET:
			return "iw";
		case GdiFont.ARABIC_CHARSET:
			return "ar";
		case GdiFont.BALTIC_CHARSET:
			return "bat";
		case GdiFont.RUSSIAN_CHARSET:
			return "ru";
		case GdiFont.THAI_CHARSET:
			return "th";
		case GdiFont.EASTEUROPE_CHARSET:
			return null;
		case GdiFont.OEM_CHARSET:
			return null;
		default:
			return null;
		}		
	}
	
	private static int[][] FBA_SHIFT_JIS = {{0x81, 0x9F}, {0xE0, 0xFC}};
	private static int[][] FBA_HANGUL_CHARSET = {{0x80, 0xFF}};
	private static int[][] FBA_JOHAB_CHARSET = {{0x80, 0xFF}};
	private static int[][] FBA_GB2312_CHARSET = {{0x80, 0xFF}};
	private static int[][] FBA_CHINESEBIG5_CHARSET = {{0xA1, 0xFE}};
	
	public static int[][] getFirstByteArea(int charset) {
		switch (charset) {
		case GdiFont.SHIFTJIS_CHARSET:
			return FBA_SHIFT_JIS;
		case GdiFont.HANGUL_CHARSET:
			return FBA_HANGUL_CHARSET;
		case GdiFont.JOHAB_CHARSET:
			return FBA_JOHAB_CHARSET;
		case GdiFont.GB2312_CHARSET:
			return FBA_GB2312_CHARSET;
		case GdiFont.CHINESEBIG5_CHARSET:
			return FBA_CHINESEBIG5_CHARSET;
		default:
			return null;
		}		
	}
}
