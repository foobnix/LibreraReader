package com.artifex.mupdf.fitz;

public interface TextWalker
{
	public void showGlyph(Font font, Matrix trm, int glyph, int unicode, boolean wmode);
}
