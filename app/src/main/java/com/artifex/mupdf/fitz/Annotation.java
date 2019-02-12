package com.artifex.mupdf.fitz;

public class Annotation
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

	protected Annotation(long p) {
		pointer = p;
	}

	public native void run(Device dev, Matrix ctm, Cookie cookie);
	public native Pixmap toPixmap(Matrix ctm, ColorSpace colorspace, boolean alpha);
	public native Rect getBounds();
	public native DisplayList toDisplayList();

	private native long advance();
}
