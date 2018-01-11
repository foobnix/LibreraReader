package com.artifex.mupdf.fitz;

public class StructuredText
{
	static {
		Context.init();
	}

	private long pointer;

	@Override
    protected native void finalize();

	public void destroy() {
		finalize();
		pointer = 0;
	}

	private StructuredText(long p) {
		pointer = p;
	}

    private StructuredText() {
        pointer = 0;
    }

	public native Rect[] search(String needle);
	public native Rect[] highlight(Point a, Point b);
	public native String copy(Point a, Point b);

	public native TextBlock[] getBlocks();

    public static native int initNative();
    public static native TextBlock[] getBlocks(long doc, long page);


    public class TextBlock {
		public TextLine[] lines;
		public Rect bbox;
	}

	public class TextLine {
		public TextChar[] chars;
		public Rect bbox;
	}

	public class TextChar {
		public int c;
		public Rect bbox;
		public boolean isWhitespace() {
			return Character.isWhitespace(c);
		}
	}

}
