package com.artifex.mupdf.fitz;

import java.util.Date;

public class PDFAnnotation extends Annotation
{
	static {
		Context.init();
	}

	private PDFAnnotation(long p) { super(p); }

	/* IMPORTANT: Keep in sync with mupdf/pdf/annot.h */
	public static final int TYPE_TEXT = 0;
	public static final int TYPE_LINK = 1;
	public static final int TYPE_FREE_TEXT = 2;
	public static final int TYPE_LINE = 3;
	public static final int TYPE_SQUARE = 4;
	public static final int TYPE_CIRCLE = 5;
	public static final int TYPE_POLYGON = 6;
	public static final int TYPE_POLY_LINE = 7;
	public static final int TYPE_HIGHLIGHT = 8;
	public static final int TYPE_UNDERLINE = 9;
	public static final int TYPE_SQUIGGLY = 10;
	public static final int TYPE_STRIKE_OUT = 11;
	public static final int TYPE_STAMP = 12;
	public static final int TYPE_CARET = 13;
	public static final int TYPE_INK = 14;
	public static final int TYPE_POPUP = 15;
	public static final int TYPE_FILE_ATTACHMENT = 16;
	public static final int TYPE_SOUND = 17;
	public static final int TYPE_MOVIE = 18;
	public static final int TYPE_WIDGET = 19;
	public static final int TYPE_SCREEN = 20;
	public static final int TYPE_PRINTER_MARK = 21;
	public static final int TYPE_TRAP_NET = 22;
	public static final int TYPE_WATERMARK = 23;
	public static final int TYPE_3D = 24;
	public static final int TYPE_UNKNOWN = -1;

	public static final int LINE_ENDING_NONE = 0;
	public static final int LINE_ENDING_SQUARE = 1;
	public static final int LINE_ENDING_CIRCLE = 2;
	public static final int LINE_ENDING_DIAMOND = 3;
	public static final int LINE_ENDING_OPENARROW = 4;
	public static final int LINE_ENDING_CLOSEDARROW = 5;
	public static final int LINE_ENDING_BUTT = 6;
	public static final int LINE_ENDING_ROPENARR = 7;
	public static final int LINE_ENDING_RCLOSEDARROW = 8;
	public static final int LINE_ENDING_SLASH = 9;

	public native int getType();
	public native int getFlags();
	public native void setFlags(int flags);
	public native String getContents();
	public native void setContents(String contents);
	public native Rect getRect();
	public native void setRect(Rect rect);
	public native float getBorder();
	public native void setBorder(float width);
	public native float[] getColor();
	public native void setColor(float[] color);
	public native float[] getInteriorColor();
	public native void setInteriorColor(float[] color);
	public native String getAuthor();
	public native void setAuthor(String author);
	protected native long getModificationDateNative();
	protected native void setModificationDate(long time);
	public Date getModificationDate() {
		return new Date(getModificationDateNative());
	}
	public void setModificationDate(Date date) {
		setModificationDate(date.getTime());
	}

	public native int[] getLineEndingStyles();
	public native void setLineEndingStyles(int startStyle, int endStyle);
	public void setLineEndingStyles(int[] styles) {
		setLineEndingStyles(styles[0], styles[1]);
	}

	public native float[] getVertices();
	public native void setVertices(float[] vertices);
	public native float[][] getQuadPoints();
	public native void setQuadPoints(float[][] quadPoints);
	public native float[][] getInkList();
	public native void setInkList(float[][] inkList);

	public native String getIcon();
	public native void setIcon(String icon);
	public native boolean isOpen();
	public native void setIsOpen(boolean open);

	public native void updateAppearance();
}
