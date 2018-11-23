package net.arnx.wmf2svg.gdi.svg;

import net.arnx.wmf2svg.gdi.GdiPatternBrush;

class SvgPatternBrush extends SvgObject implements GdiPatternBrush {
	private byte[] bmp;
	
	public SvgPatternBrush(SvgGdi gdi, byte[] bmp) {
		super(gdi);
		this.bmp = bmp;
	}
	
	public byte[] getPattern() {
		return bmp;
	}
}
