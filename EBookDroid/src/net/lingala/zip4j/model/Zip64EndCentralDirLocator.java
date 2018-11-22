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

public class Zip64EndCentralDirLocator {
	
	private long signature;
	
	private int noOfDiskStartOfZip64EndOfCentralDirRec;
	
	private long offsetZip64EndOfCentralDirRec;
	
	private int totNumberOfDiscs;

	public long getSignature() {
		return signature;
	}

	public void setSignature(long signature) {
		this.signature = signature;
	}

	public int getNoOfDiskStartOfZip64EndOfCentralDirRec() {
		return noOfDiskStartOfZip64EndOfCentralDirRec;
	}

	public void setNoOfDiskStartOfZip64EndOfCentralDirRec(
			int noOfDiskStartOfZip64EndOfCentralDirRec) {
		this.noOfDiskStartOfZip64EndOfCentralDirRec = noOfDiskStartOfZip64EndOfCentralDirRec;
	}

	public long getOffsetZip64EndOfCentralDirRec() {
		return offsetZip64EndOfCentralDirRec;
	}

	public void setOffsetZip64EndOfCentralDirRec(long offsetZip64EndOfCentralDirRec) {
		this.offsetZip64EndOfCentralDirRec = offsetZip64EndOfCentralDirRec;
	}

	public int getTotNumberOfDiscs() {
		return totNumberOfDiscs;
	}

	public void setTotNumberOfDiscs(int totNumberOfDiscs) {
		this.totNumberOfDiscs = totNumberOfDiscs;
	}
	
	
	
}
