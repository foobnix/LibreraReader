package net.arnx.wmf2svg.gdi.wmf;

import net.arnx.wmf2svg.gdi.GdiObject;

class WmfObject implements GdiObject {
	public int id;
	
	public WmfObject(int id) {
		this.id = id;
	}
	
	public int getID() {
		return id;
	}
}
