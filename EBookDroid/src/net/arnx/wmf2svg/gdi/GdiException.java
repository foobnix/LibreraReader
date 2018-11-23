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
package net.arnx.wmf2svg.gdi;

/**
 * @author Hidekatsu Izuno
 */
public class GdiException extends Exception {
	/**
	 * The class fingerprint that is set to indicate serialization compatibility
	 * with a previous version of the class.
	 */
	private static final long serialVersionUID = 6015311832170522581L;

	public GdiException() {
		super();
	}

	public GdiException(String message) {
		super(message);
	}
	
	public GdiException(String message, Throwable t) {
		super(message, t);
	}
	
	public GdiException(Throwable t) {
		super(t);
	}
}
