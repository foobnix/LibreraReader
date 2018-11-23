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

/**
 * @author Hidekatsu Izuno
 */
abstract class SvgObject {
	private SvgGdi gdi;

	public SvgObject(SvgGdi gdi) {
		this.gdi = gdi;
	}

	public SvgGdi getGDI() {
		return gdi;
	}

	public int toRealSize(int px) {
		return getGDI().getDC().getDpi() * px / 90;
	}

	public static String toColor(int color) {
		int b = (0x00FF0000 & color) >> 16;
		int g = (0x0000FF00 & color) >> 8;
		int r = (0x000000FF & color);

		return "rgb(" + r + "," + g + "," + b + ")";
	}
}
