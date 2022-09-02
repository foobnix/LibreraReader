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
package net.arnx.wmf2svg.gdi.wmf;

import net.arnx.wmf2svg.gdi.Gdi;
import net.arnx.wmf2svg.gdi.Point;
import net.arnx.wmf2svg.gdi.Size;

/**
 * @author Hidekatsu Izuno
 */
public class WmfDc implements Cloneable {
    	
	// window offset
	private int wox = 0;
	private int woy = 0;

	// window scale
	private double wsx = 1.0;
	private double wsy = 1.0;

    // viewport
	private int vx = 0;
	private int vy = 0;
	private int vw = 0;
	private int vh = 0;
	
	// viewport offset
	private int vox = 0;
	private int voy = 0;
	
	// viewport scale
	private double vsx = 1.0;
	private double vsy = 1.0;

	// current location
	private int cx = 0;
	private int cy = 0;

    private int textAlign = Gdi.TA_TOP | Gdi.TA_LEFT;

    private WmfBrush brush = null;
	private WmfFont font = null;
	private WmfPen pen = null;

    public void offsetWindowOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = wox;
			old.y = woy;
		}
		wox += x;
		woy += y;
	}

    public void scaleWindowExtEx(int x, int xd, int y, int yd, Size old) {
		// TODO
		wsx = (wsx * x)/xd;
		wsy = (wsy * y)/yd;
	}

    public void setViewportOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = vx;
			old.y = vy;
		}
		vx = x;
		vy = y;
	}
	
	public void setViewportExtEx(int width, int height, Size old) {
		if (old != null) {
			old.width = vw;
			old.height = vh;
		}
		vw = width;
		vh = height;
	}
	
	public void offsetViewportOrgEx(int x, int y, Point old) {
		if (old != null) {
			old.x = vox;
			old.y = voy;
		}
		vox = x;
		voy = y;
	}
	
	public void scaleViewportExtEx(int x, int xd, int y, int yd, Size old) {
		// TODO
		vsx = (vsx * x)/xd;
		vsy = (vsy * y)/yd;
	}

    public void moveToEx(int x, int y, Point old) {
		if (old != null) {
			old.x = cx;
			old.y = cy;
		}
		cx = x;
		cy = y;
	}

    public int getTextAlign() {
		return textAlign;
	}
	
	public void setTextAlign(int align) {
		textAlign = align;
	}

    public WmfBrush getBrush() {
		return brush;
	}
	
	public void setBrush(WmfBrush brush) {
		this.brush = brush;
	}

    public WmfFont getFont() {
		return font;
	}
	
	public void setFont(WmfFont font) {
		this.font = font;
	}

    public WmfPen getPen() {
		return pen;
	}
	
	public void setPen(WmfPen pen) {
		this.pen = pen;
	}
}
