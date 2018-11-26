/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.wmf2svg.gdi.svg;

import org.w3c.dom.*;

import net.arnx.wmf2svg.gdi.*;

/**
 * @author Hidekatsu Izuno
 */
class SvgBrush extends SvgObject implements GdiBrush {
	private int style;
	private int color;
	private int hatch;

	public SvgBrush(
		SvgGdi gdi,
		int style,
		int color,
		int hatch) {
		
		super(gdi);
		this.style = style;
		this.color = color;
		this.hatch = hatch;
	}
	
	public int getStyle() {
		return style;
	}
	
	public int getColor() {
		return color;
	}
	
	public int getHatch() {
		return hatch;
	}
	
	public Element createFillPattern(String id) {
		Element pattern = null;
		
		if (style == BS_HATCHED) {
			pattern = getGDI().getDocument().createElement("pattern");
			pattern.setAttribute("id", id);
			pattern.setAttribute("patternUnits", "userSpaceOnUse");
			pattern.setAttribute("x", "" + toRealSize(0));
			pattern.setAttribute("y", "" + toRealSize(0));
			pattern.setAttribute("width", "" + toRealSize(8));
			pattern.setAttribute("height", "" + toRealSize(8));

			if (getGDI().getDC().getBkMode() == Gdi.OPAQUE) {
				Element rect = getGDI().getDocument().createElement("rect");
				rect.setAttribute("fill", toColor(getGDI().getDC().getBkColor()));
				rect.setAttribute("x", "" + toRealSize(0));
				rect.setAttribute("y", "" + toRealSize(0));
				rect.setAttribute("width", "" + toRealSize(8));
				rect.setAttribute("height", "" + toRealSize(8));
				pattern.appendChild(rect);
			}
			
			switch (hatch) {
				case HS_HORIZONTAL: {
					Element path = getGDI().getDocument().createElement("line");
					path.setAttribute("stroke", toColor(color));
					path.setAttribute("x1", "" + toRealSize(0));
					path.setAttribute("y1", "" + toRealSize(4));
					path.setAttribute("x2", "" + toRealSize(8));
					path.setAttribute("y2", "" + toRealSize(4));
					pattern.appendChild(path);
				} break;
				case HS_VERTICAL: {
					Element path = getGDI().getDocument().createElement("line");
					path.setAttribute("stroke", toColor(color));
					path.setAttribute("x1", "" + toRealSize(4));
					path.setAttribute("y1", "" + toRealSize(0));
					path.setAttribute("x2", "" + toRealSize(4));
					path.setAttribute("y2", "" + toRealSize(8));
					pattern.appendChild(path);
				} break;
				case HS_FDIAGONAL: {
					Element path = getGDI().getDocument().createElement("line");
					path.setAttribute("stroke", toColor(color));
					path.setAttribute("x1", "" + toRealSize(0));
					path.setAttribute("y1", "" + toRealSize(0));
					path.setAttribute("x2", "" + toRealSize(8));
					path.setAttribute("y2", "" + toRealSize(8));
					pattern.appendChild(path);
				} break;
				case HS_BDIAGONAL: {
					Element path = getGDI().getDocument().createElement("line");
					path.setAttribute("stroke", toColor(color));
					path.setAttribute("x1", "" + toRealSize(0));
					path.setAttribute("y1", "" + toRealSize(8));
					path.setAttribute("x2", "" + toRealSize(8));
					path.setAttribute("y2", "" + toRealSize(0));
					pattern.appendChild(path);
				} break;
				case HS_CROSS: {
					Element path1 = getGDI().getDocument().createElement("line");
					path1.setAttribute("stroke", toColor(color));
					path1.setAttribute("x1", "" + toRealSize(0));
					path1.setAttribute("y1", "" + toRealSize(4));
					path1.setAttribute("x2", "" + toRealSize(8));
					path1.setAttribute("y2", "" + toRealSize(4));
					pattern.appendChild(path1);
					Element path2 = getGDI().getDocument().createElement("line");
					path2.setAttribute("stroke", toColor(color));
					path2.setAttribute("x1", "" + toRealSize(4));
					path2.setAttribute("y1", "" + toRealSize(0));
					path2.setAttribute("x2", "" + toRealSize(4));
					path2.setAttribute("y2", "" + toRealSize(8));
					pattern.appendChild(path2);
				} break;
				case HS_DIAGCROSS: {
					Element path1 = getGDI().getDocument().createElement("line");
					path1.setAttribute("stroke", toColor(color));
					path1.setAttribute("x1", "" + toRealSize(0));
					path1.setAttribute("y1", "" + toRealSize(0));
					path1.setAttribute("x2", "" + toRealSize(8));
					path1.setAttribute("y2", "" + toRealSize(8));
					pattern.appendChild(path1);
					Element path2 = getGDI().getDocument().createElement("line");
					path2.setAttribute("stroke", toColor(color));
					path2.setAttribute("x1", "" + toRealSize(0));
					path2.setAttribute("y1", "" + toRealSize(8));
					path2.setAttribute("x2", "" + toRealSize(8));
					path2.setAttribute("y2", "" + toRealSize(0));
					pattern.appendChild(path2);
				} break;
			}
		}
		
		return pattern;
	}

	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + color;
		result = PRIME * result + hatch;
		result = PRIME * result + style;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SvgBrush other = (SvgBrush) obj;
		if (color != other.color)
			return false;
		if (hatch != other.hatch)
			return false;
		if (style != other.style)
			return false;
		return true;
	}
	
	public Text createTextNode(String id) {
		return getGDI().getDocument().createTextNode("." + id + " { " + toString() + " }\n");
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();

		// fill
		switch (style) {
			case BS_SOLID :
				buffer.append("fill: ").append(toColor(color)).append("; ");
				break;
			case BS_HATCHED :
				break;
			default :
				buffer.append("fill: none; ");
		}
		
		if (buffer.length() > 0) buffer.setLength(buffer.length()-1);
		return buffer.toString();
	}
}
