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

public class ExtraDataRecord {
	
	private long header;
	
	private int sizeOfData;
	
	private byte[] data;

	public long getHeader() {
		return header;
	}

	public void setHeader(long header) {
		this.header = header;
	}

	public int getSizeOfData() {
		return sizeOfData;
	}

	public void setSizeOfData(int sizeOfData) {
		this.sizeOfData = sizeOfData;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}	
	
}
