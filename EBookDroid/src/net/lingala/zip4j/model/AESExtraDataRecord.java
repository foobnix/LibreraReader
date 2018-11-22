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

public class AESExtraDataRecord {
	
	private long signature;
	private int dataSize;
	private int versionNumber;
	private String vendorID;
	private int aesStrength;
	private int compressionMethod;
	
	public AESExtraDataRecord() {
		signature = -1;
		dataSize = -1;
		versionNumber = -1;
		vendorID = null;
		aesStrength = -1;
		compressionMethod = -1;
	}


	public long getSignature() {
		return signature;
	}


	public void setSignature(long signature) {
		this.signature = signature;
	}


	public int getDataSize() {
		return dataSize;
	}


	public void setDataSize(int dataSize) {
		this.dataSize = dataSize;
	}


	public int getVersionNumber() {
		return versionNumber;
	}


	public void setVersionNumber(int versionNumber) {
		this.versionNumber = versionNumber;
	}


	public String getVendorID() {
		return vendorID;
	}


	public void setVendorID(String vendorID) {
		this.vendorID = vendorID;
	}


	public int getAesStrength() {
		return aesStrength;
	}


	public void setAesStrength(int aesStrength) {
		this.aesStrength = aesStrength;
	}


	public int getCompressionMethod() {
		return compressionMethod;
	}


	public void setCompressionMethod(int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}
	
}
