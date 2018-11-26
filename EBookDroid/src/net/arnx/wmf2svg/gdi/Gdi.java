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
public interface Gdi {
    public static final int OPAQUE = 2;
    public static final int TRANSPARENT = 1;

    public static final int TA_BASELINE = 24;
    public static final int TA_BOTTOM = 8;
    public static final int TA_TOP = 0;
    public static final int TA_CENTER = 6;
    public static final int TA_LEFT = 0;
    public static final int TA_RIGHT = 2;
    public static final int TA_NOUPDATECP = 0;
    public static final int TA_RTLREADING = 256;
    public static final int TA_UPDATECP = 1;
    public static final int VTA_BASELINE = 24;
    public static final int VTA_CENTER = 6;

    public static final int ETO_CLIPPED = 4;
    public static final int ETO_NUMERICSLOCAL = 1024;
    public static final int ETO_NUMERICSLATIN = 2048;
    public static final int ETO_GLYPH_INDEX = 16;
    public static final int ETO_OPAQUE = 2;
    public static final int ETO_PDY = 8192;
    public static final int ETO_RTLREADING = 128;
    public static final int ETO_IGNORELANGUAGE = 4096;

    public static final int MM_ANISOTROPIC = 8;
    public static final int MM_HIENGLISH = 5;
    public static final int MM_HIMETRIC = 3;
    public static final int MM_ISOTROPIC = 7;
    public static final int MM_LOENGLISH = 4;
    public static final int MM_LOMETRIC = 2;
    public static final int MM_TEXT = 1;
    public static final int MM_TWIPS = 6;

    public static final int STRETCH_ANDSCANS = 2;
    public static final int STRETCH_DELETESCANS = 3;
    public static final int STRETCH_HALFTONE = 4;
    public static final int STRETCH_ORSCANS = 2;
    public static final int BLACKONWHITE = 2;
    public static final int COLORONCOLOR = 3;
    public static final int HALFTONE = 4;
    public static final int WHITEONBLACK = 2;

    public static final int ALTERNATE = 1;
    public static final int WINDING = 2;

    public static final int R2_BLACK = 1;
    public static final int R2_COPYPEN = 13;
    public static final int R2_MASKNOTPEN = 3;
    public static final int R2_MASKPEN = 9;
    public static final int R2_MASKPENNOT = 5;
    public static final int R2_MERGENOTPEN = 12;
    public static final int R2_MERGEPEN = 15;
    public static final int R2_MERGEPENNOT = 14;
    public static final int R2_NOP = 11;
    public static final int R2_NOT = 6;
    public static final int R2_NOTCOPYPEN = 4;
    public static final int R2_NOTMASKPEN = 8;
    public static final int R2_NOTMERGEPEN = 2;
    public static final int R2_NOTXORPEN = 10;
    public static final int R2_WHITE = 16;
    public static final int R2_XORPEN = 7;
    
	public static final long BLACKNESS = 66;
	public static final long DSTINVERT = 5570569;
	public static final long MERGECOPY = 12583114;
	public static final long MERGEPAINT = 12255782;
	public static final long NOTSRCCOPY = 3342344;
	public static final long NOTSRCERASE = 1114278;
	public static final long PATCOPY = 15728673;
	public static final long PATINVERT = 5898313;
	public static final long PATPAINT = 16452105;
	public static final long SRCAND = 8913094;
	public static final long SRCCOPY = 13369376;
	public static final long SRCERASE = 4457256;
	public static final long SRCINVERT = 6684742;
	public static final long SRCPAINT = 15597702;
	public static final long WHITENESS = 16711778;
	
	public static final int DIB_RGB_COLORS = 0;
	public static final int DIB_PAL_COLORS = 1;
	
	public static final int LAYOUT_BITMAPORIENTATIONPRESERVED = 8;
	public static final int LAYOUT_RTL = 1;
	
	public static final int ABSOLUTE = 1;
	public static final int RELATIVE = 2;
	
	public static final int ASPECT_FILTERING = 1;
	
    public void placeableHeader(int vsx, int vsy, int vex, int vey, int dpi);
    public void header();
    public void animatePalette(GdiPalette palette, int startIndex, int[] entries);
    public void arc(int sxr, int syr, int exr, int eyr,
		    int sxa, int sya, int exa, int eya);
    public void bitBlt(byte[] image, int dx, int dy, int dw, int dh, int sx, int sy, long rop);
    public void chord(int sxr, int syr, int exr, int eyr,
		      int sxa, int sya, int exa, int eya);
    public GdiBrush createBrushIndirect(int style, int color, int hatch);
    public GdiFont createFontIndirect(int height, int width, int escapement,
				      int orientation, int weight,
				      boolean italic, boolean underline, boolean strikeout,
				      int charset, int outPrecision, int clipPrecision,
				      int quality, int pitchAndFamily, byte[] faceName);
    public GdiPalette createPalette(int version, int[] palEntry);
    public GdiPatternBrush createPatternBrush(byte[] image);
    public GdiPen createPenIndirect(int style, int width, int color);
    public GdiRegion createRectRgn(int left, int top, int right, int bottom);
    public void deleteObject(GdiObject obj);
    public void dibBitBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, long rop);
    public GdiPatternBrush dibCreatePatternBrush(byte[] image, int usage);
    public void dibStretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop);
    public void ellipse(int sx, int sy, int ex, int ey);
    public void escape(byte[] data);
    public int excludeClipRect(int left, int top, int right, int bottom);
    public void extFloodFill(int x, int y, int color, int type);
    public void extTextOut(int x, int y, int options, int[] rect, byte[] text, int[] lpdx);
    public void fillRgn(GdiRegion rgn, GdiBrush brush);
    public void floodFill(int x, int y, int color);
    public void frameRgn(GdiRegion rgn, GdiBrush brush, int w, int h);
    public void intersectClipRect(int left, int top, int right, int bottom);
    public void invertRgn(GdiRegion rgn);
    public void lineTo(int ex, int ey);
    public void moveToEx(int x, int y, Point old);
    public void offsetClipRgn(int x, int y);
    public void offsetViewportOrgEx(int x, int y, Point point);
    public void offsetWindowOrgEx(int x, int y, Point point);
    public void paintRgn(GdiRegion rgn);
    public void patBlt(int x, int y, int width, int height, long rop);
    public void pie(int sx, int sy, int ex, int ey, int sxr, int syr, int exr, int eyr);
    public void polygon(Point[] points);
    public void polyline(Point[] points);
    public void polyPolygon(Point[][] points);
    public void realizePalette();
    public void restoreDC(int savedDC);
    public void rectangle(int sx, int sy, int ex, int ey);
    public void resizePalette(GdiPalette palette);
    public void roundRect(int sx, int sy, int ex, int ey, int rw, int rh);
    public void seveDC();
    public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old);
    public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old);
    public void selectClipRgn(GdiRegion rgn);
    public void selectObject(GdiObject obj);
    public void selectPalette(GdiPalette palette, boolean mode);
    public void setBkColor(int color);
    public void setBkMode(int mode);
    public void setDIBitsToDevice(int dx, int dy, int dw, int dh, int sx, int sy,
        			int startscan, int scanlines, byte[] image, int colorUse);
    public void setLayout(long layout);
    public void setMapMode(int mode);
    public void setMapperFlags(long flags);
    public void setPaletteEntries(GdiPalette palette, int startIndex, int[] entries);
    public void setPixel(int x, int y, int color);
    public void setPolyFillMode(int mode);
    public void setRelAbs(int mode);
    public void setROP2(int mode);
    public void setStretchBltMode(int mode);
    public void setTextAlign(int align);
    public void setTextCharacterExtra(int extra);
    public void setTextColor(int color);
    public void setTextJustification(int breakExtra, int breakCount);
    public void setViewportExtEx(int x, int y, Size old);
    public void setViewportOrgEx(int x, int y, Point old);
    public void setWindowExtEx(int width, int height, Size old);
    public void setWindowOrgEx(int x, int y, Point old);
    public void stretchBlt(byte[] image, int dx, int dy, int dw, int dh,
			int sx, int sy, int sw, int sh, long rop);
    public void stretchDIBits(int dx, int dy, int dw, int dh,
					int sx, int sy, int sw, int sh,
					byte[] image, int usage, long rop);
    public void textOut(int x, int y, byte[] text);
    public void footer();
}
