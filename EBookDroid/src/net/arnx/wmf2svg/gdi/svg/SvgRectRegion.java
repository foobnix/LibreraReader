package net.arnx.wmf2svg.gdi.svg;

import org.w3c.dom.Element;

class SvgRectRegion extends SvgRegion {
	private int left;
	private int top;
	private int right;
	private int bottom;
	
	public SvgRectRegion(SvgGdi gdi, int left, int top, int right, int bottom) {
		super(gdi);
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
	}
	
	public int getLeft() {
		return left;
	}
	
	public int getTop() {
		return top;
	}
	
	public int getRight() {
		return right;
	}
	
	public int getBottom() {
		return bottom;
	}
	
	public Element createElement() {
		Element elem = getGDI().getDocument().createElement("rect");
		elem.setAttribute("x", "" + (int)getGDI().getDC().toAbsoluteX(getLeft()));
		elem.setAttribute("y", "" + (int)getGDI().getDC().toAbsoluteY(getTop()));
		elem.setAttribute("width", "" + (int)getGDI().getDC().toRelativeX(getRight() - getLeft()));
		elem.setAttribute("height", "" + (int)getGDI().getDC().toRelativeY(getBottom() - getTop()));
		return elem;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bottom;
		result = prime * result + left;
		result = prime * result + right;
		result = prime * result + top;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SvgRectRegion other = (SvgRectRegion) obj;
		if (bottom != other.bottom)
			return false;
		if (left != other.left)
			return false;
		if (right != other.right)
			return false;
		if (top != other.top)
			return false;
		return true;
	}
}
