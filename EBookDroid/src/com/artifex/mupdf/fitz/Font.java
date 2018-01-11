package com.artifex.mupdf.fitz;

public class Font
{
	static {
		Context.init();
	}

	private long pointer;

	protected native void finalize();

	public void destroy() {
		finalize();
		pointer = 0;
	}

	private native long newNative(String name, int index);

	private Font(long p) {
		pointer = p;
	}

	public Font(String name, int index) {
		pointer = newNative(name, index);
	}

	public Font(String name) {
		pointer = newNative(name, 0);
	}

	public native String getName();

	public native int encodeCharacter(int unicode);
	public native float advanceGlyph(int glyph, boolean wmode);

	public float advanceGlyph(int glyph) {
		return advanceGlyph(glyph, false);
	}

	public String toString() {
		return "Font(" + getName() + ")";
	}
}
