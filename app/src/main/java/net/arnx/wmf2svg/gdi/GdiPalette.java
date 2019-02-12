package net.arnx.wmf2svg.gdi;

public interface GdiPalette extends GdiObject {
	public int getVersion();
	public int[] getEntries();
}
