package com.artifex.mupdf.fitz;

public class Document
{
	static {
		Context.init();
	}

	public static final String META_FORMAT = "format";
	public static final String META_ENCRYPTION = "encryption";
	public static final String META_INFO_AUTHOR = "info:Author";
	public static final String META_INFO_TITLE = "info:Title";

	protected long pointer;
	protected String path; /* for proofing */

	protected native void finalize();

	public void destroy() {
		finalize();
		pointer = 0;
	}

	protected Document(long p) {
		pointer = p;
	}

	protected native static Document openNativeWithPath(String filename);
	protected native static Document openNativeWithBuffer(byte buffer[], String magic);

	public static Document openDocument(String filename) {
		Document doc = openNativeWithPath(filename);
		doc.path = filename;
		return doc;
	}

	public static Document openDocument(byte buffer[], String magic) {
		return openNativeWithBuffer(buffer, magic);
	}

	public static native boolean recognize(String magic);

	public native boolean needsPassword();
	public native boolean authenticatePassword(String password);

	public native int countPages();
	public native Page loadPage(int number);
	public native Outline[] loadOutline();
	public native String getMetaData(String key);
	public native boolean isReflowable();
	public native void layout(float width, float height, float em);

	public native long makeBookmark(int page);
	public native int findBookmark(long mark);

	public native boolean isUnencryptedPDF();

	public boolean isPDF() {
		return false;
	}

	public String getPath() { return path; }
	protected native String proofNative (String currentPath, String printProfile, String displayProfile, int resolution);
	public String makeProof (String currentPath, String printProfile, String displayProfile, int resolution) {
		String proofFile = proofNative( currentPath,  printProfile,  displayProfile,  resolution);
		return proofFile;
	}
}
