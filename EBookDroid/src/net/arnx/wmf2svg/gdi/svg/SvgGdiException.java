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

import net.arnx.wmf2svg.gdi.GdiException;

/**
 * @author Hidekatsu Izuno
 */
public class SvgGdiException extends GdiException {
	/**
	 * The class fingerprint that is set to indicate serialization compatibility
	 * with a previous version of the class.
	 */
	private static final long serialVersionUID = -2096332410542422534L;

	public SvgGdiException() {
		super();
	}

	public SvgGdiException(String message) {
		super(message);
	}
	
	public SvgGdiException(String message, Throwable t) {
		super(message, t);
	}
	
	public SvgGdiException(Throwable t) {
		super(t);
	}
}
