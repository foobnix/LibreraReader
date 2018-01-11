package com.artifex.mupdf.fitz;

public class Page
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

    public Page(long p) {
		pointer = p;
	}

	public native Rect getBounds();

	public native Pixmap toPixmap(Matrix ctm, ColorSpace cs, boolean alpha);

	public native void run(Device dev, Matrix ctm, Cookie cookie);
	public native void runPageContents(Device dev, Matrix ctm, Cookie cookie);

	public void run(Device dev, Matrix ctm) {
		run(dev, ctm, null);
	}

	public native Annotation[] getAnnotations();
	public native Link[] getLinks();

	// FIXME: Later. Much later.
	//fz_transition *fz_page_presentation(fz_document *doc, fz_page *page, float *duration);

	public native DisplayList toDisplayList(boolean no_annotations);

    public native StructuredText toStructuredText(String options);

	public StructuredText toStructuredText() {
		return toStructuredText(null);
	}

	public native Rect[] search(String needle);

	public native byte[] textAsHtml();

	public native Separations getSeparations();
}
