/*
* Copyright 2010 Srikanth Reddy Lingala  
* 
* Licensed under the Apache License, Version 2.0 (the "License"); 
* you may not use this file except in compliance with the License. 
* You may obtain a copy of the License at 
* 
* http://www.apache.org/licenses/LICENSE-2.0 
* 
* Unless required by applicable law or agreed to in writing, 
* software distributed under the License is distributed on an "AS IS" BASIS, 
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and 
* limitations under the License. 
*/

package net.lingala.zip4j.model;

public class UnzipParameters {
	
	private boolean ignoreReadOnlyFileAttribute;
	private boolean ignoreHiddenFileAttribute;
	private boolean ignoreArchiveFileAttribute;
	private boolean ignoreSystemFileAttribute;
	private boolean ignoreAllFileAttributes;
	private boolean ignoreDateTimeAttributes;
	
	public boolean isIgnoreReadOnlyFileAttribute() {
		return ignoreReadOnlyFileAttribute;
	}
	
	public void setIgnoreReadOnlyFileAttribute(boolean ignoreReadOnlyFileAttribute) {
		this.ignoreReadOnlyFileAttribute = ignoreReadOnlyFileAttribute;
	}
	
	public boolean isIgnoreHiddenFileAttribute() {
		return ignoreHiddenFileAttribute;
	}
	
	public void setIgnoreHiddenFileAttribute(boolean ignoreHiddenFileAttribute) {
		this.ignoreHiddenFileAttribute = ignoreHiddenFileAttribute;
	}

	public boolean isIgnoreArchiveFileAttribute() {
		return ignoreArchiveFileAttribute;
	}

	public void setIgnoreArchiveFileAttribute(boolean ignoreArchiveFileAttribute) {
		this.ignoreArchiveFileAttribute = ignoreArchiveFileAttribute;
	}

	public boolean isIgnoreSystemFileAttribute() {
		return ignoreSystemFileAttribute;
	}

	public void setIgnoreSystemFileAttribute(boolean ignoreSystemFileAttribute) {
		this.ignoreSystemFileAttribute = ignoreSystemFileAttribute;
	}

	public boolean isIgnoreAllFileAttributes() {
		return ignoreAllFileAttributes;
	}

	public void setIgnoreAllFileAttributes(boolean ignoreAllFileAttributes) {
		this.ignoreAllFileAttributes = ignoreAllFileAttributes;
	}

	public boolean isIgnoreDateTimeAttributes() {
		return ignoreDateTimeAttributes;
	}

	public void setIgnoreDateTimeAttributes(boolean ignoreDateTimeAttributes) {
		this.ignoreDateTimeAttributes = ignoreDateTimeAttributes;
	}
	

}
