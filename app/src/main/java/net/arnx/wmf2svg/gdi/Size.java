package net.arnx.wmf2svg.gdi;

public class Size {
	public int width;
	public int height;
	
	public Size(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + width;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Size other = (Size) obj;
		if (height != other.height)
			return false;
		if (width != other.width)
			return false;
		return true;
	}

	public String toString() {
		return "Size [width=" + width + ", height=" + height + "]";
	}
}
