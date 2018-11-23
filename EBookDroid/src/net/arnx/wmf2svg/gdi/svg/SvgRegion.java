package net.arnx.wmf2svg.gdi.svg;

import org.w3c.dom.Element;

import net.arnx.wmf2svg.gdi.GdiRegion;

abstract class SvgRegion extends SvgObject implements GdiRegion {
	public SvgRegion(SvgGdi gdi) {
		super(gdi);
	}
	
	public abstract Element createElement();
}
