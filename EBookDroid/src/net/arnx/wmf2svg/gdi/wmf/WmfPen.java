package net.arnx.wmf2svg.gdi.wmf;

import net.arnx.wmf2svg.gdi.GdiPen;

class WmfPen extends WmfObject implements GdiPen {
	private int style;
	private int width;
	private int color;
	
	public WmfPen(int id, int style, int width, int color) {
		super(id);
	}
	
	public int getStyle() {
		return style;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getColor() {
		return color;
	}
}
