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

public class DigitalSignature {
	
	private int headerSignature;
	
	private int sizeOfData;
	
	private String signatureData;

	public int getHeaderSignature() {
		return headerSignature;
	}

	public void setHeaderSignature(int headerSignature) {
		this.headerSignature = headerSignature;
	}

	public int getSizeOfData() {
		return sizeOfData;
	}

	public void setSizeOfData(int sizeOfData) {
		this.sizeOfData = sizeOfData;
	}

	public String getSignatureData() {
		return signatureData;
	}

	public void setSignatureData(String signatureData) {
		this.signatureData = signatureData;
	}
	
}
