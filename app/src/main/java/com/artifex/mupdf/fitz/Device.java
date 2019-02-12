package com.artifex.mupdf.fitz;

public class Device
{
	static {
		Context.init();
	}

	protected long pointer;

	protected native void finalize();

	public void destroy() {
		finalize();
		pointer = 0;
	}

	private native long newNative();

	protected Device() {
		pointer = newNative();
	}

	protected Device(long p) {
		pointer = p;
	}

	/* To implement your own device in Java, you should define your own
	 * class that extends Device, and override as many of the following
	 * functions as is appropriate. For example:
	 *
	 * class ImageTraceDevice extends Device
	 * {
	 *	void fillImage(Image img, Matrix ctx, float alpha) {
	 *		System.out.println("Image!");
	 *	}
	 * };
	 */

	public void close() {}
	public void fillPath(Path path, boolean evenOdd, Matrix ctm, ColorSpace cs, float color[], float alpha) {}
	public void strokePath(Path path, StrokeState stroke, Matrix ctm, ColorSpace cs, float color[], float alpha) {}
	public void clipPath(Path path, boolean evenOdd, Matrix ctm) {}
	public void clipStrokePath(Path path, StrokeState stroke, Matrix ctm) {}
	public void fillText(Text text, Matrix ctm, ColorSpace cs, float color[], float alpha) {}
	public void strokeText(Text text, StrokeState stroke, Matrix ctm, ColorSpace cs, float color[], float alpha) {}
	public void clipText(Text text, Matrix ctm) {}
	public void clipStrokeText(Text text, StrokeState stroke, Matrix ctm) {}
	public void ignoreText(Text text, Matrix ctm) {}
	public void fillShade(Shade shd, Matrix ctm, float alpha) {}
	public void fillImage(Image img, Matrix ctm, float alpha) {}
	public void fillImageMask(Image img, Matrix ctm, ColorSpace cs, float color[], float alpha) {}
	public void clipImageMask(Image img, Matrix ctm) {}
	public void popClip() {}
	public void beginMask(Rect area, boolean luminosity, ColorSpace cs, float bc[]) {}
	public void endMask() {}
	public void beginGroup(Rect area, ColorSpace cs, boolean isolated, boolean knockout, int blendmode, float alpha) {}
	public void endGroup() {}
	public int beginTile(Rect area, Rect view, float xstep, float ystep, Matrix ctm, int id) { return 0; }
	public void endTile() {}

	/* Flags */
	public static final int FLAG_MASK = 1;
	public static final int FLAG_COLOR = 2;
	public static final int FLAG_UNCACHEABLE = 4;
	public static final int FLAG_FILLCOLOR_UNDEFINED = 8;
	public static final int FLAG_STROKECOLOR_UNDEFINED = 16;
	public static final int FLAG_STARTCAP_UNDEFINED = 32;
	public static final int FLAG_DASHCAP_UNDEFINED = 64;
	public static final int FLAG_ENDCAP_UNDEFINED = 128;
	public static final int FLAG_LINEJOIN_UNDEFINED = 256;
	public static final int FLAG_MITERLIMIT_UNDEFINED = 512;
	public static final int FLAG_LINEWIDTH_UNDEFINED = 1024;

	/* PDF 1.4 -- standard separable */
	public static final int BLEND_NORMAL = 0;
	public static final int BLEND_MULTIPLY = 1;
	public static final int BLEND_SCREEN = 2;
	public static final int BLEND_OVERLAY = 3;
	public static final int BLEND_DARKEN = 4;
	public static final int BLEND_LIGHTEN = 5;
	public static final int BLEND_COLOR_DODGE = 6;
	public static final int BLEND_COLOR_BURN = 7;
	public static final int BLEND_HARD_LIGHT = 8;
	public static final int BLEND_SOFT_LIGHT = 9;
	public static final int BLEND_DIFFERENCE = 10;
	public static final int BLEND_EXCLUSION = 11;

	/* PDF 1.4 -- standard non-separable */
	public static final int BLEND_HUE = 12;
	public static final int BLEND_SATURATION = 13;
	public static final int BLEND_COLOR = 14;
	public static final int BLEND_LUMINOSITY = 15;

	/* For packing purposes */
	public static final int BLEND_MODEMASK = 15;
	public static final int BLEND_ISOLATED = 16;
	public static final int BLEND_KNOCKOUT = 32;

	/* Device hints */
	public static final int IGNORE_IMAGE = 1;
	public static final int IGNORE_SHADE = 2;
}
