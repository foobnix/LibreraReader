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

public class Zip64EndCentralDirRecord {
	
	private long signature;
	
	private long sizeOfZip64EndCentralDirRec;
	
	private int versionMadeBy;
	
	private int versionNeededToExtract;
	
	private int noOfThisDisk;
	
	private int noOfThisDiskStartOfCentralDir;
	
	private long totNoOfEntriesInCentralDirOnThisDisk;
	
	private long totNoOfEntriesInCentralDir;
	
	private long sizeOfCentralDir;
	
	private long offsetStartCenDirWRTStartDiskNo;
	
	private byte[] extensibleDataSector;

	public long getSignature() {
		return signature;
	}

	public void setSignature(long signature) {
		this.signature = signature;
	}

	public long getSizeOfZip64EndCentralDirRec() {
		return sizeOfZip64EndCentralDirRec;
	}

	public void setSizeOfZip64EndCentralDirRec(long sizeOfZip64EndCentralDirRec) {
		this.sizeOfZip64EndCentralDirRec = sizeOfZip64EndCentralDirRec;
	}

	public int getVersionMadeBy() {
		return versionMadeBy;
	}

	public void setVersionMadeBy(int versionMadeBy) {
		this.versionMadeBy = versionMadeBy;
	}

	public int getVersionNeededToExtract() {
		return versionNeededToExtract;
	}

	public void setVersionNeededToExtract(int versionNeededToExtract) {
		this.versionNeededToExtract = versionNeededToExtract;
	}

	public int getNoOfThisDisk() {
		return noOfThisDisk;
	}

	public void setNoOfThisDisk(int noOfThisDisk) {
		this.noOfThisDisk = noOfThisDisk;
	}

	public int getNoOfThisDiskStartOfCentralDir() {
		return noOfThisDiskStartOfCentralDir;
	}

	public void setNoOfThisDiskStartOfCentralDir(int noOfThisDiskStartOfCentralDir) {
		this.noOfThisDiskStartOfCentralDir = noOfThisDiskStartOfCentralDir;
	}

	public long getTotNoOfEntriesInCentralDirOnThisDisk() {
		return totNoOfEntriesInCentralDirOnThisDisk;
	}

	public void setTotNoOfEntriesInCentralDirOnThisDisk(
			long totNoOfEntriesInCentralDirOnThisDisk) {
		this.totNoOfEntriesInCentralDirOnThisDisk = totNoOfEntriesInCentralDirOnThisDisk;
	}

	public long getTotNoOfEntriesInCentralDir() {
		return totNoOfEntriesInCentralDir;
	}

	public void setTotNoOfEntriesInCentralDir(long totNoOfEntriesInCentralDir) {
		this.totNoOfEntriesInCentralDir = totNoOfEntriesInCentralDir;
	}

	public long getSizeOfCentralDir() {
		return sizeOfCentralDir;
	}

	public void setSizeOfCentralDir(long sizeOfCentralDir) {
		this.sizeOfCentralDir = sizeOfCentralDir;
	}

	public long getOffsetStartCenDirWRTStartDiskNo() {
		return offsetStartCenDirWRTStartDiskNo;
	}

	public void setOffsetStartCenDirWRTStartDiskNo(
			long offsetStartCenDirWRTStartDiskNo) {
		this.offsetStartCenDirWRTStartDiskNo = offsetStartCenDirWRTStartDiskNo;
	}

	public byte[] getExtensibleDataSector() {
		return extensibleDataSector;
	}

	public void setExtensibleDataSector(byte[] extensibleDataSector) {
		this.extensibleDataSector = extensibleDataSector;
	}
	
	
	
}
