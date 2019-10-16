/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.wmf2svg.gdi;

/**
 * @author Hidekatsu Izuno
 */
public interface GdiFont extends GdiObject {
    public static final int FW_DONTCARE = 0;
    public static final int FW_THIN = 100;
    public static final int FW_EXTRALIGHT = 200;
    public static final int FW_ULTRALIGHT = 200;
    public static final int FW_LIGHT = 300;
    public static final int FW_NORMAL = 400;
    public static final int FW_REGULAR = 400;
    public static final int FW_MEDIUM = 500;
    public static final int FW_SEMIBOLD = 600;
    public static final int FW_DEMIBOLD = 600;
    public static final int FW_BOLD = 700;
    public static final int FW_EXTRABOLD = 800;
    public static final int FW_ULTRABOLD = 800;
    public static final int FW_HEAVY = 900;
    public static final int FW_BLACK = 900;

    public static final int ANSI_CHARSET = 0;
    public static final int DEFAULT_CHARSET = 1;
    public static final int SYMBOL_CHARSET = 2;
    public static final int MAC_CHARSET = 77;
    public static final int SHIFTJIS_CHARSET = 128;
    public static final int HANGUL_CHARSET = 129;
    public static final int JOHAB_CHARSET = 130;
    public static final int GB2312_CHARSET = 134;
    public static final int CHINESEBIG5_CHARSET = 136;
    public static final int GREEK_CHARSET = 161;
    public static final int TURKISH_CHARSET = 162;
    public static final int VIETNAMESE_CHARSET = 163;
    public static final int ARABIC_CHARSET = 178;
    public static final int HEBREW_CHARSET = 177;
    public static final int BALTIC_CHARSET = 186;
    public static final int RUSSIAN_CHARSET = 204;
    public static final int THAI_CHARSET = 222;
    public static final int EASTEUROPE_CHARSET = 238;
    public static final int OEM_CHARSET = 255;

    public static final int OUT_DEFAULT_PRECIS = 0;
    public static final int OUT_STRING_PRECIS = 1;
    public static final int OUT_CHARACTER_PRECIS = 2;
    public static final int OUT_STROKE_PRECIS = 3;
    public static final int OUT_TT_PRECIS = 4;
    public static final int OUT_DEVICE_PRECIS = 5;
    public static final int OUT_RASTER_PRECIS = 6;
    public static final int OUT_TT_ONLY_PRECIS = 7;
    public static final int OUT_OUTLINE_PRECIS = 8;
    public static final int OUT_SCREEN_OUTLINE_PRECIS = 9; 

    public static final int CLIP_DEFAULT_PRECIS = 0;
    public static final int CLIP_CHARACTER_PRECIS = 1;
    public static final int CLIP_STROKE_PRECIS = 2;
    public static final int CLIP_MASK = 15;
    public static final int CLIP_LH_ANGLES = 16;
    public static final int CLIP_TT_ALWAYS = 32;
    public static final int CLIP_EMBEDDED = 128;

    public static final int DEFAULT_QUALITY = 0;
    public static final int DRAFT_QUALITY = 1;
    public static final int PROOF_QUALITY = 2;
    public static final int NONANTIALIASED_QUALITY = 3;
    public static final int ANTIALIASED_QUALITY = 4;
    public static final int CLEARTYPE_QUALITY = 5; // Windows XP only

    public static final int DEFAULT_PITCH = 0;
    public static final int FIXED_PITCH = 1;
    public static final int VARIABLE_PITCH = 2;

    public static final int FF_DONTCARE = 0;
    public static final int FF_ROMAN = 16;
    public static final int FF_SWISS = 32;
    public static final int FF_MODERN = 48;
    public static final int FF_SCRIPT = 64;
    public static final int FF_DECORATIVE = 80;
    
	public int getHeight();
	
	public int getWidth();
	
	public int getEscapement();
	
	public int getOrientation();
	
	public int getWeight();
	
	public boolean isItalic();
	
	public boolean isUnderlined();
	
	public boolean isStrikedOut();
	
	public int getCharset();
	
	public int getOutPrecision();
	
	public int getClipPrecision();
	
	public int getQuality();
	
	public int getPitchAndFamily();
	
	public String getFaceName();
}
