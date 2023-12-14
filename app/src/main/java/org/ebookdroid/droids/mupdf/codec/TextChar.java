package org.ebookdroid.droids.mupdf.codec;

import android.graphics.RectF;

public class TextChar extends RectF {
	public int c;

	public TextChar(float x0, float y0, float x1, float y1, int _c) {
		super(x0, y0, x1, y1);
		c = _c;
	}
}
