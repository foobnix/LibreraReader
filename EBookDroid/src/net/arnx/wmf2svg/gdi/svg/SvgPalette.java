package net.arnx.wmf2svg.gdi.svg;

import net.arnx.wmf2svg.gdi.GdiPalette;

class SvgPalette  extends SvgObject implements GdiPalette {
	private int version;
	private int[] entries;
	
	public SvgPalette(
		SvgGdi gdi,
		int version,
		int[] entries) {
		super(gdi);
		this.version = version;
		this.entries = entries;
	}
	
	public int getVersion() {
		return version;
	}
	
	public int[] getEntries() {
		return entries;
	}
}
