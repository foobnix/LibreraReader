package net.arnx.wmf2svg.gdi.svg;

import net.arnx.wmf2svg.gdi.GdiRegion;

import org.w3c.dom.Element;

abstract class SvgRegion extends SvgObject implements GdiRegion {
	public SvgRegion(SvgGdi gdi) {
		super(gdi);
	}
	
	public abstract Element createElement();
}
