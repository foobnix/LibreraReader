package com.artifex.mupdf.fitz;

public class PDFPage extends Page
{
	static {
		Context.init();
	}

	private PDFPage(long p) { super(p); }

	public native PDFAnnotation createAnnotation(int subtype);
	public native void deleteAnnotation(Annotation annot);
}
