package net.arnx.wmf2svg.gdi.wmf;

import net.arnx.wmf2svg.gdi.GdiPatternBrush;

class WmfPatternBrush extends WmfObject implements GdiPatternBrush {
	private byte[] image;
	
	public WmfPatternBrush(int id, byte[] image) {
		super(id);
		this.image = image;
	}
	
	public byte[] getPattern() {
		return image;
	}
}
